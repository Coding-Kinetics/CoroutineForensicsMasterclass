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
import java.lang.Thread.sleep

/**
 * OBJECTIVES COVERED:
 * - Thread interruption flags vs blocking exception triggers
 * - Why tight CPU computation loops are deaf to [Thread.interrupt]
 * - Debunking the naive "catch InterruptedException" anti-pattern
 *
 * ROLE: Instructor Live Demo File (Demo 1B)
 *
 * INSTRUCTIONS:
 * 1. Instructor runs this task via [DungeonMasterService] under [com.codingkinetics.threadmanagement.helpers.IncidentPhase.ENCOUNTER_2_INTERRUPT_FAILURE].
 * 2. Students observe: The main thread calls [interrupt], but the berserker completes all 1000 strikes.
 * 3. Forensic lesson: [InterruptedException] is ONLY thrown by blocking JVM methods (sleep, wait, join),
 *    never by CPU-bound loops unless cooperative polling is added.
 */
class BerserkerChargeTask(
    private val district: String,
    private val strikes: Int
) : Runnable {
    override fun run() {
        try {
            TelemetryVault.logForensics("Berserker frenzy commenced in $district")

            repeat(strikes) { strikeIndex ->
                // Tight computational loop (no blocking calls)
                val current = TelemetryVault.ledger[district] ?: 0
                TelemetryVault.ledger[district] = current + 1
            }

            TelemetryVault.logForensics("Berserker frenzy completed in $district")
        } catch (e: Exception) {
            // Naive assumption: interrupt() triggers this block.
            // Real-world reality: this code is unreachable via Thread.interrupt() alone!
            TelemetryVault.logForensics("Caught exception: ${e.message}")
        }
    }
}

/**
 *  CAMPAIGN ROLE: Live Demo File (Demo 1B)
 *
 *  INSTRUCTIONS:
 *  1. Lowest dice roller runs this task live via the main function under [com.codingkinetics.threadmanagement.helpers.IncidentPhase.ENCOUNTER_2_INTERRUPT_FAILURE].
 *  2. Players observe: The main thread calls [interrupt], but the berserker completes all 1000 strikes.
 *  3. Forensic lesson: [InterruptedException] is ONLY thrown by blocking JVM methods (sleep, wait, join),
 *     never by CPU-bound loops unless cooperative polling is added.
 */
fun main() {
    println("================================================================================")
    println("LEVEL 1 RUNNER: Active Phase -> ${IncidentPhase.ENCOUNTER_2_INTERRUPT_FAILURE}")
    println("================================================================================\n")
    TelemetryVault.reset()

    runEncounter2()
}

private fun runEncounter2() {
    println(">>> PHASE 2 (DEMO): Attempting to interrupt a tight CPU loop...")
    val berserker = Thread(BerserkerChargeTask("catacomb-gates", 10_000_000), "berserker-thread")
    berserker.start()

    sleep(10)
    // TODO println(">>> PLAYER CASTS BANISHMENT: Calling berserker.interrupt() <<<")
    // TODO berserker.interrupt()

    berserker.join()
    println("[VERDICT] Berserker charged the group! Ack! Players, roll your dice to figure out which of you have been hobbled.\n")
}


