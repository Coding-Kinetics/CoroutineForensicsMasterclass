

class Test {

    @Test
    fun testReproduceStarvation() = runTest {
        // 1. Trigger concurrent payload/pipeline workload
        val job = launchWorkload()

        delay(200) // allow contention to form

        // 2. Dump telemetry
        val summary = ThreadDiagnostics.analyzeThreadStates(printDetails = true)

        // 3. Assert baseline expectations
        assertTrue(summary.blocked == 0, "Expected zero blocked worker threads")

        job.cancel()
    }
}