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

package com.codingkinetics.sev01_toolkit.telemetry

import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean

object ThreadDiagnostics {

    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean().apply {
        if (isThreadContentionMonitoringSupported) {
            isThreadContentionMonitoringEnabled = true
        }
    }

    data class ThreadStateSummary(
        val total: Int,
        val runnable: Int,
        val blocked: Int,
        val waiting: Int,
        val timedWaiting: Int,
        val deadlockedThreadIds: LongArray?
    )

    /**
     * Inspects active threads and prints a summary + lock contention breakdown.
     */
    fun analyzeThreadStates(printDetails: Boolean = false): ThreadStateSummary {
        val threadInfos: Array<ThreadInfo> = threadBean.dumpAllThreads(true, true)
        val deadlockedIds = threadBean.findDeadlockedThreads()

        var runnable = 0
        var blocked = 0
        var waiting = 0
        var timedWaiting = 0

        for (info in threadInfos) {
            when (info.threadState) {
                Thread.State.RUNNABLE -> runnable++
                Thread.State.BLOCKED -> blocked++
                Thread.State.WAITING -> waiting++
                Thread.State.TIMED_WAITING -> timedWaiting++
                else -> {}
            }
        }

        val summary = ThreadStateSummary(
            total = threadInfos.size,
            runnable = runnable,
            blocked = blocked,
            waiting = waiting,
            timedWaiting = timedWaiting,
            deadlockedThreadIds = deadlockedIds
        )

        println(
            """
            ================ THREAD DIAGNOSTICS ================
            Total Threads: ${summary.total}
            RUNNABLE     : ${summary.runnable}
            BLOCKED      : ${summary.blocked}
            WAITING      : ${summary.waiting}
            TIMED_WAITING: ${summary.timedWaiting}
            Deadlocks    : ${if (deadlockedIds != null) "${deadlockedIds.size} THREADS DEADLOCKED!" else "None"}
            ====================================================
            """.trimIndent()
        )

        if (summary.blocked > 0 || deadlockedIds != null || printDetails) {
            println("Thread Breakdown:")
            threadInfos
                .filter { it.threadState == Thread.State.BLOCKED || printDetails }
                .forEach { info ->
                    println("-> [${info.threadState}] \"${info.threadName}\" (id=${info.threadId})")
                    if (info.lockName != null) {
                        println("   Waiting on lock: ${info.lockName} (Held by: ${info.lockOwnerName ?: "Unknown"}, ID: ${info.lockOwnerId})")
                    }
                    if (printDetails) {
                        info.stackTrace.take(5).forEach { frame ->
                            println("       at $frame")
                        }
                    }
                }
        }

        return summary
    }
}