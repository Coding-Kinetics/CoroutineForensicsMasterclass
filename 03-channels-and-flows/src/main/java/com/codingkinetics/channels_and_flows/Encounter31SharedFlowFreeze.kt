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

package com.codingkinetics.channels_and_flows

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * ============================================================================
 * THE SIREN'S CHOKEPOINT (HEAD-OF-LINE BLOCKING & HOT BROADCASTS)
 * ============================================================================
 *
 * MISSION:
 * In the subterranean conduits beneath The Rusty Gasket, the fortress alarm
 * system must broadcast critical combat telemetry to all squad members at once.
 * The Rogue reacts immediately (0ms latency), while the heavy Cleric must
 * perform a lengthy, non-negotiable 500ms prayer ritual before acknowledging
 * the next signal.
 *
 * WHAT YOU WILL LEARN:
 * 1. Backpressure Mechanics in Hot Streams: How `MutableSharedFlow` handles
 *    slow subscribers compared to Cold Flows and Channels.
 * 2. Downstream Coupling: Why broadcasting is not inherently fire-and-forget,
 *    and how a single lagging consumer can quietly throttle the producer.
 * 3. Buffer Sizing vs. Dropping Strategies: When to trade memory headroom
 *    (`extraBufferCapacity`) versus tolerating data loss (`BufferOverflow.DROP_OLDEST`)
 *    in low-latency event systems.
 *
 * WHAT TO WATCH OUT FOR:
 * 1. The Hidden Suspension Trap: Calling `emit()` looks like a simple dispatch,
 *    but under default settings (`BufferOverflow.SUSPEND` and `extraBufferCapacity = 0`),
 *    it acts as a synchronous tripwire if any collector is still working.
 * 2. Unbalanced Downstream Cadence: A fast collector's throughput is strictly
 *    clamped to the slowest collector in the pool unless an asynchronous buffer
 *    decouples them.
 * 3. Premature Cancellation: Cancelling the pipeline because of an apparent hang
 *    when the coroutine is simply suspended waiting on a peer's backpressure.
 *
 * DIRECTIONS:
 * 1. Run `main()` as-is. Note the duration of Alert #1 versus Alert #2 in the logs.
 * 2. Trace `FortressBroadcaster.alerts` to inspect its buffer configuration.
 * 3. Decouple the emitter from the Cleric's prayer loop so the Rogue receives
 *    both breaches without delay.
 * ============================================================================
 */

sealed interface CombatAlert {
    data class Breach(val sector: String, val timestamp: Long = System.currentTimeMillis()) : CombatAlert
}

fun livingSpring(): Flow<String> = flow {
    println("[Spring Engine] Conduit opened (Cold start on ${Thread.currentThread().name})")
    for (i in 1..5) {
        delay(100.milliseconds) // Non-blocking suspension!
        println("[Spring Engine] Emitting Essence #$i")
        emit("Essence #$i")
    }
}

fun main() = runBlocking {
    // Subscriber 1: Fast Rogue
    val rogueJob = launch(Dispatchers.Default) {
        FortressBroadcaster.alerts.collect { alert ->
            println("[Rogue  | ${Thread.currentThread().name}] Dodged hazard: $alert")
        }
    }

    // Subscriber 2: Slow Cleric
    val clericJob = launch(Dispatchers.Default) {
        FortressBroadcaster.alerts.collect { alert ->
            println("  [Cleric | ${Thread.currentThread().name}] Commencing 500ms prayer for: $alert")
            delay(500.milliseconds)
            println("  [Cleric] Prayer finished.")
        }
    }

    delay(100.milliseconds) // Allow subscriptions to establish

    val alarmJob = launch(Dispatchers.Default) {
        println(">>> EMITTER: Firing Alert #1 (Gate Breach)...")
        var start = System.currentTimeMillis()
        FortressBroadcaster.alerts.emit(CombatAlert.Breach("Outer-Gate"))
        println(">>> EMITTER: Alert #1 emitted in ${System.currentTimeMillis() - start}ms")

        println("\n>>> EMITTER: Firing Alert #2 (Wall Breach)...")
        start = System.currentTimeMillis()
        // THE TRAP: Does this return immediately?
        FortressBroadcaster.alerts.emit(CombatAlert.Breach("North-Wall"))
        println(">>> EMITTER: Alert #2 emitted in ${System.currentTimeMillis() - start}ms")
    }

    alarmJob.join()
    rogueJob.cancel()
    clericJob.cancel()
}