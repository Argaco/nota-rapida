package com.notarapida

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notarapida.ui.NotaTheme
import com.notarapida.ui.QuickNoteScreen

/**
 * La nota adhesiva. Es una pantalla transparente: lo que estabas leyendo
 * queda visible por detrás, y al cerrarla vuelves exactamente ahí.
 */
class QuickNoteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotaTheme {
                QuickNoteScreen(onClose = { finish() })
            }
        }
    }
}
