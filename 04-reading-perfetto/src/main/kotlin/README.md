# Lab 04 BOSS: The Chronomancer’s Split Crucible (A Tale of Two Halves)

Up until now, you have fought in isolated training chambers—probing standalone coroutines and single-threaded test harnesses. That era is over.

Before you stands the gateway to the actual production citadel: the full-stack Ktor Chat monorepo. This is a live multiplatform beast with an Android client, a concurrent Ktor server, shared common modules, and multiple interacting thread pools. You cannot tackle the Sev01 Final Boss at 3:00 AM without first learning how to navigate this larger codebase and seeing across the wire.

To prepare for the final siege, the party must split forces across the Kotlin Multiplatform boundary. One half will dive into the Android client to hunt down memory leaks and UI stalls; the other half will instrument the Ktor backend using the provided sev01 ftrace kit. Once both sides extract their telemetry scrolls, we will convene at the central scrying pool (ui.perfetto.dev) to correlate the graphs together.

## Incident Briefing: The Distributed Desync

- **The Scout’s Warning (Frontend):** The handheld client stutters and hitches under sustained use. Frames drop, scrolling freezes, and prolonged chat sessions eventually crash the process with an apparent memory leak.
- **The Watchtower’s Log (Backend):** The Ktor server reports healthy ping responses, yet real client dispatches sit in limbo. Worker threads are busy, but without low-level tracing, the backend is blind to where continuations are stalling.
- **The Quest Objective:** Graduate into the full-stack repo and split the investigation. Flank A captures client-side memory bloat and GC pauses on Android; Flank B instruments Ktor routes with ftrace markers to reveal dispatcher hops. Both flanks bring their trace JSONs together to complete the end-to-end picture.

## 1. Claim the Citadel Repository
Leave your sandbox workspaces behind. Clone the main multiplatform battlefield and switch to the Lab 04 trial branch:

```Bash
git clone https://github.com/ahinchman1/ktor-chat.git
cd ktor-chat
```

Open up the project in Android Studio, and take two minutes to inspect the broader realm structure:

- `composeApp/` (or `androidApp/`): The mobile client entry point.
- `server/`: The Ktor routing engine and netty/cio server pipeline.
- `core/src/commonMain/kotlin/telemetry/sev01/`: The ftrace instrumentation primitives you will integrate.

## 2. Choose Your Flank

### FLANK A: The Android Forge (Client Forensics & Leak Hunters)

_Your mission: Catch the allocation bloat, frame stalls, and memory leaks lurking in the Android client before an Out-Of-Memory dragon devours the process._

- **Target Realm:** `composeApp/` or `androidApp/`
#### The Ritual:
- Boot the Android client on an emulator or tethered device with developer options enabled.
- Record a system trace via Android Studio Profiler or terminal command while actively stressing the UI (switching chat channels, spamming messages, rotating screens) - see code block below

```Bash
record_android_trace -o /tmp/android_client.pftrace -t 10s -b 64mb am binder_driver dalvik freq idle sched sync view
```
- Once your trace loads in [ui.perfetto.dev](https://ui.perfetto.dev) or Android Studio Profiler, answer the following questions in the section below.

#### Deliverable:
- A `.pftrace` / exported `.json` highlighting a suspicious frame drop or heap growth curve.



- **Frame Budget Breaches:** Look at the Choreographer#doFrame slice track. Are you seeing slices stretching well past the 16.6ms (60Hz) or 8.3ms (120Hz) mark? What work is active on the Main/UI thread during that elongated frame—is it heavy layout/recomposition or JSON deserialization?
- **GC Suspension Pressure**: Search the trace slices for dalvik.vm.gc or garbage collector. How often is the runtime calling a "Stop the World" pause? Are allocations climbing in a sawtooth pattern that never returns to baseline after a manual GC trigger?
- **The Retained Ghost Check:** When you leave or toggle a chat room, does the retained memory heap stay elevated? Look at active thread allocations—is a coroutine or listener still referencing the destroyed screen/ViewModel?
- **Main Thread Theft:** Expand your app's main thread track. Look for blocking calls like Thread.sleep(), disk I/O, or unconfined database queries running directly on Dispatchers.Main instead of offloaded to a worker pool.

### FLANK B: The Kernel Conduit (Backend Ftrace & Span Weavers)

_Your mission: Plumb the Ktor chat engine into Linux ftrace using the sev01 toolkit so invisible dispatcher hops become visible kernel slices._

- **Target Realm:** `server/` and `core/src/commonMain/kotlin/telemetry/sev01/`
- **The Ritual:**
    - Ensure the kernel trace gate is writable:
```Bash
sudo chmod 666 /sys/kernel/tracing/trace_marker
```
- Wire the `PerfettoTracing` application plugin into `Application.module()`, bracketing inbound endpoints with async span cookies (`S|...` and `F|...`).
- Spin up the Ktor service, trigger traffic with curl, and capture the trace:
```Bash
perfetto -c core/telemetry/record_config.pbtxt -o /tmp/ktor_backend.pftrace
traceconv json /tmp/ktor_backend.pftrace /tmp/ktor_backend.json
```

#### Ritual Instructions:

**Set up the environment:** 

- Start the Ktor backend: `./gradlew :server:rest:run`
- Start backing services: `docker compose up -d` (from grafana-setup/)
- Verify Prometheus is scraping: curl http://localhost:8080/metrics
- Open Grafana at http://localhost:3000 and navigate to the Ktor & JVM Service Metrics dashboard.

**Burst Traffic Generation**
- Launch a desktop client or 2: `./gradlew :app:desktop:run`
- Log in with 2–3 clients (can be same or different users).
- Join the same room on all clients.
- Rapidly send messages in the room for ~30–60 seconds (multiple messages per second, across conversational turns).


#### Deliverable
A .json trace showing valid start/finish spans aligned with kernel sched_switch events.

**Once you load your Ktor ftrace JSON into the scrying pool, answer these:**
1. **Is what you see what you would expect from app behavior?** Look at the visual contrast in the example trace: most operations are tiny, vertical slivers. When you run your workload, do your traces show consistent slice widths, or do you see massive outliers?  Are there threads that suddenly stretch across half the screen? If a slice is unusually wide, what questions should you ask about whether that thread is doing heavy calculation or simply stuck waiting?
2. **Dispatcher Hop Delays:** Zoom in between the moment a request starts and when your route handler actually begins executing. Do you see a gap where the thread yields via sched_switch and the continuation sits in a runnable queue waiting for an idle worker thread?
3. **Thread Contention & Stealing:** Expand the CPU cores at the top of the timeline. Is your Ktor server ping-ponging work across different cores unnecessarily, or is a single worker thread pinned at 100% while others sit idle?
4. **Blocking on Non-Blocking:** Look at the thread names. Are any of your recorded threads running on a dedicated Netty/CIO event loop thread (i.e. `eventLoopGroup-xxx`) or an internal coroutine dispatcher? Did someone slip a blocking database/network call onto a thread that shouldn't block?

### 3. The Grand Council
Once your flank has captured its trace, do not hoard the scrolls. The exercise concludes with a cross-table debrief:

- **Flank A projects their Android trace:** Where did the allocations spike? Did the UI thread freeze waiting on an unconfined background job?
- **Flank B projects their Ktor trace:** Show your perfetto trace and discuss the questions made in the Deliverable section.
- **The Synthesis:** We map client frame-drops against backend latency spikes to prove causality before entering the Final Boss arena.

## Victory Conditions

- [ ] **Flank A Clear:** Android client trace successfully recorded and analyzed for GC churn or retained memory references.
- [ ] **Flank B Clear:** Ktor backend writes valid asynchronous `ftrace` markers captured via Perfetto daemon and analyzes what they pick up.
- [ ] **The Council Convenes:** One champion from Flank A and one from Flank B present their graphs to the room for joint dissection to present their findings on forensics

## Post-Exercise Challenge

**Merging Both in Perfetto UI**
Instead of inspecting the app and the server in two different browser tabs, attendees can view the full end-to-end transaction together:

1. In Perfetto UI (`ui.perfetto.dev`), open the left sidebar.
2. Click "Open multiple trace files" (or use the multi-trace command).
3. Load both:
   - **trace.perfetto-trace** (from the ADB script)
    - **build/perfetto_trace.json** (from the Ktor server)

Perfetto will align them along a shared timeline (or allow visual correlation between the client's network call and the backend's Ktor: POST /messages slice).