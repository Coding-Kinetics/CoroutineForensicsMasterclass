/*
 * Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
 */
package com.codingkinetics.threadmanagement.lock_contention_02

import com.codingkinetics.sev01_toolkit.utils.ThreadForensics
import java.lang.Thread.sleep
import java.util.concurrent.locks.ReentrantLock

enum class IngestionPriority { LOW, HIGH }

data class IngestionBatch(
    val source: String,
    val priority: IngestionPriority,
    val workDurationMs: Long
)

/**
 * Shared downstream sink.
 * Exercise focus: Diagnose untimed lock acquisition and priority inversion.
 */
class TelemetrySink {
    val lock = ReentrantLock()

    // BROKEN BASELINE: Unbounded, un-timed synchronization
    fun flush(batch: IngestionBatch): Boolean {
        ThreadForensics.log("Attempting lock acquisition for ${batch.source}")

        lock.lock() // TODO: replace with tryLock() deadline and return false if timed out
        try {
            ThreadForensics.log("LOCKED: Processing ${batch.source} (${batch.workDurationMs}ms)")
            sleep(batch.workDurationMs) // Simulates hanging downstream I/O or network call
            return true
        } finally {
            lock.unlock()
            ThreadForensics.log("UNLOCKED: Completed ${batch.source}")
        }
    }
}