package com.notarapida.ui

import android.content.Context
import android.content.Intent
import com.notarapida.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es")).format(Date(millis))

/** Une el texto que ya había con lo que se acaba de dictar. */
fun joinText(base: String, added: String): String = when {
    base.isBlank() -> added
    added.isBlank() -> base
    else -> base.trimEnd() + " " + added
}

/** Texto plano con todos los apuntes, listo para compartir o pegar en otro lado. */
fun exportText(notes: List<Note>): String {
    if (notes.isEmpty()) return ""
    val today = formatDate(System.currentTimeMillis())
    val body = notes.joinToString(separator = "\n\n----------\n\n") { note ->
        val mark = if (note.pinned) " (fijado)" else ""
        "${formatDate(note.createdAt)}$mark\n${note.text}"
    }
    return "Mis apuntes - $today\n\n$body\n"
}

/** Abre el menú de compartir de Android con el texto dado. */
fun shareText(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, subject))
}
