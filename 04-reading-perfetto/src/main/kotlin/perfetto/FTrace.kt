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

package main.kotlin.perfetto

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintWriter

object Ftrace {
    private val ftraceMarkerFile = File("/sys/kernel/tracing/trace_marker")
    private val isLinuxFtrace = ftraceMarkerFile.exists() && ftraceMarkerFile.canWrite()

    private val marker: OutputStream? = if (isLinuxFtrace) {
        FileOutputStream(ftraceMarkerFile, true)
    } else {
        null
    }

    private val traceFile by lazy {
        if (!isLinuxFtrace) {
            val projectRoot = findProjectRoot()
            File(projectRoot, "build/perfetto_trace.json").apply {
                parentFile?.mkdirs()
                if (!exists()) writeText("[\n")
            }
        } else {
            null
        }
    }

    private val writer: PrintWriter? by lazy {
        if (isLinuxFtrace) {
            PrintWriter(marker!!, true)
        } else {
            PrintWriter(FileOutputStream(traceFile!!, true), true)
        }
    }

    private fun findProjectRoot(): File {
        var dir = File(System.getProperty("user.dir"))
        while (dir != null && !File(dir, ".git").exists() && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir ?: File(System.getProperty("user.dir"))
    }

    private fun currentTimeMicros(): Long = System.nanoTime() / 1000
    private val pid: Long = ProcessHandle.current().pid()

    inline fun <T> trace(sectionName: String, block: () -> T): T {
        begin(sectionName)
        return try {
            block()
        } finally {
            end()
        }
    }

    fun begin(sectionName: String) {
        writer?.let {
            if (marker != null) {
                it.write("B|$pid|$sectionName\n")
                it.flush()
            } else {
                val tid = Thread.currentThread().threadId()
                it.println("""{"name":"$sectionName","ph":"B","ts":${currentTimeMicros()},"pid":$pid,"tid":$tid},""")
                it.flush()
            }
        }
    }

    fun end() {
        writer?.let {
            if (marker != null) {
                it.write("E\n")
                it.flush()
            } else {
                val tid = Thread.currentThread().threadId()
                it.println("""{"ph":"E","ts":${currentTimeMicros()},"pid":$pid,"tid":$tid},""")
                it.flush()
            }
        }
    }

    @PublishedApi
    internal fun rescueProcessId(): Long = ProcessHandle.current().pid()

    fun close() {
        if (marker == null) {
            writer?.let {
                val tid = 1L
                it.println("""{"name":"end","ph":"I","ts":${currentTimeMicros()},"pid":$pid,"tid":$tid}""")
                it.println("]")
                it.flush()
                it.close()
            }
        }
    }
}