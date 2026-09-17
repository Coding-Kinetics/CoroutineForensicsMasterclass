package com.codingkinetics.channels_and_flows

import kotlinx.coroutines.flow.MutableSharedFlow

object FortressBroadcaster {
    // Defaults: replay = 0, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.SUSPEND
    val alerts = MutableSharedFlow<CombatAlert>()
}
