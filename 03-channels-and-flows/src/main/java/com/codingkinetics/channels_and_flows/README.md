# Level 3: The Subterranean Aqueduct

*The party descends into the Subterranean Aqueduct, where pressurized conduits surge with high-throughput streams of combat telemetry. The stone floor vibrates under the roar of untamed data. By your side rests the Cartographer’s Slate, humming with shifting runes that simulate an infinite army of concurrent dungeon permutations.*

**The Objective:** Stabilize the Fortress Alert Siren and survive the trial of randomized combat loads using Property-Based Testing.

---

### Quest Briefing: The Illusion of Safe Broadcasts

Moving data as single, disembodied tasks is behind you. Now, telemetry arrives as continuous, asynchronous torrents. Many adventurers reach for a `MutableSharedFlow` under the naive belief that it acts as an asynchronous, fire-and-forget broadcast bus.

In these pressurized aqueduct depths, default constructor parameters hide treacherous backpressure traps. A single sluggish collector will stall emissions across the entire pipeline, paralyzing fast responders and dragging down the entire fortress.

To complete **The Aqueduct Incident**, your party will move through three phases:

```
[Phase 1: Lab]                     [Phase 2: Property Lab]             [Phase 3: Remediation]
The Siren's Mailbox        --->   The Slate of Infinite Mimics  --->  The Resilient Conduit
(SharedFlow Slow Subscriber)      (PBT Counter-Example Found)         (Buffer Headroom & SLA Confirmed)

```

---

### Encounter 3.1: The Siren's Mailbox (The Slow Subscriber Freeze)

*Format: Hands-On Lab (15 min)*

*File: `Encounter31SharedFlowFreeze.kt`*

The party wires up a centralized alarm bell—a `MutableSharedFlow<CombatAlert>`—to broadcast perimeter alerts across two companions:

1. **The Rogue (Fast Scout):** Needs real-time alerts immediately to dodge incoming hazards (0ms).
2. **The Cleric (Slow Tank):** Heavily armored; each alert requires 500ms of ritual prayer before he can heed the next.

```kotlin
sealed interface CombatAlert {
    data class Breach(val sector: String, val timestamp: Long = System.currentTimeMillis()) : CombatAlert
}

object FortressBroadcaster {
    // Defaults: replay = 0, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.SUSPEND
    val alerts = MutableSharedFlow<CombatAlert>()
}

```

Deploy the subscribers and trigger two consecutive alerts from the watchtower:

```kotlin
fun main() = runBlocking {
    // Subscriber 1: Fast Rogue
    val rogueJob = launch(Dispatchers.Default) {
        FortressBroadcaster.alerts.collect { alert ->
            println("[Rogue  | ${Thread.currentThread().name}] Dodged hazard: $alert")
        }
    }

    // Subscriber 2: Slow Cleric
    val clericJob = launch(Dispatchers.Default) {
        FortressBroadcaster.alerts.collect { alert ->
            println("  [Cleric | ${Thread.currentThread().name}] Commencing 500ms prayer for: $alert")
            delay(500.milliseconds)
            println("  [Cleric] Prayer finished.")
        }
    }

    delay(100.milliseconds) // Allow subscriptions to establish

    val alarmJob = launch(Dispatchers.Default) {
        println(">>> EMITTER: Firing Alert #1 (Gate Breach)...")
        var start = System.currentTimeMillis()
        FortressBroadcaster.alerts.emit(CombatAlert.Breach("Outer-Gate"))
        println(">>> EMITTER: Alert #1 emitted in ${System.currentTimeMillis() - start}ms")

        println("\n>>> EMITTER: Firing Alert #2 (Wall Breach)...")
        start = System.currentTimeMillis()
        // THE TRAP: Does this return immediately?
        FortressBroadcaster.alerts.emit(CombatAlert.Breach("North-Wall"))
        println(">>> EMITTER: Alert #2 emitted in ${System.currentTimeMillis() - start}ms")
    }

    alarmJob.join()
    rogueJob.cancel()
    clericJob.cancel()
}

```

Run `main()` to inspect the runtime telemetry:

```agsl
>>> EMITTER: Firing Alert #1 (Gate Breach)...
[Rogue  | DefaultDispatcher-worker-1] Dodged hazard: Breach(sector=Outer-Gate)
  [Cleric | DefaultDispatcher-worker-2] Commencing 500ms prayer for: Breach(sector=Outer-Gate)
>>> EMITTER: Alert #1 emitted in 4ms

>>> EMITTER: Firing Alert #2 (Wall Breach)...
... (SILENCE FOR 500ms) ...
  [Cleric] Prayer finished.
[Rogue  | DefaultDispatcher-worker-1] Dodged hazard: Breach(sector=North-Wall)
  [Cleric | DefaultDispatcher-worker-2] Commencing 500ms prayer for: Breach(sector=North-Wall)
>>> EMITTER: Alert #2 emitted in 503ms!

```

**What happened here?**

Why was the emitter paralyzed for over half a second on Alert #2?

Because `MutableSharedFlow()` defaults to `extraBufferCapacity = 0` and `onBufferOverflow = BufferOverflow.SUSPEND`. It operates as a strict **rendezvous broadcast channel**.

`emit()` is a suspending call that **refuses to return until every active subscriber has finished processing the prior emission**. The sluggish Cleric hijacked the emission loop, preventing the emitter from delivering Alert #2 to the Rogue on time.

* **The Takeaway:** Hot `SharedFlow` without buffer headroom is not an asynchronous event bus. A single slow collector forces `emit()` to suspend, introducing cascading latency across all peer subscribers.

---

### Encounter 3.2: The Slate of Infinite Mimics (Property-Based Verification)

*Format: Property Testing Lab (15 min)*

*File: `SharedFlowPropertyTest.kt*`

Manual tests with static, hardcoded delays often mask concurrency bugs. The party places the **Cartographer’s Slate** upon the altar to subject the broadcaster to an infinite variety of randomized combat conditions using Property-Based Testing inside a standard JUnit 5 test harness.

#### The Architectural Invariant

> **The Invariant Property:**
> *"A fast subscriber must receive all emitted alerts within bounded emission time, completely independent of the latency of slow peer subscribers."*

Open **SharedFlowPropertyTest.kt**:

```kotlin
class SharedFlowPropertyTest {

    @Test
    fun `PROPERTY - fast subscriber delivery must be independent of slow subscriber latency`() = runTest {
        val random = Random(seed = 42) // Fixed seed for reproducible failure traces

        // Run 50 randomized iterations (simulating the Cartographer's Slate)
        repeat(50) { iteration ->
            // SUT: The naive shared flow under audit (defaults: extraBufferCapacity = 0, onBufferOverflow = SUSPEND)
            val broadcaster = MutableSharedFlow<String>()

            // GENERATORS: Generate arbitrary load
            val alertCount = random.nextInt(from = 2, until = 20)
            val slowDelayMs = random.nextLong(from = 50L, until = 300L)
            val generatedAlerts = List(alertCount) { index -> "ALERT_SECTOR_${iteration}_$index" }

            val fastCollectorReceived = mutableListOf<String>()

            // 1. FAST COLLECTOR (Rogue): Takes 0ms to handle alerts
            val fastJob = launch {
                broadcaster.collect { alert ->
                    fastCollectorReceived.add(alert)
                }
            }

            // 2. SLOW COLLECTOR (Cleric): Takes slowDelayMs per alert
            val slowJob = launch {
                broadcaster.collect {
                    delay(slowDelayMs)
                }
            }

            // Allow collectors to establish their subscriptions
            advanceUntilIdle()

            // 3. PRODUCER: Emits all generated alerts in sequence
            val emitJob = launch {
                for (alert in generatedAlerts) {
                    broadcaster.emit(alert)
                }
            }

            advanceUntilIdle()

            // INVARIANT CHECK:
            assertEquals(
                generatedAlerts.size,
                fastCollectorReceived.size,
                """
                [INVARIANT SHATTERED at iteration $iteration]
                Generated alert count: $alertCount
                Slow subscriber latency: ${slowDelayMs}ms
                Fast subscriber received: ${fastCollectorReceived.size}
                
                CAUSE: Default MutableSharedFlow() suspended emitter on Alert #2 waiting for the slow collector,
                starving the fast collector of real-time alerts!
                """.trimIndent()
            )

            fastJob.cancel()
            slowJob.cancel()
            emitJob.cancel()
        }
    }
}

```

Run the test suite via the IntelliJ gutter icon. The Slate reports a shattered invariant on iteration 0:

```agsl
org.opentest4j.AssertionFailedError: 
[INVARIANT SHATTERED at iteration 0]
Generated alert count: 14
Slow subscriber latency: 182ms
Fast subscriber received: 1

CAUSE: Default MutableSharedFlow() suspended emitter on Alert #2 waiting for the slow collector,
starving the fast collector of real-time alerts!
Expected :14
Actual   :1

```

* **The Takeaway:** The Slate isolated the minimal failing condition: any burst of $\ge 2$ items stalls delivery to the fast subscriber whenever a peer subscriber is suspended.

---

### Encounter 3.3: The Resilient Conduit (Forging Buffer Policies)

*Format: Hands-On Remediation (10 min)*

*File: `FortressBroadcaster.kt`*

The party must reinforce the broadcaster to satisfy the invariant across every permutation generated by the Cartographer's Slate.

#### Player Action: Tune Buffer Headroom and Overflow Strategy

Open `FortressBroadcaster.kt` and refactor the channel policy:

```kotlin
object FortressBroadcaster {
    // REMEDIATION: Allocate extra buffer capacity to absorb bursts
    val alerts = MutableSharedFlow<CombatAlert>(
        replay = 0,
        extraBufferCapacity = 64, // Absorbs bursts up to 64 items
        onBufferOverflow = BufferOverflow.SUSPEND
    )
}

```

**Why Are We Doing This?**
By default, `MutableSharedFlow` has a buffer capacity of $0$. That means it operates strictly on a rendezvous model: when `emit()` is called, the emitter is forbidden from returning until every single active subscriber has completed its intake loop.

1. **Decoupling Producer from Consumer Cadence:** Setting `extraBufferCapacity = 64` creates an asynchronous staging queue inside the shared flow. When bursts of alerts arrive, emit() simply enqueues the item into this shared buffer and resumes immediately (~0ms). It no longer waits on downstream coroutines to finish their work.

2. **Isolating Fast Subscribers from Slow Peers:** Because the buffer holds pending items for the lagging Cleric, the emitter never suspends during bursts. This allows the fast-moving Rogue to continuously pull alerts off the stream in real time without being tethered to the Cleric's 500ms prayer ritual.

3. **Why `replay = 0`?** `replay` governs how many historical items are immediately dispatched to brand-new subscribers who join late. We want live telemetry, not replayed historical noise when new party members connect.

4. **Why `BufferOverflow.SUSPEND?`** We want zero data loss. As long as the burst remains under 64 items, emissions are completely non-blocking. If a catastrophic disaster produces more than 64 unhandled items, the emitter gracefully applies backpressure by suspending rather than silently discarding alerts.

Update `SharedFlowPropertyTest.kt` with matching capacity:

```kotlin
val broadcaster = MutableSharedFlow<String>(
    replay = 0,
    extraBufferCapacity = 64,
    onBufferOverflow = BufferOverflow.SUSPEND
)

```

Re-run the test suite and verify the outcome:

```agsl
BUILD SUCCESSFUL in 412ms
50 tests passed. Invariant verified across 50 randomized combat loads!
[FORENSIC RESULT] Fast collectors received 100% of alerts under all permutations.

```

* **Alternative Tactic (Load Shedding):** For real-time sensor streams where stale data is disposable, configure `onBufferOverflow = BufferOverflow.DROP_OLDEST`. This ensures `emit()` never suspends and non-suspending `tryEmit()` is guaranteed to succeed.

---

### Loot Claimed: Level 3 Forensic Spoils

* **The Prism of Decoupled Flow:** Standard cold `Flow` is a direct function call. Introducing cross-thread operations (`flowOn`, `buffer`) drops a concurrent `Channel` between producer and consumer.
* **The Seal of the Resilient Broadcaster:** Unbuffered `SharedFlow` induces slow-subscriber deadlocks; multi-consumer streams require explicit `extraBufferCapacity` or `BufferOverflow` eviction policies.
* **The Slate of Infinite Realities:** Deterministic unit tests verify known paths; Property-Based Testing isolates minimal concurrency failures under randomized loads.