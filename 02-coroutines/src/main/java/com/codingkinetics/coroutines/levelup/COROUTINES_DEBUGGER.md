## Skill 1 - Arcane Tooling: Summoning the Coroutine Inspector

Before re-attempting the next encounters, attune your development environment to look beneath the high-level Kotlin syntax:

#### Step 1: Project Alignment (Gradle Sync)
Ensure `kotlinx-coroutines-debug` is resolved across all workshop modules:

```Kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.1")
}
```

After adding or pulling changes, trigger a Gradle Sync in IntelliJ.

#### Step 2: Activating the Inspector

1. In `ScopeCheck.kt`, place a standard line breakpoint inside `scopeCheck`.

> **Warning from the DM:** Avoid placing method-level breakpoints on the suspend fun declaration line. Method breakpoints severely degrade coroutine frame extraction. Use line breakpoints inside the function body instead.

2. Click the **Debug** icon next to `fun main()`.
3. In IntelliJ's bottom **Debug** tool window:
    - Select the **Threads & Variables** tab (switch away from the plain Console tab).
    - Notice the dedicated **Coroutines** panel alongside the call stack:
        - Running coroutines will display their active dispatchers and job hierarchies.
        - Suspended coroutines will reveal their preserved continuation states.

4. Switch back to the **Console** tab to verify that thread names now carry coroutine identifiers:

````Plaintext
Elder-Circle (runBlocking): main @coroutine#1
Astral-Cohort (launch-Default): DefaultDispatcher-worker-1 @coroutine#2
````

#### Skill 2 - Astral Tracking (`-Dkotlinx.coroutines.debug`)

*By default, thread logs only show OS pool names like `DefaultDispatcher-worker-1`. The debug agent binds coroutine identities directly to thread names.*

1. In IntelliJ, open **Run/Debug Configurations** for `ScopeCheck.kt`.
2. Add to **VM options**:
```text
-Dkotlinx.coroutines.debug

```


3. Run `main()` again. Notice console thread tags now include coroutine IDs (e.g., `DefaultDispatcher-worker-1 @coroutine#2`).
