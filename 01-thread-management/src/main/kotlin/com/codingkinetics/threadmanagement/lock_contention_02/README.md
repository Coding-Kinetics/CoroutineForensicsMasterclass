## Level 2: Lock Contention, Un-Timed Blocks & Starvation

### The Perils of Un-Timed Synchronization

When addressing race conditions, a common knee-jerk reaction is wrapping critical sections in unconditional locks. However, un-timed synchronization introduces a different class of system failures: **deadlock, thread starvation, and priority inversion**.

If a thread encounters high latency, an un-timed network call, or an infinite loop while holding an exclusive lock, every other thread requesting that resource is transitioned by the JVM into a `BLOCKED` or `WAITING` state. In this state:
* OS and JVM thread priorities are rendered useless: a `MIN_PRIORITY` thread holding a lock will starve a `MAX_PRIORITY` thread waiting on it.
* Thread pools quickly exhaust their worker capacity as threads pile up behind the lock.

---

### Reproducing Starvation in TelemetrySink

In `TelemetrySink.kt`, create an exclusive lock protecting a shared network sink. Simulate an un-timed, hanging background flush on a low-priority thread, then observe how it starves critical health checks.

```kotlin
package forensics.telemetry

import java.util.concurrent.locks.ReentrantLock
import java.lang.Thread.sleep

object TelemetrySink {
    private val sinkLock = ReentrantLock()

    fun flushRecords(source: String, durationMs: Long) {
        // Unconditional lock acquisition without a timeout
        sinkLock.lock()
        try {
            logForensics("ACQUIRED: Exclusive access granted to $source")
            // Simulates an unbounded remote network or I/O operation
            sleep(durationMs)
        } finally {
            sinkLock.unlock()
            logForensics("RELEASED: Lock relinquished by $source")
        }
    }
}

fun main() {
    // 1. Low-priority background worker running heavy batch telemetry
    val backgroundWorker = Thread({
        TelemetrySink.flushRecords("analytics-batch", 4000)
    }, "bg-worker").apply {
        priority = Thread.MIN_PRIORITY
    }

    // 2. High-priority critical probe that needs immediate access
    val criticalProbe = Thread({
        sleep(50) // Ensure background worker acquires the lock first
        logForensics("URGENT: Attempting to flush service heartbeat...")
        TelemetrySink.flushRecords("health-probe", 10)
    }, "critical-probe").apply {
        priority = Thread.MAX_PRIORITY
    }

    backgroundWorker.start()
    criticalProbe.start()

    // 3. Telemetry observer thread to inspect system states
    sleep(200)
    logForensics("DIAGNOSTIC: critical-probe OS state is currently: ${criticalProbe.state}")

    backgroundWorker.join()
    criticalProbe.join()
}
```

Run **TelemetrySink.kt** to inspect how priority is ignored when threads block on locks:

```agsl
[bg-worker - TID:22 - State:RUNNABLE] ACQUIRED: Exclusive access granted to analytics-batch
[critical-probe - TID:23 - State:RUNNABLE] URGENT: Attempting to flush service heartbeat...
[main - TID:1 - State:RUNNABLE] DIAGNOSTIC: critical-probe OS state is currently: WAITING
... (system stalls for 4,000ms while health probe hangs) ...
[bg-worker - TID:22 - State:RUNNABLE] RELEASED: Lock relinquished by analytics-batch
[critical-probe - TID:23 - State:RUNNABLE] ACQUIRED: Exclusive access granted to health-probe
[critical-probe - TID:23 - State:RUNNABLE] RELEASED: Lock relinquished by health-probe
```

### Remediation: Enforcing Timeouts with tryLock
To prevent unbounded starvation and thread pool exhaustion, locks must fail fast when contention exceeds safe operational thresholds.

Update **TelemetrySink.kt** to use `tryLock` with a finite timeout, allowing the high-priority probe to shed load or log failure instead of hanging indefinitely:

```kotlin
package forensics.telemetry

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

object ResilientTelemetrySink {
    private val sinkLock = ReentrantLock()

    fun flushRecords(source: String, durationMs: Long, timeoutMs: Long): Boolean {
        // Attempt to acquire the lock within the timeout window
        val acquired = sinkLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
        if (!acquired) {
            logForensics("SHED LOAD: $source could not acquire lock within ${timeoutMs}ms. Aborting to avoid thread starvation.")
            return false
        }

        try {
            logForensics("ACQUIRED: Access granted to $source")
            Thread.sleep(durationMs)
            return true
        } finally {
            sinkLock.unlock()
            logForensics("RELEASED: Lock relinquished by $source")
        }
    }
}
```
Run the probe with a 500ms threshold. Observe how the critical path protects its latency SLA and releases the executing thread:

```agsl
[bg-worker - TID:22 - State:RUNNABLE] ACQUIRED: Access granted to analytics-batch
[critical-probe - TID:23 - State:RUNNABLE] URGENT: Attempting to flush service heartbeat...
[critical-probe - TID:23 - State:TIMED_WAITING] Waiting up to 500ms for lock...
[critical-probe - TID:23 - State:RUNNABLE] SHED LOAD: health-probe could not acquire lock within 500ms. Aborting to avoid thread starvation.
```
