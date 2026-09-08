#!/usr/bin/env bash
# ==============================================================================
# Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

set -euo pipefail

# ==============================================================================
# STEP 1: ELEVATE FILE PERMISSIONS FOR TRACEFS
# ==============================================================================
# The Ktor JVM needs write access to the kernel's trace_marker pseudo-device.
# By default, only root can write to it. Run this once before capturing:
echo "[*] Granting write permissions to trace_marker..."
if [ -e /sys/kernel/tracing/trace_marker ]; then
    sudo chmod 666 /sys/kernel/tracing/trace_marker
elif [ -e /sys/kernel/debug/tracing/trace_marker ]; then
    sudo chmod 666 /sys/kernel/debug/tracing/trace_marker
else
    echo "[-] Error: tracefs trace_marker not found. Ensure ftrace is mounted."
    exit 1
fi

# ==============================================================================
# STEP 2: RUN PERFETTO TRACE CAPTURE
# ==============================================================================
# - Runs for duration_ms (10 seconds)
# - Captures thread scheduling states, process tables, and custom trace_marker prints
# - Writes binary trace to /tmp/ktor_trace.perfetto-trace
#
# NOTE: While this is running, hit your Ktor server with traffic (e.g. curl/wrk/autocannon)
echo "[*] Recording trace for 10 seconds. Generate Ktor traffic now..."

perfetto \
  -c - --txt \
  -o /tmp/ktor_trace.perfetto-trace <<EOF
buffers: {
    size_kb: 65536
    fill_policy: RING_BUFFER
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            # Low-level Linux scheduler context switches & thread wakeups
            ftrace_events: "sched/sched_switch"
            ftrace_events: "sched/sched_wakeup"
            ftrace_events: "sched/sched_process_exit"
            ftrace_events: "power/cpu_frequency"

            # CRITICAL: Captures userspace writes to trace_marker (Ftrace.begin/end)
            ftrace_events: "ftrace/print"
        }
    }
}
data_sources: {
    config {
        name: "linux.process_stats"
        process_stats_config {
            proc_per_process: true
            scan_all_processes_on_start: true
        }
    }
}
duration_ms: 10000
EOF

echo "[+] Trace capture complete: /tmp/ktor_trace.perfetto-trace"

# ==============================================================================
# STEP 3: HOW TO LOAD & INSPECT IN UI.PERFETTO.DEV
# ==============================================================================
# 1. Open Google Chrome and navigate to: https://ui.perfetto.dev
# 2. In the top-left sidebar, click "Open trace file" (or drag & drop /tmp/ktor_trace.perfetto-trace)
# 3. Locate your Ktor tracks:
#    - Press "/" on your keyboard to open the track search bar.
#    - Filter by your JVM process name (e.g., "java", "MainKt") or PID.
# 4. Inspect threads:
#    - Expand the process track to view "DefaultDispatcher-worker-*" and Netty loops.
#    - Custom Ktor spans (e.g., "Ktor: GET /endpoint") will appear inside the thread track.
#    - Green bars = Running on CPU core
#    - Blue bars  = Runnable (waiting for CPU core / starvation)
#    - Brown bars = Sleeping (blocked on socket I/O, locks, or channels)