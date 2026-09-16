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

import kotlin.time.Duration.Companion.milliseconds
import com.codingkinetics.coroutines.helpers.IncidentPhase
import com.codingkinetics.coroutines.helpers.TelemetryVault
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * OBJECTIVES COVERED:
 * - Structured cancellation mechanics using [coroutineScope] and child [Job] hierarchies
 * - Cooperative loop inspection using zero-allocation [ensureActive] vs suspending [yield]
 * - Deterministic, non-leaking squad disengagement via [cancelAndJoin]
 * - Measuring cancellation latency and confirming resource cleanup guarantees
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounter 2.3: The Paladin's Transmutation)
 *
 * INSTRUCTIONS:
 * - Lab 2.3: Run [main] to deploy the disciplined paladin patrol across the district.
 * - Sapper Command: The DM signals retreat after 2ms. Observe how calling [cancelAndJoin]
 *   trips [ensureActive], unwinding the computational loop deterministically in under 1ms.
 */

/**
 * Executes a disciplined patrol loop bound strictly to structured concurrency.
 *
 * POINTS OF INTEREST:
 * - [ensureActive] trips immediately if the calling scope or parent Job was cancelled.
 * - [yield] relinquishes dispatcher execution to give concurrent routines a turn.
 */
suspend fun executePatrol(district: String, marches: Int) = coroutineScope {
    for (march in 0 until marches) {
        // POINT OF INTEREST 1: Synchronous Inspection Tripwire
        // Zero-allocation active check without forcing an unnecessary thread switch
        ensureActive()

        val current = TelemetryVault.ledger[district] ?: 0
        TelemetryVault.ledger[district] = current + 1

        // POINT OF INTEREST 2: Cooperative Breath
        // Suspends briefly to give peer coroutines time on the dispatcher
        yield()
    }
}

fun main() = runBlocking {
    println("================================================================================")
    println("LEVEL 2 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_2_3_STRUCTURED_DISENGAGEMENT}")
    println("================================================================================\n")
    TelemetryVault.reset()

    val targetDistrict = "catacomb-perimeter"
    val targetMarches = 10_000_000

    println(">>> PHASE 1: Deploying Disciplined Paladin Patrol ($targetMarches marches planned)...")

    val startTime = System.currentTimeMillis()

    // Deploy the paladin patrol within a child coroutine on Dispatchers.Default
    val patrolJob = launch(Dispatchers.Default) {
        try {
            executePatrol(targetDistrict, targetMarches)
            println("[paladin-worker] Patrol completed all $targetMarches marches without retreat.")
        } catch (e: CancellationException) {
            val marchesCompleted = TelemetryVault.ledger[targetDistrict] ?: 0
            println("[paladin-worker] Evacuation confirmed at march #$marchesCompleted!")
            throw e // Re-throw to respect structured cancellation!
        }
    }

    // Let the patrol advance for a brief window before sounding retreat
    delay(2.milliseconds)

    val retreatIssuedTime = System.currentTimeMillis()
    println("\n>>> [DM Timeline: T+${retreatIssuedTime - startTime}ms] SOUNDING RETREAT: Calling patrolJob.cancelAndJoin() <<<")

    // POINT OF INTEREST 3: Atomic Disengagement
    // Signals cancellation and suspends until the worker coroutine completely halts
    patrolJob.cancelAndJoin()

    val completionTime = System.currentTimeMillis()
    val cancellationLatency = completionTime - retreatIssuedTime
    val finalMarches = TelemetryVault.ledger[targetDistrict] ?: 0

    println("\n=========================== FORENSIC TIMELINE REPORT ===========================")
    println("Marches Completed:       $finalMarches / $targetMarches")
    println("DM Retreat Signal:       T+${retreatIssuedTime - startTime}ms")
    println("Cancellation Latency:    ${cancellationLatency}ms (Deterministic structured cleanup)")
    println("================================================================================")

    if (finalMarches < targetMarches && cancellationLatency <= 5) {
        println("\n[VERDICT] SUCCESS: Structured concurrency halted the squad in ${cancellationLatency}ms!\n")
    } else {
        println("\n[VERDICT] WARNING: Patrol failed to disengage deterministically within the target window.\n")
    }
}