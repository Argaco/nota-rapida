package com.notarapida.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.notarapida.Prefs
import com.notarapida.service.FloatingButtonService

private val dictationLanguages = listOf(
    "es-CO" to "Español (Colombia)",
    "es-MX" to "Español (México)",
    "es-ES" to "Español (España)",
    "en-US" to "English (US)",
)

/** Abre la pantalla de Android donde se da el permiso de mostrarse sobre otras apps. */
private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var floating by remember { mutableStateOf(Prefs.floatingEnabled(context)) }
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var autoVoice by remember { mutableStateOf(Prefs.autoVoice(context)) }
    var offlineVoice by remember { mutableStateOf(Prefs.offlineVoice(context)) }
    var language by remember { mutableStateOf(Prefs.language(context)) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // Al volver de la pantalla de permisos, comprobamos si ya lo dieron.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canOverlay = Settings.canDrawOverlays(context)
        if (floating && canOverlay) FloatingButtonService.start(context)
    }

    BackHandler(onBack = onBack)

    fun setFloating(on: Boolean) {
        floating = on
        Prefs.setFloatingEnabled(context, on)
        if (!on) {
            FloatingButtonService.stop(context)
            return
        }
        if (!canOverlay) {
            openOverlaySettings(context)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        FloatingButtonService.start(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Botón flotante")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Mostrar el botón sobre otras apps",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = floating, onCheckedChange = { setFloating(it) })
            }
            Text(
                "Al tocarlo se abre una nota adhesiva encima de lo que estés usando. " +
                    "Puedes arrastrarlo a otro lugar de la pantalla.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (floating && !canOverlay) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Falta un permiso: Android debe dejar que Nota Rápida se muestre sobre otras apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { openOverlaySettings(context) }) {
                    Text("Dar permiso")
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Dictado por voz")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Empezar a dictar al abrir la nota",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = autoVoice,
                    onCheckedChange = {
                        autoVoice = it
                        Prefs.setAutoVoice(context, it)
                    },
                )
            }
            Text(
                "Funciona solo si ya diste el permiso del micrófono desde la nota.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Dictar sin internet cuando sea posible",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = offlineVoice,
                    onCheckedChange = {
                        offlineVoice = it
                        Prefs.setOfflineVoice(context, it)
                    },
                )
            }
            Text(
                "Si el idioma no está descargado en el teléfono, usa internet. Para descargarlo: " +
                    "app Google > Ajustes > Voz > Reconocimiento de voz sin conexión " +
                    "(el nombre puede variar según el teléfono).",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(16.dp))
            Text("Idioma del dictado", style = MaterialTheme.typography.bodyLarge)
            dictationLanguages.forEach { (tag, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = language == tag,
                            role = Role.RadioButton,
                            onClick = {
                                language = tag
                                Prefs.setLanguage(context, tag)
                            },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = language == tag, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
