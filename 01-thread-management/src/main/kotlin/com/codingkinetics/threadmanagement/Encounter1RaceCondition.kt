/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 *
 * COMMERCIAL WORKSHOP LICENSE:
 * This code is proprietary material developed by Coding Kinetics LLC.
 * Workshop attendees and purchasing organizations are granted a perpetual,
 * non-exclusive license to use, adapt, and integrate this utility within
 * internal projects and systems as they see fit.
 *
 * Standalone resale, redistribution, sublicensing, or inclusion in public
 * educational materials/courses outside of your organization without prior
 * written permission from Coding Kinetics LLC is strictly prohibited.
 */

package com.codingkinetics.threadmanagement

import com.codingkinetics.threadmanagement.helpers.IncidentPhase
import com.codingkinetics.threadmanagement.helpers.TelemetryVault

/**
 * OBJECTIVES COVERED:
 * - Read-modify-write atomicity failures (Race Conditions)
 * - Thread scheduler preemption via [Thread.yield]
 * - Cooperative interruption polling ([Thread.currentThread().isInterrupted])
 * - Clean lifecycle termination vs. thread overrun
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounter 1A & Encounter 1C)
 *
 * INSTRUCTIONS:
 * - Lab 1A: Inspect [GoblinRaidWorker]. Observe how yield between read and write causes silent lost updates.
 * - Lab 1C: Implement [DisciplinedPaladinWorker]. Add the cooperative check to halt immediately when interrupted.
 */

class GoblinRaidWorker(
    private val district: String,
    private val lootAttempts: Int
) : Runnable {
    override fun run() {
        TelemetryVault.logForensics("Goblin squad deployed to $district")

        repeat(lootAttempts) { rollIndex ->
            // Step 1: Read current state
            val currentLoot = TelemetryVault.ledger[district] ?: 0

            // Step 2: Yield initiative (provokes thread preemption / context switch)
            Thread.yield()

            // Step 3: Blind write-back (overwrites concurrent changes)
            TelemetryVault.ledger[district] = currentLoot + 1
        }

        TelemetryVault.logForensics("Goblin squad retreated from $district")
    }
}

/**
 * Encounter 1A: The Unsynchronized Mutation
 *
 * PLAYER ACTION:
 * Run [GoblinRaidWorker] under [IncidentPhase.ENCOUNTER_1_RACE_CONDITION] in the main function below.
 * Observe that two concurrent squads reading, yielding, and writing cause lost updates.
 */
fun main() {
    println("================================================================================")
    println("LEVEL 1 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_1_RACE_CONDITION}")
    println("================================================================================\n")
    TelemetryVault.reset()

    runEncounter1()
}

private fun runEncounter1() {
    println(">>> PHASE 1: Deploying two goblin squads concurrently (1000 attempts each)...")
    val t1 = Thread(GoblinRaidWorker("catacomb-gates", 1000), "goblin-squad-1")
    val t2 = Thread(GoblinRaidWorker("catacomb-gates", 1000), "goblin-squad-2")

    t1.start()
    t2.start()
    t1.join()
    t2.join()

    val total = TelemetryVault.ledger["catacomb-gates"] ?: 0
    println("\n[FORENSIC RESULT] Expected: 2000 | Actual: $total")
    if (total < 2000) {
        println("[VERDICT] Critical Failure: ${2000 - total} records lost to race conditions!\n")
    }
}
