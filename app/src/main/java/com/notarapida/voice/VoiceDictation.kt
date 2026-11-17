package com.notarapida.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Dictado por voz con el reconocimiento de Android.
 * Debe usarse siempre desde el hilo principal.
 */
class VoiceDictation(
    private val context: Context,
    private val language: String,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onListeningChange: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onMessage("Este teléfono no tiene reconocimiento de voz disponible.")
            return
        }
        destroy()

        val newRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        newRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChange(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                onListeningChange(false)
                onMessage(messageFor(error))
            }

            override fun onResults(results: Bundle?) {
                onListeningChange(false)
                firstResult(results)?.let(onFinalText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let(onPartialText)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer = newRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        newRecognizer.startListening(intent)
    }

    /** Termina de escuchar; el texto final llega igual a onFinalText. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Libera el reconocedor. Se puede llamar varias veces. */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        onListeningChange(false)
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun messageFor(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "No se escuchó nada. Toca el micrófono e inténtalo de nuevo."

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Sin conexión: en este teléfono el dictado necesita internet."

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Falta el permiso del micrófono."

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "El reconocimiento de voz está ocupado. Inténtalo otra vez."

        else -> "No se pudo dictar (código $code)."
    }
}
