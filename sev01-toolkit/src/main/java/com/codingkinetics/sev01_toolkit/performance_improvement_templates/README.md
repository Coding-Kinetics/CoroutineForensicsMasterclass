<!--
  Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.

  COMMERCIAL WORKSHOP LICENSE:
  This material is proprietary and copyrighted by Coding Kinetics LLC. 
  By purchasing access to this workshop, you and your organization are granted 
  a perpetual, non-exclusive, internal-use license to use, modify, adapt, 
  and integrate this template, documentation, and associated tooling within 
  your internal systems and workflows as you see fit.

  Redistribution, public hosting, reselling, or sublicensing of this material 
  as a standalone commercial product or training offering without prior written 
  permission from Coding Kinetics LLC is strictly prohibited.
-->

# Post-Incident Performance & ROI Assessment

**Incident Reference:** SEV-01: [Incident Title/Ticket ID]  
**Remediation Date:** YYYY-MM-DD  
**Evaluation Window:** [e.g., 7 days pre-fix vs. 7 days post-fix]  
**Lead Engineer / Team:** [Name / Team Handle]

---

### 1. Executive Summary & Impact
* **Core Problem:** Brief 1–2 sentence recap of the root bottleneck (e.g., thread exhaustion, memory leak, unindexed query cascade).
* **Remediation Implemented:** Specific architectural or code fix deployed.
* **High-Level ROI:** [e.g., "Eliminated $12k/mo in runaway container compute, reduced checkout p99 latency by 64%, and recovered an estimated ~2.3% in dropped transaction revenue."]

---

### 2. Systems & Compute Delta

| Metric | Pre-Fix Baseline | Post-Fix (24h) | Post-Fix (7d Sustained) | % Delta | Source / Dashboard |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CPU Utilization (Avg / Peak)** | 85% / 100% | 38% / 55% | 40% / 52% | -53% | [Link] |
| **Memory (RSS / Heap Peak)** | 14.2 GB (OOM) | 4.1 GB | 4.3 GB | -70% | [Link] |
| **p95 Latency** | 1,850 ms | 210 ms | 195 ms | -89% | [Link] |
| **p99 Latency** | 6,200 ms | 480 ms | 430 ms | -93% | [Link] |
| **Throughput / Concurrency** | Max 1.2k rps | 4.5k rps | 4.8k rps | +300% | [Link] |
| **Goroutines / Thread Count** | Leaking to ~45k | Stable at ~1.8k| Stable at ~1.8k| -96% | [Link] |

---

### 3. Reliability & Error Budget

| Metric | Pre-Fix (Incident State) | Post-Fix Target | Observed Post-Fix | Status |
| :--- | :--- | :--- | :--- | :--- |
| **HTTP 5xx / RPC Error Rate** | 8.4% | < 0.05% | 0.01% | Resolved |
| **Failed Feature Interactions** | ~14,200 / day | < 50 / day | 12 / day | Resolved |
| **Timeout / Cancellation Rate** | 12.1% | < 0.1% | 0.04% | Resolved |
| **Error Budget Burn Rate** | 14x | < 1x | 0.2x | Restored |

---

### 4. Business ROI & Financial Attribution

* **Infrastructure / Cloud Savings:**
    * Downscaled from `[X]` instances/nodes to `[Y]`.
    * Direct monthly savings: **$[Amount]/month** ($[Amount] annualized).
* **Direct Revenue / Transaction Recovery:**
    * Baseline funnel conversion loss during incident: **[X]%**.
    * Recovered run-rate: **~$[Amount]** in preserved transactions per week/month.
* **Customer Support & Operational Load:**
    * Escalated ticket volume reduction: **-[X]%** week-over-week.
    * Engineering on-call toil: Zero off-hour pages related to this subsystem since release.

---

### 5. Verification Methodology
* **Telemetry Sources:** [Prometheus, Grafana Dashboards, Datadog Monitors, Perfetto/Trace IDs]
* **Load / Canary Comparison:** [Notes on whether changes were validated via canary, synthetic soak tests, or organic production traffic]
* **Regression Guardrails Added:** [Link to alerting rules, automated performance regression CI tests, or load test suites]