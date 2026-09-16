package com.codingkinetics.n3_channels_and_flows

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration.Companion.milliseconds

fun main() = runBlocking {
    println(">>> ENCOUNTER 3.2: The Courier Tube (Hot Channels)...")

    // BASELINE: Channel.RENDEZVOUS (capacity = 0)
    // Step 2 TODO: Add Channel(Channel.BUFFERED) or Channel(capacity = 5)
    val soulChannel = Channel<String>()

    // PRODUCER COROUTINE
    val producer = launch(Dispatchers.Default) {
        for (i in 1..3) {
            println("[Producer | ${Thread.currentThread().name}] Attempting to push Soul #$i into tube...")
            val sendStart = System.currentTimeMillis()
            soulChannel.send("Soul #$i")
            val duration = System.currentTimeMillis() - sendStart
            println("[Producer] Pushed Soul #$i after waiting ${duration}ms (Rendezvous achieved)")
        }
        soulChannel.close()
    }

    // SLOW CONSUMER COROUTINE
    val consumer = launch(Dispatchers.Default) {
        delay(200.milliseconds) // Consumer starts sluggishly
        for (soul in soulChannel) {
            println("  [Consumer | ${Thread.currentThread().name}] Read: $soul")
            delay(200.milliseconds) // Slow processing creates backpressure
        }
    }

    producer.join()
    consumer.join()
}