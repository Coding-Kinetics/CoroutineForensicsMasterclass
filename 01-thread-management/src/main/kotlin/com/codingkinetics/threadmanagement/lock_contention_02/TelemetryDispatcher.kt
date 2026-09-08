/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 */
package com.codingkinetics.threadmanagement.lock_contention_02

import com.codingkinetics.sev01_toolkit.utils.ThreadForensics
import java.lang.Thread.sleep

fun main() {
    val sharedSink = TelemetrySink()

    // 1. Low-priority thread running a heavy, slow batch
    val backgroundWorker = Thread({
        val heavyBatch = IngestionBatch("analytics-sync", IngestionPriority.LOW, 4000)
        sharedSink.flush(heavyBatch)
    }, "bg-batch-worker").apply {
        priority = Thread.MIN_PRIORITY
    }

    // 2. High-priority thread needing fast turn-around
    val criticalReporter = Thread({
        // Brief sleep to guarantee the background thread grabs the lock first
        sleep(100)
        val urgentBatch = IngestionBatch("service-heartbeat", IngestionPriority.HIGH, 25)
        sharedSink.flush(urgentBatch)
    }, "critical-reporter").apply {
        priority = Thread.MAX_PRIORITY
    }

    println("--- STARTING TELEMETRY PIPELINE ---")
    backgroundWorker.start()
    criticalReporter.start()

    // 3. Observer probe: inspect thread state while contending
    sleep(300)
    println("\n>>> TRIGGERING RUNTIME FORENSICS PROBE <<<")
    ThreadForensics.inspectThread(criticalReporter)
    println("-----------------------------------------\n")

    backgroundWorker.join()
    criticalReporter.join()
    println("--- PIPELINE FINISHED ---")
}