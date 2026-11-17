package com.notarapida

import android.content.Context

/** Ajustes sencillos guardados en el teléfono. */
object Prefs {
    private const val FILE = "nota_rapida"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun onboarded(context: Context): Boolean =
        prefs(context).getBoolean("onboarded", false)

    fun setOnboarded(context: Context) {
        prefs(context).edit().putBoolean("onboarded", true).apply()
    }

    fun floatingEnabled(context: Context): Boolean =
        prefs(context).getBoolean("floating", false)

    fun setFloatingEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("floating", value).apply()
    }

    /** Si está activo, la nota rápida empieza a dictar apenas se abre. */
    fun autoVoice(context: Context): Boolean =
        prefs(context).getBoolean("auto_voice", false)

    fun setAutoVoice(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("auto_voice", value).apply()
    }

    fun language(context: Context): String =
        prefs(context).getString("language", "es-CO") ?: "es-CO"

    fun setLanguage(context: Context, tag: String) {
        prefs(context).edit().putString("language", tag).apply()
    }

    fun bubbleOnRight(context: Context): Boolean =
        prefs(context).getBoolean("bubble_right", true)

    fun bubbleY(context: Context): Int =
        prefs(context).getInt("bubble_y", 400)

    fun setBubblePosition(context: Context, onRight: Boolean, y: Int) {
        prefs(context).edit()
            .putBoolean("bubble_right", onRight)
            .putInt("bubble_y", y)
            .apply()
    }
}
