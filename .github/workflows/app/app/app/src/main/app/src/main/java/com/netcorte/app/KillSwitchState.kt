package com.netcorte.app

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

object KillSwitchState {
    @Volatile
    var active: Boolean = false
        private set

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<(Boolean) -> Unit>()

    fun set(value: Boolean) {
        active = value
        main.post { listeners.forEach { it(value) } }
    }

    fun addListener(l: (Boolean) -> Unit) { listeners.add(l) }
    fun removeListener(l: (Boolean) -> Unit) { listeners.remove(l) }
}
