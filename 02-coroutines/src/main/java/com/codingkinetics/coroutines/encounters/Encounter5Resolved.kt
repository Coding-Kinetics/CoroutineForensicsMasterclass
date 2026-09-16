package com.codingkinetics.coroutines.encounters

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

import com.codingkinetics.coroutines.helpers.IncidentPhase
import com.codingkinetics.coroutines.helpers.TelemetryVault
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * OBJECTIVES COVERED:
 * - Bidirectional failure propagation under a standard [Job] vs [SupervisorJob]
 * - Preventing sibling cancellation cascades during unhandled child failures
 * - Establishing global uncaught exception boundaries via [CoroutineExceptionHandler] (CEH)
 * - Safe scope survival and post-failure dispatch validation
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounter 2.5: The Shattered Mirror)
 *
 * INSTRUCTIONS:
 * - Phase 1 (The Trap): Run [main] with the default setup (Standard [Job], no CEH).
 *   Notice how Scout 2 detonating an uncaught [IllegalStateException] tears down the parent
 *   scope, silently strangles innocent Scout 3 mid-flight, and causes an uncaught crash.
 * - Phase 2 (The Sapper Patch):
 *   1. Swap [Job] to [SupervisorJob] to isolate sibling blast radiuses.
 *   2. Attach the [CoroutineExceptionHandler] to the root [CoroutineScope] to safely trap
 *      the detonation and advance the incident to [IncidentPhase.RESOLVED].
 */

suspend fun returnScoutData(name: String) {
    delay(50.milliseconds)
    TelemetryVault.logForensics("[$name] Returned safely with map data.")
}

suspend fun triggerDemolitionTrap(name: String) {
    delay(20.milliseconds)
    TelemetryVault.logForensics("[$name] Stepped on a cursed glyph! Detonating...")
    throw IllegalStateException("$name triggered an explosive glyph trap!")
}

suspend fun recoverRelic(name: String) {
    delay(100.milliseconds)
    TelemetryVault.logForensics("[$name] Successfully recovered the relic!")
}

fun main() = runBlocking {
    println("================================================================================")
    println("LEVEL 2 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_2_5_BLAST_RADIUS_ISOLATION}")
    println("================================================================================\n")
    TelemetryVault.reset()

    // -------------------------------------------------------------------------
    // THE SAPPER PATCH INSTRUCTIONS:
    // 1. Swap Job() with SupervisorJob() to prevent upward cancellation cascades.
    // 2. Install ceh into the expeditionScope context.
    // -------------------------------------------------------------------------

    // STEP 1: Replace Job() with SupervisorJob()
    val expeditionJob = Job() // PATCH: SupervisorJob()

    // STEP 2: Configure the CoroutineExceptionHandler for the root scope
    val ceh = CoroutineExceptionHandler { context, throwable ->
        val coroutineName = context[CoroutineName]?.name ?: "Unknown Scout"
        println("\n[CEH TRAP | ${Thread.currentThread().name}] Intercepted root detonation in $coroutineName: ${throwable.message}")
    }

    // Root expedition scope (Add '+ ceh' here when patching)
    val expeditionScope = CoroutineScope(Dispatchers.Default + expeditionJob)

    println(">>> PHASE 1: Deploying expedition scouts into the astral vault...")

    // Scout 1: Fast perimeter reconnaissance
    val scout1 = expeditionScope.launch(CoroutineName("Scout-1-Perimeter")) {
        returnScoutData("Scout 1")
    }

    // Scout 2: The Breacher (detonates uncaught exception)
    val scout2 = expeditionScope.launch(CoroutineName("Scout-2-Demolitions")) {
        triggerDemolitionTrap("Scout 2")
    }

    // Scout 3: Deep relic extraction (innocent sibling running concurrently)
    val scout3 = expeditionScope.launch(CoroutineName("Scout-3-RelicRecovery")) {
        recoverRelic("Scout 3")
    }

    // Await all deployed scouts to observe lifecycle unwinding
    scout1.join()
    scout2.join()
    scout3.join()

    // Attempt to deploy reinforcements to test if the parent scope survived
    val reinforcement = expeditionScope.launch(CoroutineName("Reinforcements")) {
        TelemetryVault.logForensics("[Reinforcements] Entering catacombs... Perimeter secure.")
    }
    reinforcement.join()

    println("\n--- EXPEDITION STATUS DUMP ---")
    println("Parent Job isActive:    ${expeditionJob.isActive}")
    println("Parent Job isCancelled: ${expeditionJob.isCancelled}")
    println("Scout 3 isCompleted:    ${scout3.isCompleted}")
    println("Scout 3 isCancelled:    ${scout3.isCancelled}")
    println("Reinforcement ran?      ${reinforcement.isCompleted && !reinforcement.isCancelled}")

    if (expeditionJob.isCancelled || scout3.isCancelled) {
        println("\n[VERDICT] CATASTROPHIC FAILURE: Scout 2 wiped out the party and collapsed the parent scope!\n")
    } else {
        println("\n[VERDICT] SUCCESS: Blast radius isolated. Sibling scouts survived and reinforcements deployed!\n")
        println(IncidentPhase.RESOLVED.description)
    }
}