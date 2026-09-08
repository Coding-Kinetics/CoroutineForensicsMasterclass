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

package com.codingkinetics.sev01_toolkit.utils

import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.time.Instant

/**
 * Diagnostics & thread state inspector for concurrent labs.
 *
 * USAGE EXAMPLES:
 *
 * 1. Fast inline telemetry (use inside worker loops / run blocks):
 *    ThreadForensics.log("Ingested record $pingIndex", aggregatedEvents)
 *
 * 2. External thread inspection (use from main/test thread to see why a worker hangs):
 *    val worker = Thread { ... }.apply { start() }
 *    ThreadForensics.inspectThread(worker)
 *
 * 3. JVM-wide lock dump (use in timeout handlers or catch blocks):
 *    ThreadForensics.dumpContendedThreads()
 */
object ThreadForensics {

    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean()

    /**
     * Standard inline logger for current thread execution.
     * Displays timestamp, thread name, TID, JVM state, priority, and interrupted bit.
     *
     * Example:
     *   ThreadForensics.log("Processing batch item", mapOf("batchId" to 42))
     *
     * Output:
     *   [2026-09-05T08:45:00Z] [pool-1-thread-1 (TID:24) | RUNNABLE | Prio:5]: Processing batch item | State: {batchId=42}
     */
    fun log(message: String, context: Any? = null) {
        val t = Thread.currentThread()
        val interrupted = if (t.isInterrupted) "[INTERRUPTED]" else ""
        val ctxString = context?.let { " | State: $it" } ?: ""

        println(
            "[${Instant.now()}] [${t.name} (TID:${t.id}) | ${t.state} | Prio:${t.priority}] $interrupted: $message$ctxString"
        )
    }

    /**
     * Inspects and logs the exact state, lock monitor info, and stack frame of an external target thread.
     *
     * Example:
     *   val criticalProbe = Thread { MetricSink.flush() }
     *   criticalProbe.start()
     *   Thread.sleep(100) // let it hit the lock
     *   ThreadForensics.inspectThread(criticalProbe)
     *
     * Output:
     *   ====================== THREAD INSPECTOR ======================
     *   Thread:       critical-probe (TID: 28)
     *   JVM State:    WAITING | Waiting on lock: ReentrantLock (held by: bg-worker [TID:27])
     *   Blocked:      Count: 0 | Waited: 1
     *   Call Stack:
     *     at jdk.internal.misc.Unsafe.park(Native Method)
     *     at java.util.concurrent.locks.LockSupport.park(...)
     *   =============================================================
     */
    fun inspectThread(target: Thread, maxStackDepth: Int = 3) {
        val info: ThreadInfo? = threadBean.getThreadInfo(target.id, maxStackDepth)

        if (info == null) {
            println("[FORENSICS] Target Thread [${target.name} (TID:${target.id})] has terminated or is invalid.")
            return
        }

        val lockDetails = buildString {
            if (info.lockName != null) {
                append(" | Waiting on lock: ${info.lockName}")
                if (info.lockOwnerName != null) {
                    append(" (held by: ${info.lockOwnerName} [TID:${info.lockOwnerId}])")
                }
            }
            if (info.isSuspended) append(" [SUSPENDED]")
            if (info.isInNative) append(" [IN_NATIVE]")
        }

        val stackSnapshot = info.stackTrace.take(maxStackDepth).joinToString("\n      ") { frame ->
            "at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})"
        }

        println(
            """
            ====================== THREAD INSPECTOR ======================
            Thread:       ${info.threadName} (TID: ${info.threadId})
            JVM State:    ${info.threadState}$lockDetails
            Blocked:      Count: ${info.blockedCount} | Waited: ${info.waitedCount}
            Call Stack:
              $stackSnapshot
            =============================================================
            """.trimIndent()
        )
    }

    /**
     * Dumps all threads currently in BLOCKED or WAITING states across the entire JVM.
     */
    fun dumpContendedThreads() {
        val deadlockedThreadIds = threadBean.findDeadlockedThreads()
        if (!deadlockedThreadIds.isNullOrEmpty()) {
            System.err.println("CRITICAL: Deadlock detected across threads: ${deadlockedThreadIds.joinToString()}")
        }

        val allInfos = threadBean.dumpAllThreads(false, false)
        val blockedThreads = allInfos.filter {
            it.threadState in setOf(Thread.State.BLOCKED, Thread.State.WAITING, Thread.State.TIMED_WAITING)
        }

        println("[FORENSICS] Contended Threads (${blockedThreads.size}/${allInfos.size}):")
        blockedThreads.forEach { t ->
            println("  -> [${t.threadName} | TID:${t.threadId}] State: ${t.threadState} | Lock: ${t.lockName ?: "None"} | Lock Owner: ${t.lockOwnerName ?: "None"}")
        }
    }
}