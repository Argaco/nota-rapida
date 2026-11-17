package com.notarapida.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.notarapida.Prefs
import com.notarapida.data.NoteStore
import com.notarapida.voice.VoiceDictation
import kotlinx.coroutines.delay

/**
 * La nota adhesiva flotante. Tocar fuera de ella, o volver atrás, guarda lo escrito y la cierra.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuickNoteScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var baseText by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val dictation = remember {
        VoiceDictation(
            context = context,
            language = Prefs.language(context),
            onPartialText = { text = joinText(baseText, it) },
            onFinalText = { text = joinText(baseText, it) },
            onListeningChange = { listening = it },
            onMessage = { message = it },
        )
    }

    DisposableEffect(Unit) {
        onDispose { dictation.destroy() }
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startVoice() {
        message = null
        baseText = text
        dictation.start()
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startVoice() else message = "Sin permiso de micrófono no se puede dictar."
    }

    fun toggleMic() {
        when {
            listening -> dictation.stop()
            hasMicPermission() -> startVoice()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun saveAndClose() {
        dictation.destroy()
        val clean = text.trim()
        if (clean.isNotEmpty()) {
            NoteStore.add(context, clean)
            Toast.makeText(context, "Nota guardada", Toast.LENGTH_SHORT).show()
        }
        onClose()
    }

    fun discard() {
        dictation.destroy()
        onClose()
    }

    BackHandler { saveAndClose() }

    LaunchedEffect(Unit) {
        if (Prefs.autoVoice(context) && hasMicPermission()) {
            startVoice()
        } else {
            delay(200)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .pointerInput(Unit) { detectTapGestures { saveAndClose() } }
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StickyYellow, contentColor = StickyInk),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                // Evita que un toque sobre la nota cuente como "tocar fuera".
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (listening) "Escuchando…" else "Nueva nota",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Escribe o dicta tu apunte") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = StickyInk,
                        unfocusedTextColor = StickyInk,
                        cursorColor = StickyInk,
                    ),
                )

                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { discard() }) {
                        Icon(Icons.Default.Close, contentDescription = "Descartar")
                    }
                    IconButton(onClick = { toggleMic() }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (listening) "Detener dictado" else "Dictar",
                            tint = if (listening) Color(0xFFC62828) else LocalContentColor.current,
                        )
                    }
                    Button(
                        onClick = { saveAndClose() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StickyInk,
                            contentColor = StickyYellow,
                        ),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Guardar", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
