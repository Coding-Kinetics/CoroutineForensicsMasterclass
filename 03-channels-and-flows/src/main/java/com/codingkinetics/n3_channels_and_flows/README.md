# Level 3: The Subterranean Aqueduct

*The party descends into the Subterranean Aqueduct, where pressurized conduits surge with high-throughput streams of combat telemetry. The stone floor vibrates under the roar of untamed data. By your side rests the Cartographer’s Slate, humming with shifting runes that simulate an infinite army of concurrent dungeon permutations.*

**The Objective:** Stabilize the Fortress Alert Conduit and survive the trial of randomized combat loads using Property-Based Testing.

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

Open **Encounter31SharedFlowFreeze.kt**.

The party wires up a centralized alarm bell—a `MutableSharedFlow<CombatAlert>`—to broadcast perimeter alerts across two companions:

1. **The Rogue (Fast Scout):** Needs real-time alerts immediately to dodge incoming hazards.
2. **The Cleric (Slow Tank):** Heavily armored; each alert requires 500ms of ritual prayer before he can heed the next.

```kotlin
sealed interface CombatAlert {
    data class Breach(val sector: String, val timestamp: Long = System.currentTimeMillis()) : CombatAlert
}

object FortressBroadcaster {
    // THE NAIVE CONTRACT:
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

Manual tests with static, hardcoded delays often mask concurrency bugs. The party places the **Cartographer’s Slate** upon the altar to subject the broadcaster to an infinite variety of randomized combat conditions using Property-Based Testing.

#### The Architectural Invariant

> **The Invariant Property:**
> *"A fast subscriber must receive all emitted alerts within bounded emission time, completely independent of the latency of slow peer subscribers."*

Open **SharedFlowPropertyTest.kt**:

```kotlin
class SharedFlowPropertyTest : FunSpec({

    test("PROPERTY: Fast consumer delivery must be decoupled from slow consumer latency") {
        val dispatcher = StandardTestDispatcher()

        // GENERATORS: Generate random alert batches (2..20 items) and slow delays (50..300ms)
        val alertBatchArb = Arb.list(Arb.string(5..10), 2..20)
        val slowDelayArb = Arb.int(50, 300)

        checkAll(alertBatchArb, slowDelayArb) { alerts, slowDelayMs ->
            runTest(dispatcher) {
                // SUT: Naive shared flow under audit
                val broadcaster = MutableSharedFlow<String>()

                val fastReceived = mutableListOf<String>()

                // Fast Collector: receives instantly (0 virtual ms)
                val fastJob = launch {
                    broadcaster.collect { fastReceived.add(it) }
                }

                // Slow Collector: simulates heavy work on each item
                val slowJob = launch {
                    broadcaster.collect {
                        delay(slowDelayMs.toLong())
                    }
                }

                advanceUntilIdle() // Ensure subscribers are attuned

                // Producer: fires all generated alerts
                val emitJob = launch {
                    for (alert in alerts) {
                        broadcaster.emit(alert)
                    }
                }

                // Advance virtual time strictly enough for the producer to complete
                advanceUntilIdle()

                // VERIFICATION: Fast collector must have received every alert!
                fastReceived.size shouldBe alerts.size

                fastJob.cancel()
                slowJob.cancel()
                emitJob.cancel()
            }
        }
    }
})

```

Run the test suite. Notice how the Kotest engine hammers the broadcaster with dynamic batches:

```agsl
Property failed after 1 attempts!
  Input 1: ["ALERT_A", "ALERT_B"]
  Input 2: 50 (slowDelayMs)

Expected: 2
Actual: 1

Kotest Property Assertion Failed:
At virtual time T=0ms, Alert #1 was emitted.
When the producer attempted to emit Alert #2, it SUSPENDED because the slow collector
had not completed its 50ms delay, stalling delivery to the fast collector.

```

* **Forensic Diagnosis:** The Property-Based Test automatically found the minimum shrinking counter-example: any emission sequence of $\ge 2$ items stalls when paired with a subscriber whose delay is $> 0\text{ms}$.

---

### Encounter 3.3: The Resilient Conduit (Forging Buffer Policies)

*Format: Hands-On Remediation (10 min)*

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
100 tests passed. Invariant verified across 100 randomized combat loads!
[FORENSIC RESULT] Fast collectors received 100% of alerts under all permutations.

```

* **Alternative Tactic (Load Shedding):** For real-time sensor streams where stale data is disposable, configure `onBufferOverflow = BufferOverflow.DROP_OLDEST`. This ensures `emit()` never suspends and non-suspending `tryEmit()` is guaranteed to succeed.

---

### Loot Claimed: Level 3 Forensic Spoils

* **The Prism of Decoupled Flow:** Standard cold `Flow` is a direct function call. Introducing cross-thread operations (`flowOn`, `buffer`) drops a concurrent `Channel` between producer and consumer.
* **The Seal of the Resilient Broadcaster:** Unbuffered `SharedFlow` induces slow-subscriber deadlocks; multi-consumer streams require explicit `extraBufferCapacity` or `BufferOverflow` eviction policies.
* **The Slate of Infinite Realities:** Deterministic unit tests verify known paths; Property-Based Testing isolates minimal concurrency failures under randomized loads.