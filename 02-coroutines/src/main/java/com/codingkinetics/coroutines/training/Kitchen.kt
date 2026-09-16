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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * ============================================================================
 * TAVERN KITCHEN ORDERS (ANATOMY OF A BROKEN SCOPE HIERARCHY)
 * ============================================================================
 *
 * OBJECTIVES:
 * 1. Identify how instantiating a standalone `CoroutineScope(Job())` creates an
 *    unmanaged lifecycle completely detached from the caller's surrounding scope (`runBlocking`).
 * 2. Inspect Incomplete Lifecycles in the Debugger: Use the Coroutines Debugger
 *    panel to observe orphaned child routines still running in the background
 *    after their enclosing function has already claimed completion.
 *
 * DIRECTIONS:
 * 1. RUN: Execute `main()` as-is. Inspect the console logs carefully:
 *    - Notice the timestamp/order: Does "Pasta is ready!" log BEFORE "Water is hot!" and "Pasta is cooked."?
 *    - Why did the kitchen announce all dishes were prepared before the water boiled?
 * 2. DEBUG: Place a breakpoint at `log("All dishes are prepared!")` and run in Debug mode.
 *    - Open the Coroutines Debugger panel.
 *    - Locate `kitchenScope`, `boilWater`, and `cookPasta`.
 *    - Notice that `boilWater` and `cookPasta` are STILL active and suspended,
 *      even though `pastaOrder.await()` has already returned!
 * 3. REPAIR:
 *    - How would you restore structured concurrency here?
 * ============================================================================
 */
fun main(): Unit = runBlocking {
    val parentJob = Job()
    val kitchenScope = CoroutineScope(parentJob)

    val saladOrder: Job  = kitchenScope.launch {
        log("Making salad            ")
        delay(400.milliseconds)
        log("Salad is ready!         ")
    }

    val pastaOrder: Deferred<String> = kitchenScope.async {
        log("Making pasta            ")

        val boilWater = launch {
            log("    Boiling water...    ")
            delay(800.milliseconds)
            log("    Water is hot! ")
        }

        val cookPasta = async {
            log("    Cooking pasta...    ")
            delay(700.milliseconds)
            log("    Pasta is cooked.    ")
        }

        "Pasta is ready!         "
    }

    saladOrder.join()
    val pastaResult = pastaOrder.await()
    log(pastaResult)
    log("All dishes are prepared!")
}