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

package com.codingkinetics.sev01_toolkit.coroutine_debug_unit_test_debuggers

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.println

class VaccinationPen(
    private val category: String = "coroutine",
) {
    private val events = ConcurrentLinkedQueue<String>()
    private val startTimeUs = System.nanoTime() / 1000

    fun traceBlock(name: String, block: () -> Unit) {
        val start = (System.nanoTime() / 1000) - startTimeUs
        val tid = Thread.currentThread().id
        try {
            block()
        } finally {
            val end = (System.nanoTime() / 1000) - startTimeUs
            val duration = end - start
            events.add("""{"name":"$name","cat":"$category","ph":"X","ts":$start,"dur":$duration,"pid":1,"tid":$tid}""")
        }
    }

    fun exportToFile(file: File) {
        val json = events.joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]")
        file.writeText(json)
        println("[+] Perfetto trace written to: ${file.absolutePath} (Drag into ui.perfetto.dev)")
    }
}
