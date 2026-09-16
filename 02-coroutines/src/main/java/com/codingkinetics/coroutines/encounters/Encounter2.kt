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

import com.codingkinetics.coroutines.helpers.IncidentPhase
import com.codingkinetics.coroutines.helpers.TelemetryVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * OBJECTIVES COVERED:
 * - Cooperative coroutine cancellation via [yield]
 * - Intercepting and re-throwing [CancellationException] to preserve structured concurrency
 * - Immediate task preemption vs. JVM thread spinning
 *
 * CAMPAIGN ROLE: Live Demonstration Unit (Encounter 2.2)
 *
 * INSTRUCTIONS:
 * - Lab 2.2: Run [main] to unleash the Berserker coroutine.
 * - Forensic Goal: Observe how calling [Job.cancel] halts the millions of unconstrained strikes
 *   the moment [yield] checks active job status.
 */

/**
 * Unleashes a frenzied coroutine worker simulating an unyielding computational loop.
 *
 * POINT OF INTEREST:
 * Incorporates [yield] as an active suspension tripwire that inspects scope cancellation status.
 */
fun CoroutineScope.launchBerserker(district: String, strikes: Int): Job = launch(Dispatchers.Default) {
    println("[${Thread.currentThread().name}] Berserker frenzy commenced in $district")

    try {
        repeat(strikes) { strikeIndex ->
            val current = TelemetryVault.ledger[district] ?: 0
            TelemetryVault.ledger[district] = current + 1

            // POINT OF INTEREST 1: The Active Tripwire
            // Cooperative suspension point that checks if the parent Job has been cancelled
            yield()
        }
        println("[${Thread.currentThread().name}] Berserker completed all strikes!")
    } catch (e: CancellationException) {
        // POINT OF INTEREST 2: Cooperative Unwinding
        println("[${Thread.currentThread().name}] BANISHED: Berserker surrendered to cancellation shockwave.")

        // POINT OF INTEREST 3: Concurrency Protocol
        // NEVER swallow this exception! Re-throw to inform parent scopes of cancellation state
        throw e
    }
}

/**
 * Encounter 2.2: The Berserker's Pacification
 *
 * PLAYER ACTION:
 * Run [main], issue a cancellation signal after a short delay, and measure strike truncation.
 */
fun main() = runBlocking {
    println("================================================================================")
    println("LEVEL 2 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_2_2_COOPERATIVE_CANCELLATION}")
    println("================================================================================\n")
    TelemetryVault.reset()

    val targetDistrict = "catacomb-gates"
    val massiveStrikeCount = 10_000_000

    println(">>> Unleashing Berserker on $targetDistrict ($massiveStrikeCount target strikes)...")
    val berserkerJob = launchBerserker(targetDistrict, massiveStrikeCount)

    // Let the Berserker thrash for a brief window (20ms)
    delay(20.milliseconds)

    println("\n>>> SAPPER COMMAND: Issuing berserkerJob.cancel() <<<")
    berserkerJob.cancelAndJoin()

    val actualStrikes = TelemetryVault.ledger[targetDistrict] ?: 0
    println("\n[FORENSIC RESULT] Berserker halted at strike $actualStrikes / $massiveStrikeCount!")
    println("[VERDICT] SUCCESS: Cooperative cancellation tripwire successfully terminated the loop.\n")
}