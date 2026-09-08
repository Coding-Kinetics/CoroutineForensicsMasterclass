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
                // Tight computational combat loop
                val currentLoot = dungeonTreasury[party.districtName] ?: 0
                dungeonTreasury[party.districtName] = currentLoot + 1

                // Forensics log demonstrates active CPU cycle execution
                logForensics("Berserker strike $strikeIndex landed in ${party.districtName}")
            }

            println("Berserker concluded frenzy in ${party.districtName}")
        } catch (e: InterruptedException) {
            // The illusion: naive parties expect Banishment (interrupt) to land here
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

        // Allow the rampage to start
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