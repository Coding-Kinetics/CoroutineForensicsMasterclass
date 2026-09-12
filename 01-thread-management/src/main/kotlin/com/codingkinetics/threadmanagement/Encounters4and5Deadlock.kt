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

import com.codingkinetics.sev01_toolkit.utils.ThreadForensics
import com.codingkinetics.threadmanagement.helpers.IncidentPhase
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * OBJECTIVES COVERED:
 * - Diagnosing untimed lock acquisition and thread starvation
 * - Priority inversion (MAX_PRIORITY blocked behind MIN_PRIORITY)
 * - Remediating contention with fail-fast timeouts using [ReentrantLock.tryLock]
 *
 * CAMPAIGN ROLE: Player Trial Battlefield (Encounters 1.4 & 1.5)
 */

class TelemetrySink {
    val lock = ReentrantLock()

    /**
     * ENCOUNTER 1.4 BASELINE: Unconditional, untimed synchronization.
     * THE TRAP: lock.lock() waits forever. No timeouts, no escape.
     */
    fun flushUnbounded(batch: IngestionBatch): Boolean {
        ThreadForensics.log("Attempting lock acquisition for ${batch.source}")

        lock.lock()
        try {
            ThreadForensics.log("LOCKED: Processing ${batch.source} (${batch.workDurationMs}ms)")
            Thread.sleep(batch.workDurationMs)
            return true
        } finally {
            lock.unlock()
            ThreadForensics.log("UNLOCKED: Completed ${batch.source}")
        }
    }

    /**
     * ENCOUNTER 1.5 PLAYER ACTION:
     * Replace unbounded blocking with a bounded tryLock.
     * If the lock cannot be claimed within [timeoutMs], shed load immediately and return false.
     */
    fun flushWithDeadline(batch: IngestionBatch, timeoutMs: Long): Boolean {
        ThreadForensics.log("Attempting bounded lock for ${batch.source} (deadline: ${timeoutMs}ms)")

        // TODO (Encounter 1.5): Replace with lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
        val acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
        if (!acquired) {
            ThreadForensics.log("SHED LOAD: ${batch.source} timed out waiting for lock. Dropping telemetry to protect SLA.")
            return false
        }

        try {
            ThreadForensics.log("LOCKED: Processing ${batch.source} (${batch.workDurationMs}ms)")
            Thread.sleep(batch.workDurationMs)
            return true
        } finally {
            lock.unlock()
            ThreadForensics.log("UNLOCKED: Completed ${batch.source}")
        }
    }
}

data class IngestionBatch(
    val source: String,
    val priority: IngestionPriority,
    val workDurationMs: Long
)

enum class IngestionPriority {
    LOW,
    HIGH
}

/**
 * ENCOUNTER RUNNER: Encounters 1.4 & 1.5
 *
 * PLAYER ACTION:
 * 1. Set activePhase to [IncidentPhase.ENCOUNTER_4_LOCK_STARVATION] to witness the priority inversion trap.
 * 2. Complete the TODO in [TelemetrySink.flushWithDeadline].
 * 3. Switch activePhase to [IncidentPhase.ENCOUNTER_5_TIMED_LOCKS] to verify bounded load-shedding.
 */
fun main() {
    // TOGGLE ACTIVE ENCOUNTER:
    val activePhase = IncidentPhase.ENCOUNTER_4_LOCK_STARVATION // TODO swap out when ready
    // val activePhase = IncidentPhase.ENCOUNTER_5_TIMED_LOCKS

    println("================================================================================")
    println("LEVEL 1 RUNNER: Active Phase -> $activePhase")
    println("================================================================================\n")

    when (activePhase) {
        IncidentPhase.ENCOUNTER_4_LOCK_STARVATION -> runEncounter4()
        IncidentPhase.ENCOUNTER_5_TIMED_LOCK -> runEncounter5()
        else -> println("Unrecognized phase for this battleground: $activePhase")
    }
}

/**
 * ENCOUNTER 1.4: The Starving Sentinel (Priority Inversion & Lock Starvation)
 *
 * Demonstrates that Thread.MAX_PRIORITY cannot bypass an unconditional lock.lock()
 * held by a Thread.MIN_PRIORITY scavenger.
 */
private fun runEncounter4() {
    println(">>> ENCOUNTER 1.4: Demonstrating Unbounded Lock Contention & Starvation...")
    val sink = TelemetrySink()
    val baselineNanos = System.nanoTime()

    val scavenger = Thread({
        val heavy = IngestionBatch("salvage-cart", IngestionPriority.LOW, 4000)
        sink.flushUnbounded(heavy)
    }, "scavenger").apply { priority = Thread.MIN_PRIORITY }

    val sentinel = Thread({
        Thread.sleep(50) // Guarantee scavenger claims the lock first
        val urgent = IngestionBatch("sentinel-alarm", IngestionPriority.HIGH, 10)
        val requestNanos = System.nanoTime()
        val requestElapsedMs = TimeUnit.NANOSECONDS.toMillis(requestNanos - baselineNanos)
        println(">>> [Sentinel Timeline: T+${requestElapsedMs}ms] URGENT: Requesting gate access with MAX_PRIORITY <<<")

        sink.flushUnbounded(urgent)

        val acquiredElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baselineNanos)
        val waitDurationMs = acquiredElapsedMs - requestElapsedMs
        println(">>> [Sentinel Timeline: T+${acquiredElapsedMs}ms] Breached gate after waiting ${waitDurationMs}ms! <<<")
    }, "sentinel").apply { priority = Thread.MAX_PRIORITY }

    scavenger.start()
    sentinel.start()

    // Inspect the trapped sentinel 200ms into the siege
    Thread.sleep(200)
    println("\n>>> FIRING THREAD INSPECTOR (Observing Sentinel during contention) <<<")
    ThreadForensics.inspectThread(sentinel)
    println("=====================================================================\n")

    scavenger.join()
    sentinel.join()

    println("[VERDICT] CRITICAL SLA BREACH: High-priority sentinel hung for 4,000ms behind salvage-cart!\n")
}

/**
 * ENCOUNTER 1.5: The Ward of Timed Locks (Fail-Fast Load Shedding)
 *
 * Verifies that flushWithDeadline gives up after 200ms, shedding telemetry
 * and keeping the Sentinel responsive.
 */
private fun runEncounter5() {
    println(">>> ENCOUNTER 1.5: Verifying Timed tryLock Remediation...")
    val sink = TelemetrySink()
    val baselineNanos = System.nanoTime()

    val scavenger = Thread({
        val heavy = IngestionBatch("salvage-cart", IngestionPriority.LOW, 4000)
        sink.flushWithDeadline(heavy, 5000)
    }, "scavenger").apply { priority = Thread.MIN_PRIORITY }

    val sentinel = Thread({
        Thread.sleep(50) // Guarantee scavenger claims the lock first
        val urgent = IngestionBatch("sentinel-alarm", IngestionPriority.HIGH, 10)
        val requestNanos = System.nanoTime()
        val requestElapsedMs = TimeUnit.NANOSECONDS.toMillis(requestNanos - baselineNanos)
        println(">>> [Sentinel Timeline: T+${requestElapsedMs}ms] URGENT: Requesting bounded gate access (200ms deadline) <<<")

        val success = sink.flushWithDeadline(urgent, 200)

        val outcomeElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baselineNanos)
        val durationMs = outcomeElapsedMs - requestElapsedMs

        println("\n=========================== FORENSIC TIMELINE REPORT ===========================")
        println("Gate Access Granted:      $success")
        println("Sentinel Wait Duration:   ${durationMs}ms (Target deadline: 200ms)")
        println("================================================================================")

        if (!success && durationMs in 180..300) {
            println("[VERDICT] SUCCESS: Sentinel failed fast and shed load in ${durationMs}ms. SLA preserved!\n")
        } else {
            println("[VERDICT] FAILURE: Sentinel waited ${durationMs}ms (Lock was not bounded properly)!\n")
        }
    }, "sentinel").apply { priority = Thread.MAX_PRIORITY }

    scavenger.start()
    sentinel.start()

    scavenger.join()
    sentinel.join()
}