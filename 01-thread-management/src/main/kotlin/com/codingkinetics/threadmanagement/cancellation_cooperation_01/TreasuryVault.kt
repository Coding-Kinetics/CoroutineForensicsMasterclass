/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 */

package com.codingkinetics.threadmanagement.cancellation_cooperation_01

// The communal treasure chest contested by goblin squads
internal val dungeonTreasury = HashMap<String, Int>()

fun logForensics(message: String) {
    val thread = Thread.currentThread()
    println("[${thread.name} - TID:${thread.id}] $message | Current Treasury: $dungeonTreasury")
}

// A raid party targeting a specific dungeon district
data class RaidParty(val districtName: String, val lootAttempts: Int)

internal class GoblinRaidTask(private val party: RaidParty) : Runnable {
    override fun run() {
        logForensics("Goblin squad deployed to ${party.districtName}")

        repeat(party.lootAttempts) { rollIndex ->
            // TODO: Contest shared memory and deposit loot into dungeonTreasury
        }

        println("Goblin squad retreated from ${party.districtName}")
    }
}