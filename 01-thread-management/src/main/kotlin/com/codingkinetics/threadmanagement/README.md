# Level 1: The Crypt of Mortal Threads

*The party descends into the lower vaults of the Crypts of Mortal Threads. The air smells of ozone, stale locks, and burning CPU cycles. A skittering noise echoes from the ledger vault—the Ingestion Goblins have breached the perimeter, scrambling over shared memory while the dungeon's defensive wards hang in deadlock.*

**The Objective:** Fetch the primary chronomancy relic.

---

### Quest Briefing: The Fragility of Bare Metal Threads

Before modern spellcraft introduced structured coroutine scopes, ancient systems ran directly on JVM OS threads. In these low-level depths, synchronization errors yield silent corruption, rogue workers ignore banishment wards, and unconditional locks starve high-priority sentinels.

To survive the crypt, your party must complete **The Thread Forensics Incident** by moving through five tactical phases:

```
[Phase 1: Lab]                   [Phase 2: Demo]                  [Phase 3: Lab]
Ambush of Memory Goblins  --->  The Illusion of Banishment --->  The Cooperative Paladin
(Race Condition)                (Naive Interrupt Fails)          (Thread Checks isInterrupted)
                                                                            |
                                                                            v
[Phase 5: Lab]                   [Phase 4: Demo]
The Ward of Timed Locks   <---  The Starving Sentinel
(tryLock Load Shedding)         (Contention & Priority Inversion)

```

---

### Encounter 1.1: Ambush of the Memory Goblins (The Race Condition)
*Format: Hands-On Lab (10 min)*

Open **Encounter1RaceCondition.kt** for the first exercise.

The party turns around upon the sounds of stampeding in the distance. Goblins appear!

Multiple goblin raiding parties pillage the dungeon treasury simultaneously. In `GoblinRaidWorker.kt`, each squad reads the current coin balance, yields initiative briefly to the scheduler via `Thread.yield()`, and writes back an increment.

```kotlin
class GoblinRaidWorker(
    private val district: String,
    private val lootAttempts: Int
) : Runnable {
    override fun run() {
        TelemetryVault.logForensics("Goblin squad deployed to $district")

        repeat(lootAttempts) { rollIndex ->
            // Microscopic hesitation—a contested roll in shared memory
            val currentLoot = TelemetryVault.ledger[district] ?: 0
            Thread.yield()

            // Unsynchronized write-back
            TelemetryVault.ledger[district] = currentLoot + 1
        }

        TelemetryVault.logForensics("Goblin squad retreated from $district")
    }
}

```

Deploy two squads targeting `catacomb-gates` with 1,000 attempts each. Run the `main` function to inspect the battle log:

```agsl
[pool-1-thread-1 - TID:24] Goblin squad deployed to catacomb-gates | Ledger: {}
[pool-1-thread-2 - TID:25] Goblin squad deployed to catacomb-gates | Ledger: {catacomb-gates=0}
[pool-1-thread-1 - TID:24] Stale read! Goblin stole update | Ledger: {catacomb-gates=1}
[pool-1-thread-2 - TID:25] Stale read! Goblin stole update | Ledger: {catacomb-gates=1}
...
Expected: 2000 loot tokens | Actual aggregated: 1342 tokens
CRITICAL FAILURE: Silent data loss under concurrent load.

```
**What happened here?**
Why were the threads lost? Because the three operations (Read, Increment, Write) are not wrapped in an atomic unit or 
mutual exclusion lock, the two threads interleaved their execution, meaning that the results had been overwritten in the same 
shared resource by different threads.

`Thread.yield()` widens the race window - without synchronization, a context switch between read and write turns a microscopic concurrency gap into a rewrite race condition that happens consistently. Go ahead and comment out `Thread.yield()` to see how we mean. You should see less threads lost to race conditions due to the smaller gap.

* **The Takeaway:** Unsynchronized read-modify-write operations fail silently. Concurrency defects rarely throw explicit exceptions - they manifest as corrupted ledger states under heavy load.

The battle ends just as abruptly as it begins, as many goblins try to attack, and many are also lost in the confusion of race conditions.

---

### Encounter 1.2: The Illusion of Banishment (Thread Interrupt Failure)

*Format: Live Demo (10 min)*

Open **Encounter2InterruptFailure.kt** for the next exercise.

The party moves past the horde of goblins and proceeds deeper into the crypt, where it becomes wetter, darker, and a bit more jagged amongst the rocks. Finally, the party arrives at a clearing, wherein lies an atler holding an hourglass. The relic is cool to the touch, heavy with the weight of unseen seconds. As you tilt its gilded frame, the prismatic grains inside cascade in 
reverse, emitting a faint chiming sound like shattered glass settling back together.

**Instructions**
The team should roll dice on who retrieves the relic.

As the party makes its way back, a live beserker appears - and it looks astonishingly angry, and ready to charge the whole party. The dungeon party must roll their dice against the Dungeon Master's to determine who runs the live demo. 

**Instructions**
The team is put up to battle. Everyone rolls the dice, and the lowest roller must go up to the front of the room to display their screen and run the exercise. Lowest roller will be the player to attempt a distraction and a stop to the angry Beserker.

The panicked player attempts to cast a Banishment ward (`.interrupt()`) on an over-provisioned worker burning CPU cycles in an unbroken combat loop:

```kotlin
class BerserkerChargeTask(private val district: String, private val strikes: Int) : Runnable {
    override fun run() {
        try {
            ThreadForensics.logForensics("Berserker frenzy commenced in $district")
            
            repeat(strikes) { strikeIndex ->
                // Tight computational combat loop
                val current = ThreadForensics.ledger[district] ?: 0
                ThreadForensics.ledger[district] = current + 1
            }
            
            ThreadForensics.logForensics("Berserker concluded frenzy in $district")
        } catch (e: InterruptedException) {
            // Naive expectation: Banishment lands here
            println("Banishment caught on ${Thread.currentThread().name}: Banished to astral plane.")
        }
    }
}

```

Run the `main` function in this class to see what happens.

Player issues `berserkerThread.interrupt()` after 50ms - go ahead an uncomment all those TODOs. Watch the telemetry:

```agsl
[Thread-0 - TID:28 - State:RUNNABLE] Berserker frenzy commenced in catacomb-gates
>>> DM CASTS BANISHMENT: Issuing interrupt to Thread-0 <<<
[Thread-0 - TID:28 - State:RUNNABLE] Berserker strike 245 landed...
[Thread-0 - TID:28 - State:RUNNABLE] Berserker strike 999 landed...
[Thread-0 - TID:28 - State:RUNNABLE] Berserker concluded frenzy in catacomb-gates

```

It appears that the Berserker laughed off the player's attempt to stop it, seized the core right back, and makes a charge at the crew. 

*Players should roll dice against the DM to assess damage.*

* **The Takeaway:** Calling `.interrupt()` or `yield()` simply toggles an internal JVM boolean flag. It never throws `InterruptedException` unless the target thread is actively resting within explicit blocking states (`Thread.sleep()`, `Object.wait()`). A thread spinning in pure computation is completely deaf to outside interrupts.
* **Challenge:** Can you think of a way to interrupt a thread that's actively running? 

---

### Encounter 1.3: The Cooperative Paladin (Battlefield Awareness)

*Format: Hands-On Lab (15 min)*

A retreat is made, but the chaos from uncooperative threads refuse to die. In Encounter 1.2, you witnessed the Berserker ignore your banishment ward and burn CPU core until its loop had finished.

Now, the party needs to evacuate the dungeon. Threads do not share a synchronized master clock; each operates on its own OS timeline. If a worker fails to check the battlefield state, it runs wild on its own schedule, keeping the JVM alive and wasting CPU cycles long after the caller has abandoned the result.

To survive, your workers must possess **battlefield perception** with the help of the chronomaster's skills to manage time.

---

#### Interactive encounter!

**Step 1: Instrument the Timelines**
Open `Encounter3Cooperation.kt`. Notice we capture high-precision epoch timestamps on both the DM thread and the Paladin thread:

```kotlin
class DisciplinedPaladinWorker(
    private val district: String, 
    private val attempts: Int,
    private val startGate: CountDownLatch? = null
) : Runnable {
    override fun run() {
        val startTime = System.currentTimeMillis()
        TelemetryVault.logForensics("Paladin patrol commenced for $district at T+0ms")
        startGate?.countDown()
        
        for (march in 0 until attempts) {
            // STEP 2 TODO: Add the cooperative perception check with timestamping
            /*if (Thread.currentThread().isInterrupted) {
                val haltTime = System.currentTimeMillis()
                val elapsedSinceStart = haltTime - startTime
                TelemetryVault.logForensics(
                    "Retreat horn heeded at march #$march! Halting after ${elapsedSinceStart}ms on Paladin timeline."
                )
                return
            }*/

            val current = TelemetryVault.ledger[district] ?: 0
            TelemetryVault.ledger[district] = current + 1
            Thread.yield()
        }

        val totalTime = System.currentTimeMillis() - startTime
        TelemetryVault.logForensics("Paladin secured $district after ${totalTime}ms")
    }
}

```

---

**Step 2: Witness the Independent Timeline**

1. Ensure the `if (Thread.currentThread().isInterrupted)` block inside `DisciplinedPaladinWorker` is **commented out**.
2. Run `main()`.
3. Compare the DM's timeline against the Paladin's runaway timeline:

```agsl
[main          | T+0ms]   >>> DM SOUNDS RETREAT: Calling paladin.interrupt() <<<
... (main thread waits on join(), while paladin marches on its own clock) ...
[paladin-thread| T+842ms] Paladin secured catacomb-gates after 842ms
[FORENSIC RESULT] 
  - DM signaled retreat at:     T+12ms
  - Paladin acknowledged at:    NEVER (ran to completion)
  - Time wasted after retreat:  830ms
  - Cycles burned after signal: 9,998,512 marches
[VERDICT] CRITICAL FAILURE: Uncooperative thread hijacked the core for 830ms!

```

---

**Step 3: Attune Perception (Uncomment Check & Measure Latency Delta)**

1. Un-comment the cooperative interrupt check and timing logs.
2. Re-run `main()`.
3. Measure the **Cancellation Latency Difference** ($\Delta t = T_{\text{when it actually stops}} - T_{\text{cancellation signal emits}}$):

```agsl
[main          | T+14ms] >>> DM SOUNDS RETREAT: Calling paladin.interrupt() <<<
[paladin-thread| T+16ms] Retreat horn heeded at march #214! Halting after 16ms on Paladin timeline.
[FORENSIC RESULT] 
  - DM signaled retreat at:     T+14ms
  - Paladin acknowledged at:    T+16ms
  - Cancellation Latency:       2ms (Instantaneous disengagement)
  - Wasted cycles avoided:      9,999,786 marches
[VERDICT] SUCCESS: Paladin synchronized with retreat signal in 2ms!

```

---

#### The Takeaway: Why Timelines Matter

* **Threads are not synchronized steps:** Calling `.interrupt()` on Thread A from Thread B does not stop Thread A at that exact timestamp. It merely raises a flag on Thread A's independent timeline.
* **Latency Delta ($\Delta t$):** The time between when you ask work to stop and when the thread actually stops is your **cancellation latency**. Without cooperative checks, $\Delta t$ equals the entire remaining duration of the task.
* **Bridge to Level 2 (Coroutines):** In Level 2, when you issue `job.cancel()`, you will measure this exact same delta. If your coroutine lacks cooperative suspension points, its timeline will similarly drift and burn dispatcher cycles long after the parent scope has died.
---

### Encounter 1.4: The Starving Sentinel (Contention & Priority Inversion)

*Format: Interactive Diagnostic Lab (10 min)*

Having restored order to the vault, the party attempts to permanently seal the shared ledger against future raids by erecting an unyielding barrier: an unconditional mutex lock (`lock.lock()`).

Deep in the catacombs, a low-priority goblin salvage cart (`MIN_PRIORITY`) begins hauling scrap iron through the vault gateway, holding the portal open for four agonizing seconds.

Suddenly, ward sirens wail. The Catacomb Sentinel (`MAX_PRIORITY`)—the fortress’s automated watchtower probe—spots a catastrophic breach at the outer gates. The Sentinel sprints to the vault to sound the alarm and write an urgent Sev-0 report into the ledger.

It reaches the gateway, only to hit a dead stop behind the crawling salvage cart.

#### Dungeon Master Explains

In bare-metal JVM threading, thread priority numbers are merely scheduler suggestions. Once a thread acquires an exclusive monitor lock, the scheduler's priority ladder is completely dismantled. The lowest-priority thread in the system holds the highest-priority thread hostage.

#### Player Action: The Contention Probe

1. Open `TelemetrySink.kt`.
2. Locate `main()` and click the green **Run** arrow.
3. Observe the battle log and inspect the diagnostic dump:

```agsl
[scavenger - TID:22 | RUNNABLE | Prio:1]  : Attempting lock acquisition for salvage-cart
[scavenger - TID:22 | RUNNABLE | Prio:1]  : LOCKED: Processing salvage-cart (4000ms)
[sentinel  - TID:23 | RUNNABLE | Prio:10] : Attempting lock acquisition for sentinel-alarm

>>> FIRING THREAD INSPECTOR (Observing Sentinel during contention) <<<
====================== THREAD INSPECTOR ======================
Thread:       sentinel (TID: 23)
JVM State:    WAITING | Waiting on lock: ReentrantLock (held by: scavenger [TID:22])
Blocked:      Count: 0 | Waited: 1
Call Stack:
  at jdk.internal.misc.Unsafe.park(Native Method)
  at java.util.concurrent.locks.LockSupport.park(LockSupport.java:221)
  at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:715)
=============================================================
... (the entire fortress hangs for 4,000ms while the watch alarm is paralyzed) ...
[scavenger - TID:22 | RUNNABLE | Prio:1]  : UNLOCKED: Completed salvage-cart
[sentinel  - TID:23 | RUNNABLE | Prio:10] : LOCKED: Processing sentinel-alarm (10ms)
[sentinel  - TID:23 | RUNNABLE | Prio:10] : UNLOCKED: Completed sentinel-alarm

```

* **Forensic Diagnosis:** **Priority Inversion & Thread Starvation.** Despite running at `MAX_PRIORITY` (10), the Sentinel was parked in a `WAITING` state on the OS runqueue for 4,000ms. Unbounded synchronization introduces cascading SLA violations under high contention.

---

### Encounter 1.5: The Ward of Timed Locks (Load-Shedding with `tryLock`)

*Format: Hands-On Player Remediation (15 min)*

The fortress cannot tolerate a watch sentinel going dark for four seconds. When catastrophic contention strikes, critical telemetry must either acquire the channel swiftly or **fail fast and shed load**, preserving downstream responsiveness and alerting fallback routes.

#### Dungeon Master Explains

The party must dismantle the unconditional gate and replace it with a **timed boundary ward**. Using `ReentrantLock.tryLock()`, the Sentinel will wait up to a bounded deadline. If the gate does not open in time, it retreats cleanly rather than entering a comatose sleep.

#### Player Action: Forge the Timed Boundary

1. In `TelemetrySink.kt`, locate the incomplete `flushWithDeadline` function:
```kotlin
fun flushWithDeadline(batch: IngestionBatch, timeoutMs: Long): Boolean

```


2. Replace the unbounded `lock.lock()` call with timed acquisition:
```kotlin
val acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
if (!acquired) {
    ThreadForensics.log("SHED LOAD: ${batch.source} timed out waiting for lock. Dropping telemetry to protect SLA.")
    return false
}

```


3. In `main()`, navigate to the `sentinel` thread setup. Comment out `sink.flushUnbounded(urgent)` and activate the timed alternative:
```kotlin
// sink.flushUnbounded(urgent)
sink.flushWithDeadline(urgent, 200) // Give the sentinel a 200ms deadline

```


4. Click **Run** and review the outcome.

#### Outcome to Verify

Instead of waiting 4,000ms, the Sentinel gives up after exactly 200ms, sheds the payload, and allows the monitoring loop to remain alive:

```agsl
[scavenger - TID:22 | RUNNABLE | Prio:1]  : Attempting lock acquisition for salvage-cart
[scavenger - TID:22 | RUNNABLE | Prio:1]  : LOCKED: Processing salvage-cart (4000ms)
[sentinel  - TID:23 | RUNNABLE | Prio:10] : Attempting bounded lock for sentinel-alarm (deadline: 200ms)
[sentinel  - TID:23 | RUNNABLE | Prio:10] : SHED LOAD: sentinel-alarm timed out waiting for lock. Dropping telemetry to protect SLA.
[scavenger - TID:22 | RUNNABLE | Prio:1]  : UNLOCKED: Completed salvage-cart
[FORENSIC RESULT] All combatants cleared the gate. System SLA preserved.

```

* **Forensic Diagnosis:** **Deterministic Bounded Waiting.** By bounding lock acquisition with timeouts, the system sheds load proactively under Sev-0 pressure rather than collapsing into thread pool exhaustion.

---

*Loot Claimed: **Aegis of the Timed Ward** (+2 Resilience against cascading deadlock and priority inversion).*
---

### Loot Claimed: Level 1 Forensic Spoils

* **The Banner of Cooperation:** Thread cancellation is strictly cooperative; threads must actively check if their world has ended.
* **The Seal of Bounded Waiting:** Unconditional locks invert priority and cause starvation; real-world systems must enforce timeouts and shed load.

---