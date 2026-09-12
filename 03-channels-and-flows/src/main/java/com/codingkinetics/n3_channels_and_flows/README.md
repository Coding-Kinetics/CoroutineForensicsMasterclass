# Level 53 , Quest 4: The Hall of Whispering Scopes

*Leaving the mortal thread crypts behind, the party breaches the Sanctum of Continuation. Here, tasks float across a pool of astral workers. But spellcasters often confuse the vessel (the thread) with the soul (the coroutine), misplacing their circles and leaking mana across dimensions.*

---

## Pre-Flight Check: Roll for Perception

Before altering the runes in `ScopeCheck.kt`, predict the outcomes:

| Checkpoint | The Riddle | Prediction | Actual Runtime Value |
| --- | --- | --- | --- |
| Baseline `scryingWardCheck` | Does `circle.coroutineContext === currentCoroutineContext()` return `true` or `false`? |  |  |
| Planar Suspension | Will the line after `delay(300.milliseconds)` run on the exact same worker thread name? |  |  |
| Delegation Polymorphism | Does `AttunedSpell(baseArtifact).b()` print `10 b`, fail compilation, or throw an error? |  |  |
| Dispatcher Redundancy | Does `withContext(Dispatchers.Default)` inside `communeWithGreatPrime` force a new thread hop? |  |  |

---

## Encounter 3.1: The Baseline Scrying Circle

Open `ScopeCheck.kt`. Run `main()` once to observe the baseline:

```kotlin
/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
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
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    ArrayDeque<Int>().sum() // Red herring artifact

    runBlocking {
        println("Elder-Circle (runBlocking): ${Thread.currentThread().name}")
        scryingWardCheck(this)

        val expedition = launch(Dispatchers.Default) {
            println("Astral-Cohort (launch-Default): ${Thread.currentThread().name}")
            scryingWardCheck(this)

            launch {
                println("Scout-Sprite (nested launch): ${Thread.currentThread().name}")
                scryingWardCheck(this)
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

suspend fun scryingWardCheck(circle: CoroutineScope) {
    val matches = circle.coroutineContext === currentCoroutineContext()
    println("circle: ${circle.coroutineContext}")
    println("ambientContext: ${currentCoroutineContext()}")
    println("Do they share the identical soul-gem? $matches")
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

```

Notice that `scryingWardCheck` reports `true` every time. This creates a dangerous illusion: *“Passing `CoroutineScope` into a suspending function is fine because it's always the same context.”*

Time to break the ward.

---

## Encounter 3.2: Trial of the Severed Tether (Grandparent Scope Bleed)

Inside `main()`, locate the nested `launch` block:

```kotlin
launch {
    println("Scout-Sprite (nested launch): ${Thread.currentThread().name}")
    scryingWardCheck(this)
}

```

### The Mutation

Replace `scryingWardCheck(this)` with the grandparent scope:

```kotlin
scryingWardCheck(this@runBlocking)

```

### Run and Observe

```agsl
circle: [BlockingCoroutine{Active}@..., BlockingEventLoop@...]
ambientContext: [StandaloneCoroutine{Active}@..., Dispatchers.Default]
Do they share the identical soul-gem? false

```

### Grimoire Insight

* `this@runBlocking` refers to the outer blocking event loop on `main`.
* `currentCoroutineContext()` is the child coroutine scheduled on `Dispatchers.Default`.
* If a helper method accepts an arbitrary `CoroutineScope` and launches work onto it, it anchors tasks to an unintended lifecycle and thread pool, evading the caller's cancellation tree.

---

## Encounter 3.3: Trial of the Planar Shift (Dispatcher Mismatch)

A common pattern is calling a helper while switching dispatchers. What happens to context identity when `withContext` is invoked?

### The Mutation

In `expedition`, wrap `scryingWardCheck` inside an I/O realm shift:

```kotlin
val expedition = launch(Dispatchers.Default) {
    // Shift context to IO, but pass the outer Default scope into the check
    withContext(Dispatchers.IO) {
        println("Shifted to IO Realm: ${Thread.currentThread().name}")
        scryingWardCheck(this@launch) // <-- Pass the outer Default scope
    }
    // ...
}

```

### Run and Observe

```agsl
Shifted to IO Realm: DefaultDispatcher-worker-2
circle: [StandaloneCoroutine{Active}@..., Dispatchers.Default]
ambientContext: [DispatchedCoroutine{Active}@..., Dispatchers.IO]
Do they share the identical soul-gem? false

```

### Grimoire Insight

* `currentCoroutineContext()` dynamically reflects the immediate suspension frame (`Dispatchers.IO`).
* The passed-in scope parameter (`this@launch`) remains frozen to `Dispatchers.Default`. Any child routine launched on `circle` silently jumps back to the default dispatcher without the caller knowing.

---

## Encounter 3.4: Trial of the Runaway Familiar (Leaking Concurrency)

Refactor `castDualSummons()` to explore why structured concurrency forbids scope-passing.

### The Mutation

Replace `castDualSummons()` with an uncontained version that accepts a scope parameter:

```kotlin
// BAD: Anti-pattern that leaks concurrency
suspend fun castLeakingSummons(escapeScope: CoroutineScope) {
    val runawayFamiliar = escapeScope.async {
        delay(500.milliseconds)
        println(">>> ESCAPED: Familiar completed outside parent lifecycle! <<<")
        99
    }
}

```

Update `expedition` to invoke it, then immediately let `expedition` complete:

```kotlin
val expedition = launch(Dispatchers.Default) {
    castLeakingSummons(this@runBlocking) // Launching on the long-lived Elder-Circle
    println("Expedition routine completed!")
}

expedition.join()
println("Expedition joined! Returning to tavern...")

```

### Run and Observe

```agsl
Expedition routine completed!
Expedition joined! Returning to tavern...
>>> ESCAPED: Familiar completed outside parent lifecycle! <<<

```

### Grimoire Insight

* `expedition.join()` finished and the party returned to the tavern, yet `runawayFamiliar` was still running loose in the background.
* By passing `this@runBlocking` instead of confining the work with `coroutineScope { }`, the child async job detached from the parent `expedition`. In production, this causes ghost coroutines, leaked HTTP connections, and memory growth.

---

## Arcane Rules of Engagement (Summary)

* **Ban `suspend fun doWork(scope: CoroutineScope)`:** If a function is suspending, it must derive concurrency through `coroutineScope { }` or read `currentCoroutineContext()`.
* **Reserve `CoroutineScope` for Boundaries:** Only pass scopes into constructors or non-suspending boundary classes (e.g., ViewModels, Presenters, Service daemons) that manage an explicit lifecycle.
* **Suspension is Non-Affinity:** Resuming after `delay()` or I/O does not guarantee the same physical thread, only the same dispatcher rules.

---

*Loot Claimed: **The Amulet of Continuation** (+2 to Scope-Leak Detection, Immunity to Stale Scope Passing).*