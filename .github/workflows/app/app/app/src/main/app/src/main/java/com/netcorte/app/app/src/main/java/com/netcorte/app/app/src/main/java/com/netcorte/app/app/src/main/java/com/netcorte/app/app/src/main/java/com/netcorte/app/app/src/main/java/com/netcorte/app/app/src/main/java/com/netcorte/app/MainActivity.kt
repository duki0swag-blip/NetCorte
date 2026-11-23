package com.netcorte.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        const val EXTRA_REQUEST_VPN = "request_vpn"
        private const val REQ_VPN = 1
        private const val REQ_NOTIF = 2
    }

    private lateinit var txtStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnFloating: Button
    private lateinit var txtSize: TextView
    private lateinit var txtAlpha: TextView
    private lateinit var seekSize: SeekBar
    private lateinit var seekAlpha: SeekBar

    private var startAfterVpn = false
    private val stateListener: (Boolean) -> Unit = { refreshUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        btnToggle = findViewById(R.id.btnToggle)
        btnFloating = findViewById(R.id.btnFloating)
        txtSize = findViewById(R.id.txtSize)
        txtAlpha = findViewById(R.id.txtAlpha)
        seekSize = findViewById(R.id.seekSize)
        seekAlpha = findViewById(R.id.seekAlpha)

        seekSize.max = Prefs.SIZE_MAX - Prefs.SIZE_MIN
        seekSize.progress = Prefs.sizeDp(this) - Prefs.SIZE_MIN
        txtSize.text = getString(R.string.size_label, Prefs.sizeDp(this))
        seekSize.setOnSeekBarChangeListener(object : SimpleSeekListener() {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                val v = p + Prefs.SIZE_MIN
                Prefs.setSizeDp(this@MainActivity, v)
                txtSize.text = getString(R.string.size_label, v)
                FloatingButtonService.instance?.applyConfig()
            }
        })

        seekAlpha.max = Prefs.ALPHA_MAX - Prefs.ALPHA_MIN
        seekAlpha.progress = Prefs.alphaPct(this) - Prefs.ALPHA_MIN
        txtAlpha.text = getString(R.string.alpha_label, Prefs.alphaPct(this))
        seekAlpha.setOnSeekBarChangeListener(object : SimpleSeekListener() {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                val v = p + Prefs.ALPHA_MIN
                Prefs.setAlphaPct(this@MainActivity, v)
                txtAlpha.text = getString(R.string.alpha_label, v)
                FloatingButtonService.instance?.applyConfig()
            }
        })

        btnToggle.setOnClickListener {
            if (KillSwitchState.active) KillSwitchController.stop(this) else startBlocking()
        }
        btnFloating.setOnClickListener { toggleFloating() }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { openOverlaySettings() }
        findViewById<Button>(R.id.btnVpn).setOnClickListener {
            startAfterVpn = false
            val i = VpnService.prepare(this)
            if (i != null) startActivityForResult(i, REQ_VPN)
            else Toast.makeText(this, R.string.vpn_ok, Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
        }

        if (Prefs.floatingEnabled(this) && Settings.canDrawOverlays(this) && !FloatingButtonService.running) {
            startFloating()
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_REQUEST_VPN, false) == true) {
            i.removeExtra(EXTRA_REQUEST_VPN)
            startBlocking()
        }
    }

    private fun startBlocking() {
        val i = VpnService.prepare(this)
        if (i != null) {
            startAfterVpn = true
            startActivityForResult(i, REQ_VPN)
        } else {
            KillSwitchController.start(this)
        }
    }

    private fun toggleFloating() {
        if (FloatingButtonService.running) {
            Prefs.setFloatingEnabled(this, false)
            stopService(Intent(this, FloatingButtonService::class.java))
            btnFloating.postDelayed({ refreshUi() }, 300)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.overlay_needed, Toast.LENGTH_LONG).show()
                openOverlaySettings()
                return
            }
            Prefs.setFloatingEnabled(this, true)
            startFloating()
            btnFloating.postDelayed({ refreshUi() }, 300)
        }
    }

    private fun startFloating() {
        val i = Intent(this, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN) {
            if (resultCode == RESULT_OK) {
                if (startAfterVpn) KillSwitchController.start(this)
                else Toast.makeText(this, R.string.vpn_ok, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.vpn_denied, Toast.LENGTH_LONG).show()
            }
            startAfterVpn = false
        }
    }

    private fun refreshUi() {
        val cut = KillSwitchState.active
        txtStatus.setText(if (cut) R.string.status_cut else R.string.status_on)
        txtStatus.setTextColor(if (cut) 0xFFE53935.toInt() else 0xFF2E7D32.toInt())
        btnToggle.setText(if (cut) R.string.btn_restore else R.string.btn_cut)
        btnFloating.setText(if (FloatingButtonService.running) R.string.float_hide else R.string.float_show)
    }

    override fun onStart() {
        super.onStart()
        KillSwitchState.addListener(stateListener)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onStop() {
        KillSwitchState.removeListener(stateListener)
        super.onStop()
    }

    private abstract class SimpleSeekListener : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
