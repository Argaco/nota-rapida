package com.notarapida.ui

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
