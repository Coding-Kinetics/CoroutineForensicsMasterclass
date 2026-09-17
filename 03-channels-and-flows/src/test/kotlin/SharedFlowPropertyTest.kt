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
package com.codingkinetics.coroutines.streaming_03

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * ============================================================================
 * THE CARTOGRAPHER'S INVARIANT: THE SLOW CLERIC & THE UNBUFFERED STREAM
 * ============================================================================
 *
 * MISSION:
 * Deep in the aqueducts beneath The Rusty Gasket, the party encounters an
 * asymmetric telemetry bottleneck. The fast rogue scout ingests environmental
 * alerts instantaneously, while the sluggish cleric requires heavy,
 * variable chanting delays (50ms–300ms) before finishing each intake cycle.
 *
 * Using the Cartographer's Slate, we execute a property-based test (PBT) harness
 * across 50 randomized trials. Rather than relying on lucky unit test timings,
 * we bombard the stream with generated batch sizes and arbitrary subscriber
 * latencies to force concurrency bugs into the light.
 *
 * WHAT YOU WILL LEARN:
 * 1. How to shift from testing single, happy-path timing sequences to asserting
 *    universal system invariants
 * 2. Harnessing PBT for Coroutines - How combining randomized load generators
 *    with `kotlinx-coroutines-test` to catches edge cases that
 *    manual unit tests miss due to fixed delay assumptions.
 * 3. Hot Stream Coupling Mechanics: How shared pipelines behave when multiple
 *    subscribers consume from a common source at wildly divergent cadences.
 *
 * WHAT TO WATCH OUT FOR:
 * 1. Why a concurrency test that passes 10 times in a row can fail catastrophically
 *    on the 11th run once batch sizes scale past buffer limits.
 * 2. Assuming hot streams are naturally isolated between collectors, when in fact
 *    an unbuffered upstream emitter binds all subscriber progress to the slowest link.
 * 3. Flaky Time-Based Assertions - Relying on real-world wall-clock delays in
 *    tests instead of controlled virtual time schedulers (`advanceUntilIdle`).
 *
 * DIRECTIONS:
 * 1. Run the test suite to trigger the Cartographer's Slate.
 * 2. Analyze the failure report, inspect which generated batch size and cleric
 *    delay caused the fast collector's invariant to shatter.
 * 3. Inspect the broadcaster's buffer configuration and decouple the consumers.
 * 4. Re-run the suite to verify that your invariant holds across all 50 randomized trials.
 * ============================================================================
 */

class SharedFlowPropertyTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `PROPERTY - fast subscriber delivery must be independent of slow subscriber latency`() = runTest {
        val random = Random(seed = 42) // Fixed seed for reproducible failure traces

        // Run 50 randomized iterations (simulating the property engine)
        repeat(50) { iteration ->
            // SUT: The naive shared flow under audit (defaults: extraBufferCapacity = 0, onBufferOverflow = SUSPEND)
            val broadcaster = MutableSharedFlow<String>()

            // GENERATORS: Generate arbitrary load
            val alertCount = random.nextInt(from = 2, until = 20)
            val slowDelayMs = random.nextLong(from = 50L, until = 300L)
            val generatedAlerts = List(alertCount) { index -> "ALERT_SECTOR_${iteration}_$index" }

            val fastCollectorReceived = mutableListOf<String>()

            // 1. FAST COLLECTOR (Rogue): Takes 0ms to handle alerts
            val fastJob = launch {
                broadcaster.collect { alert ->
                    fastCollectorReceived.add(alert)
                }
            }

            // 2. SLOW COLLECTOR (Cleric): Takes slowDelayMs per alert
            val slowJob = launch {
                broadcaster.collect {
                    delay(slowDelayMs)
                }
            }

            // Allow collectors to establish their subscriptions
            advanceUntilIdle()

            // 3. PRODUCER: Emits all generated alerts in sequence
            val emitJob = launch {
                for (alert in generatedAlerts) {
                    broadcaster.emit(alert)
                }
            }

            // Advance virtual time until all ready work is done
            advanceUntilIdle()

            // The fast collector MUST have received every alert regardless of the slow collector's delay.
            assertEquals(
                generatedAlerts.size,
                fastCollectorReceived.size,
                """
                [INVARIANT SHATTERED at iteration $iteration]
                Generated alert count: $alertCount
                Slow subscriber latency: ${slowDelayMs}ms
                Fast subscriber received: ${fastCollectorReceived.size}
                
                CAUSE: Default MutableSharedFlow() suspended emitter on Alert #2 waiting for the slow collector,
                starving the fast collector of real-time alerts!
                """.trimIndent()
            )

            fastJob.cancel()
            slowJob.cancel()
            emitJob.cancel()
        }
    }
}