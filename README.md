# Coroutine Forensics Masterclass

Welcome to the **Catacombs of Asynchrony** — a hands-on debugging and performance forensics workshop for JVM, Kotlin, coroutines, and full-stack telemetry.

This course is structured as a progressive adventure. Each module is a “level” in the same production incident storyline: a seemingly simple performance bug grows from local thread corruption into coroutine leaks, dispatcher starvation, trace analysis, and finally a full SEV-01 production triage.

You are not just learning APIs. You are learning how to think like a concurrency investigator.

The journey begins in the thread crypts, where raw JVM threads fight over shared state. From there, the party ascends into coroutine scopes, cancellation hierarchies, dispatcher behavior, structured concurrency, and flow control. Eventually, the fantasy dungeon gives way to the real battlefield: Android clients, Ktor backends, Perfetto traces, Grafana dashboards, support escalations, and incident ROI.

By the end, you should be able to move from “the app is slow” to empirical proof: which thread, coroutine, dispatcher, route, client screen, or resource bottleneck caused the failure — and how to verify that the fix worked.

## The Workshop Storyline

### Level 1: The Thread Crypts

The party descends into the lower vaults of the Catacombs of Asynchrony, where old JVM beasts still rule: raw threads, shared memory, blocking locks, and runaway CPU loops.

#### Quest 1: The Crypt of the Unbound Threads

The first threat is deceptively simple: multiple worker parties raid the same treasury at once. Because they update shared state without coordination, loot disappears from the ledger.

Learners investigate:

- Race conditions on shared mutable state.
- Why unsynchronized reads and writes lose updates.
- Why `Thread.interrupt()` does not magically stop active CPU work.
- How cooperative cancellation requires explicit checks.

The party earns the **Banner of Cooperation** by learning that cancellation is not force — it is a contract.

#### Quest 2: Lock Contention, Un-Timed Blocks & Starvation

After surviving shared-memory corruption, the party attempts to protect the treasury with locks. But unconditional locking introduces a new danger: starvation.

A low-priority background task acquires an exclusive telemetry lock and holds it while a high-priority health probe waits helplessly behind it.

Learners investigate:

- JVM `BLOCKED`, `WAITING`, and `TIMED_WAITING` states.
- Why thread priority does not rescue a thread blocked on a monitor or lock.
- How unbounded critical sections exhaust thread pools.
- Why `tryLock` with timeouts is safer for operational systems.

The lesson: synchronization must have escape hatches. A lock without a timeout can become a dungeon door that never opens.

---

### Level 2: The Sanctum of Continuation

Leaving raw threads behind, the party enters the coroutine realm. Work is no longer bound to one physical thread. It suspends, resumes, migrates, inherits context, and participates in structured cancellation.

#### Quest 3: The Hall of Whispering Scopes

The party studies the difference between the vessel and the soul: the physical thread versus the coroutine context.

Learners investigate:

- Why a coroutine is not a thread.
- How dispatchers schedule continuations across worker pools.
- Why resuming after `delay()` may happen on a different worker thread.
- How child coroutines inherit dispatcher and job context.
- Why passing `CoroutineScope` into suspending functions can leak work.
- How `coroutineScope { }` preserves structured concurrency.

The party earns the **Amulet of Continuation**, granting resistance to stale scope passing and thread-affinity assumptions.

#### Quest 4: Cooperative Cancellation & Coroutine Job Hierarchy

The party then discovers that coroutines improve cancellation ergonomics, but do not make cancellation preemptive.

A metrics ingestion coroutine keeps running after cancellation because it is trapped in a CPU-bound loop without suspension points or active cancellation checks.

Learners investigate:

- `Job` cancellation state.
- `CancellationException`.
- `ensureActive()`.
- `yield()`.
- Cancellation latency.
- How generic exception handling can accidentally swallow cancellation signals.

This quest connects directly back to Level 1: whether using threads or coroutines, long-running work must cooperate with cancellation.

---

### Level 3: Streams, Channels, and Flow Control

The party next approaches the moving rivers of asynchronous data: channels, flows, producers, consumers, buffering, and backpressure.

This level is where isolated coroutine lessons become system behavior. The party must reason not only about a single job, but about pipelines of work where slow consumers, eager producers, and missing cancellation boundaries can create invisible pressure.

Learners investigate:

- Producer/consumer coordination.
- Backpressure.
- Flow collection lifecycles.
- Coroutine leaks in streaming pipelines.
- How unbounded work queues can mimic memory leaks or latency spikes.

The core question becomes: when events arrive faster than the system can process them, does the application slow down gracefully — or does it silently accumulate doom?

---

### Level 4: The Chronomancer’s Split Crucible

The training chambers end. The party leaves the sandbox and enters a real full-stack Kotlin Multiplatform battlefield: an Android client, a Ktor backend, shared modules, and multiple thread pools.

The incident is no longer local. The frontend stutters. The backend claims it is healthy. Somewhere between UI frames, network calls, dispatcher hops, and server routes, the system desynchronizes.

The party splits into two flanks:

#### Flank A: The Android Forge

Client investigators capture Android traces to hunt:

- UI thread stalls.
- Frame budget breaches.
- GC pauses.
- Allocation spikes.
- Retained screens or ViewModels.
- Main-thread blocking work.

#### Flank B: The Kernel Conduit

Backend investigators instrument Ktor routes with ftrace markers and inspect Perfetto traces to reveal:

- Dispatcher hops.
- Coroutine continuation delays.
- Thread contention.
- Event loop blocking.
- Wide latency slices.
- Worker starvation.

The flanks reunite at the scrying pool — Perfetto — to correlate client symptoms with backend behavior.

This level teaches that performance bugs are rarely confined to one layer. The truth lives on the timeline.

---

### Level 5: Final Boss — Full-Stack Incident Triage & Telemetry

The fantasy collapses into the real nightmare: a 3:00 AM production incident.

Customers report that the app is laggy, slow, and nearly unusable. The issue affects all platforms and is difficult to reproduce. The party must now operate like an engineering incident response team.

Learners must:

- Bring Prometheus and Grafana online.
- Reproduce load.
- Inspect live JVM and service metrics.
- Capture runtime traces.
- Identify coroutine leaks or blocked dispatchers.
- Tie symptoms to a specific file, route, or anti-pattern.
- Ship and verify the mitigation.

The party roles become operational:

- **The Paladin** coordinates triage and verifies root cause.
- **The Diviner** reads dashboards and correlates metrics.
- **The Chronomancer** inspects traces, thread dumps, and coroutine behavior.

Victory requires more than a patch. The team must prove the fix restored throughput, reduced queue depth, stabilized memory, and prevented the incident from recurring.

---
## Module Map

| Module | Story Location | Focus |
| --- | --- | --- |
| `01-thread-management` | The Thread Crypts | Race conditions, interruption, cooperative cancellation, lock contention, starvation |
| `02-coroutines` | The Sanctum of Continuation | Coroutine scopes, dispatcher inheritance, suspension, job hierarchy, cooperative cancellation |
| `03-channels-and-flows` | The Rivers of Backpressure | Channels, flows, streaming pipelines, producer/consumer pressure |
| `04-reading-perfetto` | The Chronomancer’s Split Crucible | Android traces, Ktor ftrace instrumentation, Perfetto timeline analysis |
| `05-telemetry` | The Final Boss Incident | Full-stack SEV-01 triage, Grafana, Prometheus, Perfetto, root-cause verification |
| `sev01-toolkit` | The Adventurer’s Field Kit | Support templates, performance ROI templates, tracing utilities, diagnostic scripts |

---

### The Sev01 Toolkit: Field Gear for the Final Siege

Alongside the quests, the `sev01-toolkit` provides the operational templates and instrumentation needed for real incident response.

It includes:

- Support escalation intake templates.
- Performance improvement and ROI assessment templates.
- Thread diagnostic helpers.
- Perfetto/ftrace utilities.
- Scripts for Android, Ktor, and JVM thread-dump collection.

These are the party’s field instruments: the forms, probes, and rituals that turn vague complaints into reproducible evidence.


TODO idk where to move this

### New Skill Acquired: How to Use Thread Logging in Labs

You can make use of [ThreadForensics.kt] API from the /sev01 module to deepen your understanding of what's going on with these exercises. Give it a try as you go along!

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
