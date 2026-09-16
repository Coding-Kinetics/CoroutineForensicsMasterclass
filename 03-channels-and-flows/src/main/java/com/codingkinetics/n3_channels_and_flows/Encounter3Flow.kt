package com.codingkinetics.n3_channels_and_flows

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

fun livingSpring(): Flow<String> = flow {
    println("[Spring Engine] Conduit opened (Cold start on ${Thread.currentThread().name})")
    for (i in 1..5) {
        delay(100.milliseconds) // Non-blocking suspension!
        println("[Spring Engine] Emitting Essence #$i")
        emit("Essence #$i")
    }
}

fun main(): Unit = runBlocking {
    println(">>> ENCOUNTER 3.3: The Living Spring (Cold Flows)...")

    val stream = livingSpring()
    println("Flow instantiated. (Notice: Zero work has been executed yet)")
    delay(200.milliseconds)

    println("\n>>> Party deploys Collector 1:")
    val collectionJob = launch(Dispatchers.Default) {
        stream
            .map { "$it (Attuned by Mage)" }
            .flowOn(Dispatchers.Default) // Swaps emission dispatcher upstream
            .collect { item ->
                println("  [Collector 1 | ${Thread.currentThread().name}] Received: $item")
            }
    }

    // Cancellation safety check: Cut the spell mid-flow
    delay(250.milliseconds)
    println("\n>>> DM Casts Silence: Cancelling collector job...")
    collectionJob.cancelAndJoin()

    println("\n[FORENSIC RESULT] Notice that [Spring Engine] ceased emitting immediately.")
}