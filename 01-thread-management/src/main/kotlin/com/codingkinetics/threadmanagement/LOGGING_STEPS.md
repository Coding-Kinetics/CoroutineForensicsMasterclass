### How to Use Thread Logging in Labs

**1. Inline Execution Logging**
   Use this inside loops or tasks to display the running thread's state, priority, and interruption flag:

```kotlin
ThreadForensics.log("Ingesting batch index $index", aggregatedEvents)
```

Output:
```agsl
[2026-09-05T08:38:00Z] [pool-1-thread-2 (TID:25) | RUNNABLE | Prio:5] : Ingesting batch index 12 | State: {service=12}
```

**2. Target Thread Inspection from the Outside**

Call this from your test runner or main thread to expose why a worker thread isn't finishing:

```kotlin
val probe = Thread { MetricSink.flushRecords(...) }
probe.start()

// Later, probe the thread state and lock ownership
ThreadForensics.inspectThread(probe)
```

Output:
```agsl
====================== THREAD INSPECTOR ======================
Thread:       critical-probe (TID: 28)
JVM State:    WAITING | Waiting on lock: java.util.concurrent.locks.ReentrantLock$NonfairSync@4e50df2e (held by: bg-worker [TID:27])
Blocked:      Count: 0 | Waited: 1
Call Stack:
  at jdk.internal.misc.Unsafe.park(Native Method)
  at java.util.concurrent.locks.LockSupport.park(LockSupport.java:221)
  at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:715)
=============================================================
```

