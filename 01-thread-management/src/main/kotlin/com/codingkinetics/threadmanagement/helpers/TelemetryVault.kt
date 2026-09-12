package com.codingkinetics.threadmanagement.helpers

import com.codingkinetics.sev01_toolkit.utils.ThreadForensics
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * OBJECTIVES COVERED:
 * - Shared mutable state across JVM thread boundaries
 * - Forensic thread inspection via [com.codingkinetics.sev01_toolkit.utils.ThreadForensics]
 * - Contention remediation via bounded lock timeouts (fail-fast / load shedding)
 *
 * ROLE: Shared Infrastructure & Lab 1E Target
 */
object TelemetryVault {
    val ledger = HashMap<String, Int>()
    val lock = ReentrantLock()

    fun reset() {
        ledger.clear()
    }

    fun logForensics(message: String) {
        ThreadForensics.log(message, ledger)
    }

    /**
     * BROKEN BASELINE (Used in Demo 1D): Unconditional lock acquisition.
     */
    fun flushUnbounded(district: String, durationMs: Long) {
        lock.lock()
        try {
            logForensics("ACQUIRED: Exclusive lock held by $district")
            Thread.sleep(durationMs)
        } finally {
            lock.unlock()
            logForensics("RELEASED: Lock relinquished by $district")
        }
    }

    /**
     * LAB 1E (Student Action): Bounded deadline with fail-fast load shedding.
     */
    fun flushWithDeadline(district: String, durationMs: Long, timeoutMs: Long): Boolean {
        // TODO (Lab 1E): Acquire lock within timeoutMs using lock.tryLock().
        val acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
        if (!acquired) {
            logForensics("SHED LOAD: $district timed out after ${timeoutMs}ms. Aborting to protect SLA.")
            return false
        }

        try {
            logForensics("ACQUIRED: Access granted to $district")
            Thread.sleep(durationMs)
            return true
        } finally {
            lock.unlock()
            logForensics("RELEASED: Lock relinquished by $district")
        }
    }
}