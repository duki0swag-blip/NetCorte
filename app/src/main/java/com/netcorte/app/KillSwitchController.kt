package com.netcorte.app

import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Punto único para activar / desactivar el corte desde cualquier lugar. */
object KillSwitchController {

    fun toggle(ctx: Context) = if (KillSwitchState.active) stop(ctx) else start(ctx)

    fun start(ctx: Context) {
        if (VpnService.prepare(ctx) != null) {
            // Falta el permiso de VPN: se pide desde la Activity y luego se activa solo.
            ctx.startActivity(
                Intent(ctx, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_REQUEST_VPN, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        ctx.startService(
            Intent(ctx, KillSwitchVpnService::class.java).setAction(KillSwitchVpnService.ACTION_START)
        )
    }

    fun stop(ctx: Context) {
        ctx.startService(
            Intent(ctx, KillSwitchVpnService::class.java).setAction(KillSwitchVpnService.ACTION_STOP)
        )
    }
}
