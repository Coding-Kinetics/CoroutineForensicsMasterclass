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

package com.codingkinetics.coroutines.scopes_01

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    ArrayDeque<Int>().sum() // Red herring artifact

    runBlocking {
        println("Elder-Circle (runBlocking): ${Thread.currentThread().name}")
        scopeCheck(this)

        val expedition = launch(Dispatchers.Default) {
            println("Astral-Cohort (launch-Default): ${Thread.currentThread().name}")
            scopeCheck(this)

            launch {
                println("Scout-Sprite (nested launch): ${Thread.currentThread().name}")
                scopeCheck(this)
            }

            delay(300.milliseconds)

            println("Post-Trance: ${Thread.currentThread().name}")
            castDualSummons()
            communeWithGreatPrime()
            println("Expedition-End: ${Thread.currentThread().name}")
        }

        expedition.join()
    }

    val baseArtifact = BaseSpellImpl(10)
    AttunedSpell(baseArtifact).a()
    AttunedSpell(baseArtifact).b()
}

suspend fun scopeCheck(scope: CoroutineScope) {
    println("scope: ${scope.coroutineContext} | coroutineContext: ${currentCoroutineContext()}")
    println("is the same context? ${scope.coroutineContext === currentCoroutineContext()}")
    println()
}

suspend fun castDualSummons() = coroutineScope {
    val familiarOne = async {
        println("Familiar-1 channeled on: ${Thread.currentThread().name}")
        4 * 2
    }
    val familiarTwo = async { 2 * 5 }
    println("Familiars returned essences: ${familiarOne.await()}, ${familiarTwo.await()}")
}

suspend fun communeWithGreatPrime(): BigInteger = withContext(Dispatchers.Default) {
    BigInteger.probablePrime(4096, Random())
}

interface SpellScroll {
    fun a()
    fun b()
}

class BaseSpellImpl(val powerLevel: Int) : SpellScroll {
    override fun a() { println("$powerLevel a") }
    override fun b() { println("$powerLevel b") }
}

class AttunedSpell(scroll: SpellScroll) : SpellScroll by scroll {
    override fun a() { println("abc") }
}