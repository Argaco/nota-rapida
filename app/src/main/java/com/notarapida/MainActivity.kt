package com.notarapida

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notarapida.service.FloatingButtonService
import com.notarapida.ui.NotaRapidaApp
import com.notarapida.ui.NotaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si el botón flotante estaba activado, lo volvemos a poner al abrir la app.
        if (Prefs.floatingEnabled(this) && Settings.canDrawOverlays(this)) {
            FloatingButtonService.start(this)
        }

        setContent {
            NotaTheme {
                NotaRapidaApp()
            }
        }
    }
}
