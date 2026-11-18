package com.notarapida.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Dictado por voz con el reconocimiento de Android.
 * Si preferOffline es verdadero, intenta primero dictar sin internet (reconocimiento en el
 * propio teléfono). Si el idioma no está descargado, pasa solo al dictado con internet.
 * Debe usarse siempre desde el hilo principal.
 */
class VoiceDictation(
    private val context: Context,
    private val language: String,
    private val preferOffline: Boolean,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onListeningChange: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        begin(offline = preferOffline)
    }

    private fun begin(offline: Boolean) {
        recognizer?.destroy()
        recognizer = null

        val onDevice = offline &&
            Build.VERSION.SDK_INT >= 33 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(context)) {
            onListeningChange(false)
            onMessage("Este teléfono no tiene reconocimiento de voz disponible.")
            return
        }

        val newRecognizer = if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        newRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChange(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Sin el idioma descargado para dictar sin internet: probamos con internet.
                if (onDevice && isLanguageProblem(error)) {
                    onMessage("Idioma sin descargar para dictar sin internet. Usando internet.")
                    handler.post { begin(offline = false) }
                    return
                }
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
            // En teléfonos sin reconocimiento propio, pide el modo sin conexión si está disponible.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offline)
        }
        newRecognizer.startListening(intent)
    }

    /** Termina de escuchar; el texto final llega igual a onFinalText. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Libera el reconocedor. Se puede llamar varias veces. */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        onListeningChange(false)
    }

    private fun isLanguageProblem(code: Int): Boolean =
        code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE

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
            "Sin conexión: este idioma necesita internet para dictar en este teléfono."

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Falta el permiso del micrófono."

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "El reconocimiento de voz está ocupado. Inténtalo otra vez."

        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "El idioma elegido no está disponible para dictar."

        else -> "No se pudo dictar (código $code)."
    }
}
