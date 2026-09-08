package com.codingkinetics.threadmanagement.cancellation_cooperation_01

internal class DisciplinedPaladinTask(private val party: RaidParty) : Thread() {
    override fun run() {
        logForensics("Paladin patrol commenced for ${party.districtName}")

        for (strikeIndex in 0 until party.lootAttempts) {
            // Perception check: Did the DM sound the horn of retreat?
            if (currentThread().isInterrupted) {
                logForensics("Retreat horn heard at strike $strikeIndex! Sheathing blades and breaking combat.")
                return // Cooperative disengage
            }

            val currentLoot = dungeonTreasury[party.districtName] ?: 0
            dungeonTreasury[party.districtName] = currentLoot + 1
            logForensics("Sanctified strike $strikeIndex landed in ${party.districtName}")
        }

        println("Paladin secured ${party.districtName}")
    }
}