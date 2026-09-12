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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * OBJECTIVES COVERED:
 * - Cooperative interruption polling via [Thread.currentThread().isInterrupted]
 * - Measuring asynchronous thread time-dilation (that threads run on different timelines) and cancellation latency
 * - Clean lifecycle termination vs. runaway CPU overrun
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounter 1C)
 */
class DisciplinedPaladinWorker(
    private val district: String,
    private val attempts: Int,
    private val startGate: CountDownLatch? = null
) : Runnable {
    var haltTimeNanos: Long = 0L
        private set

    override fun run() {
        val workerStartTime = System.nanoTime()
        TelemetryVault.logForensics("Paladin patrol commenced for $district [Paladin Timeline: T+0ms]")

        // Signal to the DM that the squad has entered the field
        startGate?.countDown()

        for (march in 0 until attempts) {
            // STEP 2 TODO: Un-comment this check to observe cooperative disengagement!
            if (Thread.currentThread().isInterrupted) {
                haltTimeNanos = System.nanoTime()
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(haltTimeNanos - workerStartTime)
                TelemetryVault.logForensics(
                    "Retreat horn heeded at march #$march! Breaking combat after ${elapsedMs}ms on Paladin clock."
                )
                return
            }

            val current = TelemetryVault.ledger[district] ?: 0
            TelemetryVault.ledger[district] = current + 1

            // Micro-burn to prevent the JIT from completing 10M loops before the OS schedules the DM
            Thread.yield()
        }

        haltTimeNanos = System.nanoTime()
        val totalMs = TimeUnit.NANOSECONDS.toMillis(haltTimeNanos - workerStartTime)
        TelemetryVault.logForensics("Paladin secured $district after ${totalMs}ms on Paladin clock")
    }
}

/**
 * ENCOUNTER RUNNER: Encounter 1C
 *
 * PLAYER ACTION:
 * Run main() to observe the timeline divergence between the DM's command
 * and the Paladin's response.
 */
fun main() {
    println("================================================================================")
    println("LEVEL 1 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_3_COOPERATIVE_PALADIN}")
    println("================================================================================\n")
    TelemetryVault.reset()

    runEncounter3()
}

private fun runEncounter3() {
    println(">>> PHASE 3: Testing cooperative paladin cancellation...")
    val combatStarted = CountDownLatch(1)
    val baselineNanos = System.nanoTime()

    // FIX: combatStarted latch is now passed to the worker
    val worker = DisciplinedPaladinWorker("catacomb-gates", 10_000_000, combatStarted)
    val paladin = Thread(worker, "paladin-thread")
    paladin.start()

    // Deterministically wait for the paladin to start marching
    combatStarted.await()

    val signalTimeNanos = System.nanoTime()
    val signalElapsedMs = TimeUnit.NANOSECONDS.toMillis(signalTimeNanos - baselineNanos)
    println(">>> [DM Timeline: T+${signalElapsedMs}ms] SOUNDING RETREAT: Calling paladin.interrupt() <<<")
    paladin.interrupt()

    paladin.join()

    val total = TelemetryVault.ledger["catacomb-gates"] ?: 0
    val latencyMs = TimeUnit.NANOSECONDS.toMillis(worker.haltTimeNanos - signalTimeNanos)

    println("\n=========================== FORENSIC TIMELINE REPORT ===========================")
    println("Marches Completed:       $total / 10,000,000")
    println("DM Retreat Signal:       T+${signalElapsedMs}ms")
    println("Cancellation Latency:    ${latencyMs}ms (delay before thread acknowledged signal)")
    println("================================================================================")

    if (total < 10_000_000) {
        println("[VERDICT] SUCCESS: Paladin surrendered execution in ${latencyMs}ms at march #$total!\n")
    } else {
        println("[VERDICT] FAILURE: Paladin completed all 10,000,000 marches! (Wasted ${latencyMs}ms after retreat)\n")
    }
}