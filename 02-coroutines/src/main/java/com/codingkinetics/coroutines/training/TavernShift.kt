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

package com.codingkinetics.coroutines.training

import com.codingkinetics.coroutines.helpers.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.Job

/**
 * ============================================================================
 * THE SEVERED APRON STRINGS (ROUGE SCOPES & STRUCTURED CONCURRENCY)
 * ============================================================================
 *
 * OBJECTIVES:
 * 1. Inspect Structured Concurrency: Use the Coroutines Debugger to view the
 *    natural parent-child hierarchy between `runBlocking`, `tavernShift`, and `pourAle`.
 * 2. Break structured concurrency by launching into a detached root scope and note how
 *    the debugger hierarchy splits.
 *
 * DIRECTIONS:
 * 1. Set a breakpoint at `delay(6000)` and run with the Coroutines Debugger.
 *    Inspect the tree: notice `pourAle` is nested under `tavernShift`.
 * 2. Uncomment `val scope = CoroutineScope(Job())` and replace `async` with
 *    `scope.async`.
 * 3. Re-run with the debugger and inspect the tree again: notice `pourAle` has
 *    been severed from `tavernShift` into an isolated root job.
 * ============================================================================
 */
fun main(): Unit = runBlocking {
    log("Rusty Gasket doors open")
    // val scope = CoroutineScope(Job())

    val tavernShift = launch(Dispatchers.Default) {
        log("	Barkeeper starts round")

        val pourAle = async { // TODO replace with val pourAle = scope.async {
            log("	Tapping the dwarven stout...")
            delay(6000.milliseconds)
            "	Mug filled and foaming!"
        }

        log(pourAle.await())
    }

    log("Order shouted to bar ")
    tavernShift.join()
    log("Last call, lights out ")
}