package com.netcorte.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class KillSwitchVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopBlocking()
            ACTION_TOGGLE -> if (tun != null) stopBlocking() else startBlocking()
            else -> startBlocking()
        }
        return START_NOT_STICKY
    }

    private fun startBlocking() {
        if (tun != null) return
        tun = try {
            Builder()
                .setSession("NetCorte")
                .addAddress("10.255.255.1", 32)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd00::1", 128)
                .addRoute("::", 0)
                .setBlocking(true)
                .establish()
        } catch (e: Exception) {
            null
        }
        if (tun == null) {
            KillSwitchState.set(false)
            stopSelf()
        } else {
            KillSwitchState.set(true)
        }
    }

    private fun stopBlocking() {
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        KillSwitchState.set(false)
        stopSelf()
    }

    override fun onRevoke() = stopBlocking()

    override fun onDestroy() {
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        KillSwitchState.set(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.netcorte.app.START"
        const val ACTION_STOP = "com.netcorte.app.STOP"
        const val ACTION_TOGGLE = "com.netcorte.app.TOGGLE"
    }
}
