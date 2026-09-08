## Guided Inspection Steps

1. **Inspect Continuation Passing:**
* Place a breakpoint inside `scopeCheck`.
* Examine the stack frame in the debugger: locate the hidden `$completion` (`Continuation`) parameter injected by the compiler.
* Compare `$completion.context` with the parameter `scope.coroutineContext`.

2. **Observe Thread Hopping:**
* Run the snippet with JVM flag: `-Dkotlinx.coroutines.debug`.
* Observe how the coroutine names (`@coroutine#2`, etc.) attach to the thread names in the console logs.
* Note the thread ID before and after `delay(300)`.