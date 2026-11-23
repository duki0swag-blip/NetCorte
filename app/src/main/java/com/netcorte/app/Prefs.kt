package com.netcorte.app

import android.content.Context

/** Ajustes persistentes: tamaño, opacidad, posición del botón y si debe mostrarse. */
object Prefs {
    const val SIZE_MIN = 32
    const val SIZE_MAX = 120
    const val ALPHA_MIN = 30
    const val ALPHA_MAX = 100

    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("netcorte", Context.MODE_PRIVATE)

    fun sizeDp(c: Context) = sp(c).getInt("size", 64)
    fun setSizeDp(c: Context, v: Int) = sp(c).edit().putInt("size", v).apply()

    fun alphaPct(c: Context) = sp(c).getInt("alpha", 90)
    fun setAlphaPct(c: Context, v: Int) = sp(c).edit().putInt("alpha", v).apply()

    fun posX(c: Context) = sp(c).getInt("x", -1)
    fun posY(c: Context) = sp(c).getInt("y", -1)
    fun setPos(c: Context, x: Int, y: Int) = sp(c).edit().putInt("x", x).putInt("y", y).apply()

    fun floatingEnabled(c: Context) = sp(c).getBoolean("floating", false)
    fun setFloatingEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("floating", v).apply()
}
