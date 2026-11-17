package com.notarapida.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notarapida.Prefs
import com.notarapida.data.Note

/** Las pantallas de la app principal. */
sealed interface Screen {
    data object Home : Screen
    data object Config : Screen

    /** note == null significa un apunte nuevo. */
    data class Editor(val note: Note?) : Screen
}

@Composable
fun NotaRapidaApp(vm: NotesViewModel = viewModel()) {
    val context = LocalContext.current
    var onboarded by remember { mutableStateOf(Prefs.onboarded(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    if (!onboarded) {
        OnboardingScreen(
            onStart = {
                Prefs.setOnboarded(context)
                onboarded = true
            },
        )
    } else {
        when (val current = screen) {
            Screen.Home -> NotesListScreen(
                notes = notes,
                query = query,
                onQueryChange = vm::setQuery,
                onOpen = { screen = Screen.Editor(it) },
                onNew = { screen = Screen.Editor(null) },
                onSettings = { screen = Screen.Config },
            )

            Screen.Config -> SettingsScreen(onBack = { screen = Screen.Home })

            is Screen.Editor -> {
                val note = current.note
                EditorScreen(
                    note = note,
                    onSave = { text ->
                        if (note == null) vm.add(text) else vm.update(note, text)
                    },
                    onDelete = if (note == null) {
                        null
                    } else {
                        {
                            vm.delete(note)
                            screen = Screen.Home
                        }
                    },
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }
}
