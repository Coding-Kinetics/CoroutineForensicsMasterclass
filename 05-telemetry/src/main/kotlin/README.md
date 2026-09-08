## Lab 05 FINAL BOSS: Full-Stack Incident Triage & Telemetry

The party reaches the deepest vault of the Catacombs, where the stone arches crack open to reveal something far more terrifying than goblins or liches: a humming, cold-lit terminal in a modern operations room. The fantasy collapses into the waking nightmare of every engineer.

The dungeon master's screen is replaced by a PagerDuty siren. The goblin hordes were never an abstraction; they were thousands of unbuffered concurrent client webhooks pounding the production cluster. The Lich wasn't cast in folklore; it was an unconfined coroutine leak bleeding CPU cycles across worker pools until the JVM collapsed.

A production incident is underway for a full-stack Ktor chat. Your mission is to stand up the observability pipeline, generate traffic, inspect live dashboard metrics, and isolate the bottleneck. Hundreds of customers are reporting the issue.

### Incident Briefing: Call at 3:00AM

1. **What is the problem?** The app gets laggy and slow and now it's unusable
2. **When did it start?** Today
3. **Affected Platforms:** All platforms
4. **Easy to Reproduce?:** Not easy to reproduce

### 1. Clone & Switch Branches

In a separate terminal window, clone the target repository and checkout the telemetry challenge branch:

```bash
git clone [https://github.com/ahinchman1/ktor-chat.git](https://github.com/ahinchman1/ktor-chat.git)
cd ktor-chat
git checkout challenge/workshop_challenge_5_telemetry
```

### 2. Launch the Environment & Begin Triage

Follow the setup and incident triage instructions located directly in the challenge README:

- **Guide Location:** core/src/commonMain/kotlin/telemetry/README.md
- **Stack Prerequisite:** Docker Compose (Colima / Docker Desktop) - more details in README
- **Objective:** Spin up the Docker stack (`docker compose up -d`), launch the REST server and desktop client, open the Grafana dashboard at http://localhost:3000, and diagnose the reported customer issue.

### 3. The Mission:

- Spin up the diagnostic scrying array (Prometheus + Grafana).
- Synthesize Perfetto traces and live JVM thread metrics under synthetic load.
- Locate the offending coroutine leak or blocked dispatcher in the Ktor routing pipeline.
- Ship the patch, stabilize the cluster, and clear the queue before memory limits force an OOM crash.

### Party Roles - Specialization Check

- **The Paladin** (Platform Architect / Lead): Coordinates triage order, manages communication lines, and confirms root-cause verification before approving rollback or deployment.
- **The Diviner** (Telemetry Specialist): Reads Grafana dashboard runes, correlates dispatcher queue depth against thread pool exhaustion, and maps the timeline of the spike.
- **The Chronomancer** (Forensics & Perfetto Specialist): Pulls runtime thread dumps, inspects continuation suspensions, and locates the exact coroutine stuck in an uncooperative, blocking loop.

### Victory Conditions (Claiming the Sev01 Grimoire)
The boss is defeated only when the party moves from speculation to empirical proof:

- [ ] **Telemetry Online:** Prometheus and Grafana instances collecting live Ktor metrics.
- [ ] **Load Reproduction:** Traffic generator reproduces the starvation curve and thread stall.
- [ ] **Forensic Identification:** The exact file, route, and anti-pattern causing thread pool exhaustion are documented with supporting telemetry evidence.
- [ ] **Mitigation Verified:** The patch restores throughput, drops queue depth back to baseline, and proves non-blocking resilience under load.