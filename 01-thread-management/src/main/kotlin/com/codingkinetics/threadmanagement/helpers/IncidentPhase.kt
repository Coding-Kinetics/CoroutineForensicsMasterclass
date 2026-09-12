package com.codingkinetics.threadmanagement.helpers

/**
 * OBJECTIVES COVERED:
 * - Orchestrating and verifying thread lifecycle phases
 * - Observing thread states (RUNNABLE vs. WAITING vs. TIMED_WAITING)
 * - Simulating priority inversion and load shedding
 *
 * ROLE: Lab Runner / Test Harness
 *
 * INSTRUCTIONS:
 * 1. Switch the [ACTIVE_PHASE] variable to run each encounter sequentially.
 * 2. Check the console output against the expected forensics in each section.
 */
enum class IncidentPhase {
    ENCOUNTER_1_RACE_CONDITION,        // Lab 1A: Hands-on (Observe silent corruption)
    ENCOUNTER_2_INTERRUPT_FAILURE,     // Demo 1B: Instructor Demo (Tight CPU loop ignores interrupt)
    ENCOUNTER_3_COOPERATIVE_PALADIN,   // Lab 1C: Hands-on (Thread checks isInterrupted)
    ENCOUNTER_4_LOCK_STARVATION,       // Demo 1D: Instructor Demo (Priority inversion & WAITING state)
    ENCOUNTER_5_TIMED_LOCK          // Lab 1E: Hands-on (Bounded SLA load shedding)
}