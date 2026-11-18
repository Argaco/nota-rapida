package com.notarapida.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notarapida.data.AppDatabase
import com.notarapida.data.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _query
        .flatMapLatest { dao.observe(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun add(text: String, pinned: Boolean) {
        viewModelScope.launch { dao.insert(Note(text = text, pinned = pinned)) }
    }

    fun update(note: Note, text: String, pinned: Boolean) {
        viewModelScope.launch {
            dao.update(
                note.copy(text = text, pinned = pinned, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }

    /** Arma el texto con TODOS los apuntes (sin importar la búsqueda) y lo entrega en onReady. */
    fun exportAll(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(exportText(dao.getAll())) }
    }
}
