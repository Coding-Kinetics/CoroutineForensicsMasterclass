## Quest 1: The Crypt of the Unbound Threads

*The party descends into the lower vaults of the Catacombs of Asynchrony. The air smells of ozone and burning CPU cycles. A skittering noise echoes from the ledger vault—the Ingestion Goblins have breached the perimeter, scrambling over shared memory and ignoring your party's commands to halt.*

---

### Encounter 1.1: Ambush of the Memory Goblins (The Race Condition)

In `TreasuryVault.kt`, remove the `// TODO` comment to simulate multiple goblin squads pillaging and depositing loot counters into the shared treasury vault (`dungeonTreasury`).

```kotlin
/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 */

package com.codingkinetics.threadmanagement.cancellation_cooperation_01

// ...

internal class GoblinRaidTask(private val party: RaidParty) : Runnable {
    override fun run() {
        logForensics("Goblin squad deployed to ${party.districtName}")
        
        repeat(party.lootAttempts) { rollIndex ->
            // Goblin peeks into the communal chest
            val currentLoot = dungeonTreasury[party.districtName] ?: 0 // <-- add here
            
            // Microscopic hesitation—a contested roll in shared memory
            Thread.yield()
            
            // Goblin writes back its count, oblivious to the other squad
            dungeonTreasury.put(party.districtName, currentLoot + 1)   // <-- add here
            logForensics("Loot tally #$rollIndex secured for ${party.districtName}") // <-- add here
        }
        
        println("Goblin squad retreated from ${party.districtName}")
    }
}

```

Each squad reads the current coin balance, yields initiative briefly via `Thread.yield()`, increments by one, and blindly writes back to the unsynchronized hash table. When two squads roll initiative simultaneously, they overwrite each other’s tallies.

Unleash both squads concurrently and inspect the forensic battle log:

```agsl
[pool-1-thread-1 - TID:24] Goblin squad deployed to catacomb-gates | Current Treasury: {}
[pool-1-thread-2 - TID:25] Goblin squad deployed to catacomb-gates | Current Treasury: {catacomb-gates=0}
[pool-1-thread-1 - TID:24] Loot tally #0 secured for catacomb-gates | Current Treasury: {catacomb-gates=1}
[pool-1-thread-2 - TID:25] Loot tally #0 secured for catacomb-gates | Current Treasury: {catacomb-gates=1}  <-- Stale read! Goblin stole the update
[pool-1-thread-1 - TID:24] Loot tally #1 secured for catacomb-gates | Current Treasury: {catacomb-gates=2}
...
Goblin squad retreated from catacomb-gates
Expected: 200 loot tokens | Actual aggregated: 134 tokens (Critical failure: data lost in the fray)

```

---

### Encounter 1.2: The Illusion of Banishment (Thread Interruption Failure)

A panicked Dungeon Master attempts to cast a Banishment ward on an over-provisioned worker thread. But JVM threads do not yield to mere magical words. Calling `.interrupt()` simply toggles an internal boolean banner—it cannot banish a creature currently swinging in a tight loop.

In `DungeonMasterService.kt`, dispatch a high-volume task to an `ExecutorService`, attempt to sever its bond after 50 milliseconds using `berserker.interrupt()`, and observe how a naive `try/catch` on `InterruptedException` fails completely against active execution:

```kotlin
/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 */

package com.codingkinetics.threadmanagement.cancellation_cooperation_01

import java.util.concurrent.Executors
import java.lang.Thread.sleep

internal class BerserkerChargeTask(private val party: RaidParty) : Thread() {
    override fun run() {
        try {
            logForensics("Berserker rampage began in ${party.districtName}")

            repeat(party.lootAttempts) { strikeIndex ->
                // Tight, unbroken combat computation loop
                val currentLoot = dungeonTreasury[party.districtName] ?: 0
                dungeonTreasury[party.districtName] = currentLoot + 1

                logForensics("Berserker strike $strikeIndex landed in ${party.districtName}")
            }

            println("Berserker concluded frenzy in ${party.districtName}")
        } catch (e: InterruptedException) {
            println("Banishment successful! Caught InterruptedException on ${Thread.currentThread().name}: Banished to astral plane.")
        }
    }
}

fun main() {
    val dungeonRegistry = Executors.newSingleThreadExecutor()
    val bloodMoonRaid = RaidParty("catacomb-gates", 1000)
    val berserker = BerserkerChargeTask(bloodMoonRaid)

    try {
        val future = dungeonRegistry.submit(berserker)

        // Give the berserker a moment to rage
        sleep(50)

        println(">>> DM CASTS BANISHMENT: Issuing interrupt to ${berserker.name} <<<")
        berserker.interrupt()

        future.get()
    } catch (e: Exception) {
        println("Encounter resolution error: $e")
    } finally {
        dungeonRegistry.shutdown()
    }
}

```

Run `DungeonMasterService.kt`. Even though the DM cast banishment, the berserker ignores the signal and cleaves through every single strike:

```agsl
[Thread-0 - TID:28] Berserker rampage began in catacomb-gates | Current Treasury: {}
[Thread-0 - TID:28] Berserker strike 0 landed in catacomb-gates | Current Treasury: {catacomb-gates=1}
[Thread-0 - TID:28] Berserker strike 1 landed in catacomb-gates | Current Treasury: {catacomb-gates=2}
>>> DM CASTS BANISHMENT: Issuing interrupt to Thread-0 <<<
[Thread-0 - TID:28] Berserker strike 2 landed in catacomb-gates | Current Treasury: {catacomb-gates=3}
[Thread-0 - TID:28] Berserker strike 3 landed in catacomb-gates | Current Treasury: {catacomb-gates=4}
...
[Thread-0 - TID:28] Berserker strike 999 landed in catacomb-gates | Current Treasury: {catacomb-gates=1000}
Berserker concluded frenzy in catacomb-gates

```

**Why did Banishment fail?** `InterruptedException` only triggers when a thread rests within designated ritual states (such as `Thread.sleep()`, `Object.wait()`, or blocking I/O channel locks). If a thread is burning CPU cycles in an uninterrupted loop, the ward goes unnoticed unless the creature is explicitly coded to check for it.

---

### Encounter 1.3: Binding the Will (Cooperative Cancellation)

To bind non-blocking execution to your command, the warrior must possess battlefield awareness. Replace `BerserkerChargeTask` with `DisciplinedPaladinTask` by inserting a perception check against `Thread.currentThread().isInterrupted` inside the combat loop:

```kotlin
internal class DisciplinedPaladinTask(private val party: RaidParty) : Thread() {
    override fun run() {
        logForensics("Paladin patrol commenced for ${party.districtName}")
        
        for (strikeIndex in 0 until party.lootAttempts) {
            // Wisdom check: Did the DM sound the horn of retreat?
            if (Thread.currentThread().isInterrupted) { // <-- Cooperative check
                logForensics("Retreat horn heard at strike $strikeIndex! Sheathing blades and breaking combat.")
                return // Orderly retreat
            }

            val currentLoot = dungeonTreasury[party.districtName] ?: 0
            dungeonTreasury[party.districtName] = currentLoot + 1
            logForensics("Sanctified strike $strikeIndex landed in ${party.districtName}")
        }
        
        println("Paladin secured ${party.districtName}")
    }
}

```

Next, return to `main()` in `DungeonMasterService.kt` and deploy `DisciplinedPaladinTask` in place of `BerserkerChargeTask`:

```kotlin
fun main() {
    val dungeonRegistry = Executors.newSingleThreadExecutor()
    val bloodMoonRaid = RaidParty("catacomb-gates", 1000)
    
    // Swap in the cooperative unit
    val paladin = DisciplinedPaladinTask(bloodMoonRaid)

    try {
        val future = dungeonRegistry.submit(paladin)
        sleep(50)

        println(">>> DM CASTS BANISHMENT: Issuing interrupt to ${paladin.name} <<<")
        paladin.interrupt()

        future.get()
    } catch (e: Exception) {
        println("Encounter resolution error: $e")
    } finally {
        dungeonRegistry.shutdown()
    }
}
```

Run `DungeonMasterService.kt`. The telemetry marks the exact heartbeat when the unit heeds the horn, halts execution, and avoids thread overrun:

```agsl
[Thread-0 - TID:30] Paladin patrol commenced for catacomb-gates | Current Treasury: {}
[Thread-0 - TID:30] Sanctified strike 0 landed in catacomb-gates | Current Treasury: {catacomb-gates=1}
[Thread-0 - TID:30] Sanctified strike 1 landed in catacomb-gates | Current Treasury: {catacomb-gates=2}
>>> DM CASTS BANISHMENT: Issuing interrupt to Thread-0 <<<
[Thread-0 - TID:30] Retreat horn heard at strike 2! Sheathing blades and breaking combat. | Current Treasury: {catacomb-gates=3}

```

---

*Loot Claimed: **The Banner of Cooperation** (+2 Insight against runaway CPU loops).*