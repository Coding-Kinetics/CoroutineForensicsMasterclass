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

package com.codingkinetics.coroutines.cancellation_02

import kotlinx.coroutines.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

// Thread-safe map for metrics aggregation
internal val aggregatedEvents = ConcurrentHashMap<String, Int>()

fun logForensics(message: String) {
    val thread = Thread.currentThread()
    println("[${Instant.now()}] [${thread.name} - TID:${thread.id}] $message | Current Aggregation: $aggregatedEvents")
}

data class MetricBatch(val serviceName: String, val totalPings: Int)

/**
 * Demonstrates cancellation cooperation in coroutines.
 *
 * Notice how clean cooperation is:
 * - Option A: Call `yield()` to yield CPU and check cancellation automatically.
 * - Option B: Call `ensureActive()` inside tight CPU-bound loops.
 */
suspend fun ingestBatch(batch: MetricBatch) {
    logForensics("Starting ingestion for ${batch.serviceName}")

    try {
        repeat(batch.totalPings) { pingIndex ->
            // 1. Cooperative checkpoint: Throws CancellationException if the Job is cancelled
            currentCoroutineContext().ensureActive()

            // Alternatively, yield() allows other coroutines to execute on the dispatcher:
            // yield()

            aggregatedEvents.compute(batch.serviceName) { _, count -> (count ?: 0) + 1 }
            logForensics("Ping #$pingIndex registered for ${batch.serviceName}")
        }
        println("Successfully completed batch for ${batch.serviceName}")
    } catch (e: CancellationException) {
        // Transparent cancellation handling
        logForensics("Cancellation signal received! Safely rolling back / aborting for ${batch.serviceName}")
        throw e // Re-throw to allow parent scope to remain aware of cancellation
    }
}

fun main() = runBlocking {
    val longBatch = MetricBatch("billing-pipeline", 1000)

    println("--- LAUNCHING COROUTINE TASK ---")
    val ingestionJob: Job = launch(Dispatchers.Default) {
        ingestBatch(longBatch)
    }

    // Let the task spin up on a background worker thread
    delay(50.milliseconds)

    println("\n>>> Circuit breaker tripped! Calling job.cancelAndJoin() <<<")
    ingestionJob.cancelAndJoin()

    println("--- PIPELINE HALTED CLEANLY ---")
}