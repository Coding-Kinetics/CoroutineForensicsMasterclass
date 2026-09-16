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

package com.codingkinetics.coroutines.helpers

/**
 * OBJECTIVES COVERED:
 * - Tracking workshop progression through structured concurrency encounters
 * - Providing clear telemetry markers for logs and trace baselines
 * - Delineating cooperative hazards, scope leaks, and cancellation failures
 *
 * CAMPAIGN ROLE: Incident State Tracker
 */
enum class IncidentPhase(val description: String) {
    ENCOUNTER_2_1_RACE_CONDITIONS(
        "Encounter 2.1: Astral Goblin Swarm (Shared State & Lost Updates)"
    ),
    ENCOUNTER_2_2_COOPERATIVE_CANCELLATION(
        "Encounter 2.2: Berserker Pacification (Suspending Cancellation Tripwires)"
    ),
    ENCOUNTER_2_3_STRUCTURED_DISENGAGEMENT(
        "Encounter 2.3: Paladin Transmutation (ensureActive & cancelAndJoin)"
    ),
    ENCOUNTER_2_4_DEADLINE_SHEDDING(
        "Encounter 2.4: Sentinel Deadline (withTimeoutOrNull Non-Blocking Shedding)"
    ),
    ENCOUNTER_2_5_BLAST_RADIUS_ISOLATION(
        "Encounter 2.5: Shattered Mirror (SupervisorJob Failure Containment)"
    ),
    RESOLVED(
        "Incident Resolved: Structured Concurrency Restored"
    );

    override fun toString(): String = description
}