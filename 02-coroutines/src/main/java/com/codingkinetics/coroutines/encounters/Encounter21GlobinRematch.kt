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

package com.codingkinetics.coroutines.encounters

import kotlinx.coroutines.yield
import com.codingkinetics.coroutines.helpers.IncidentPhase
import com.codingkinetics.coroutines.helpers.TelemetryVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * OBJECTIVES COVERED:
 * - Read-modify-write atomicity hazards in suspending routines
 * - Cooperative dispatcher preemption via [yield]
 * - Shared mutable state hazards across concurrent coroutines
 * - The fallacy that single-threaded dispatchers eliminate logical race conditions
 *
 * CAMPAIGN ROLE: Concurrent Threat Actor (Encounter 2A)
 *
 * INSTRUCTIONS:
 * - Inspect [AstralGoblinRaider.raid]. Note that even without OS thread switching,
 *   cooperative suspension via [yield] allows interleaved state corruption.
 * - Sapper Patch: Protect the ledger mutation using thread-safe primitives,
 *   a [kotlinx.coroutines.sync.Mutex], or atomic references.
 */

/**
 * Simulates an ethereal raider exploiting cooperative yield points in coroutine execution.
 */
class AstralGoblinRaider(
    private val district: String,
    private val attempts: Int
) {
    suspend fun raid() {
        repeat(attempts) {
            // Step 1: Unconfined memory read
            val currentLoot = TelemetryVault.ledger[district] ?: 0

            // Step 2: Suspending yield: cooperative pause on the dispatcher
            yield()

            // Step 3: Unsynchronized write-back
            TelemetryVault.ledger[district] = currentLoot + 1
        }
    }
}

/**
 * OBJECTIVES COVERED:
 * - Observing cooperative race conditions inside structured concurrency
 * - Dispatcher preemption behavior under [yield]
 * - Lost updates in coroutines sharing unconfined mutable state
 * - Forensic verification against expected vs. actual ledger tallies
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounter 2A)
 *
 * INSTRUCTIONS:
 * - Lab 2A: Run [main] to deploy twin [AstralGoblinRaider] jobs concurrently.
 * - Forensic Goal: Notice that cooperative suspension via [yield] produces lost updates
 *   even when coroutines share cooperative dispatch loops.
 * - Sapper Patch: Refactor the raid execution to serialize state mutations or guard
 *   the shared [TelemetryVault.ledger] using a [kotlinx.coroutines.sync.Mutex].
 */

/**
 * Encounter 2A: The Astral Incursion (Cooperative Race Conditions)
 *
 * PLAYER ACTION:
 * Run [main] and observe the ledger output.
 * Verify how concurrent [AstralGoblinRaider.raid] invocations overwrite shared state.
 */
fun main() = runBlocking {
    println("================================================================================")
    println("LEVEL 2 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_2_1_RACE_CONDITIONS}")
    println("================================================================================\n")
    TelemetryVault.reset()

    runEncounter2A()
}

private suspend fun runEncounter2A() = coroutineScope {
    val targetDistrict = "astral-rift-sanctum"
    val raidAttempts = 10_000
    val expectedTotal = raidAttempts * 2

    println(">>> PHASE 1: Deploying two Astral Goblin raiders ($raidAttempts attempts each)...")

    val raider1 = AstralGoblinRaider(district = targetDistrict, attempts = raidAttempts)
    val raider2 = AstralGoblinRaider(district = targetDistrict, attempts = raidAttempts)

    // Launch concurrent raids within the structured scope
    val job1 = launch(Dispatchers.Default) {
        raider1.raid()
    }
    val job2 = launch(Dispatchers.Default) {
        raider2.raid()
    }

    job1.join()
    job2.join()

    val actualTotal = TelemetryVault.ledger[targetDistrict] ?: 0
    println("\n[FORENSIC RESULT] Expected: $expectedTotal | Actual: $actualTotal")

    if (actualTotal < expectedTotal) {
        val lostUpdates = expectedTotal - actualTotal
        println("[VERDICT] DEPLOYMENT FAILURE: $lostUpdates updates lost to cooperative race conditions!\n")
    } else {
        println("[VERDICT] Vault Secure: All mutations successfully accounted for.\n")
    }
}