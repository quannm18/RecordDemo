package com.example.recoremo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

val TAG = "AudioRecordThreadNewVer"
class RecorderAndText {
    private var recorder: AudioRecordThreadNewVer? = null

    fun startRecording(context: Context, outputFile: File, onError: (Exception) -> Unit) {
        checkAudioPermission(context)
        recorder = AudioRecordThreadNewVer(outputFile, onError)
        recorder?.start()
//        startSpeechToTextP(context)
    }
    fun startSpeechToTextP(context :Context){
        startSpeechToText(context)
    }

     fun stopRecording() {
         CoroutineScope(Dispatchers.IO).launch {
             recorder?.stopRecording()
             @Suppress("BlockingMethodInNonBlockingContext")
             recorder?.join()
             recorder = null
         }
    }
}

private class AudioRecordThreadNewVer(
    private val outputFile: File,
    private val onError: (Exception) -> Unit
) :
    Thread("com.example.recoremo.AudioRecorder") {

    private var quit = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 4
            val buffer = ShortArray(bufferSize / 2)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            try {
                audioRecord.startRecording()

                val allData = mutableListOf<Short>()

                while (!quit.get()) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            allData.add(buffer[i])
                        }
                    } else {
                        throw java.lang.RuntimeException("audioRecord.read returned $read")
                    }
                }

                audioRecord.stop()
                encodeWaveFile(outputFile, allData.toShortArray())
            } finally {
                audioRecord.release()
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopRecording() {
        quit.set(true)
    }
}

fun startSpeechToText(context : Context, onText : (String) -> Unit = {}) {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    speechRecognizerIntent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    )
    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(bundle: Bundle?) {
            Log.e(TAG, "onReadyForSpeech: ", )
        }
        override fun onBeginningOfSpeech() {
            Log.e(TAG, "onBeginningOfSpeech: ", )
        }
        override fun onRmsChanged(v: Float) {
            Log.e(TAG, "onRmsChanged: ", )
        }
        override fun onBufferReceived(bytes: ByteArray?) {
            Log.e(TAG, "onBufferReceived: ", )
        }
        override fun onEndOfSpeech() {
            // changing the color of our mic icon to
            // gray to indicate it is not listening
            Log.e(TAG, "onEndOfSpeech: ", )
        }

        override fun onError(i: Int) {
            Log.e(TAG, "onError: $i", )
            startSpeechToText(context, onText)

        }

        override fun onResults(bundle: Bundle) {
            val result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (result != null) {
                Log.e(TAG, "onResults: ${result[0]}", )
                onText(result[0])
            }
            startSpeechToText(context, onText)
        }

        override fun onEndOfSegmentedSession() {
            Log.e(TAG, "onEndOfSegmentedSession: ", )
            super.onEndOfSegmentedSession()
        }

        override fun onSegmentResults(segmentResults: Bundle) {
            Log.e(TAG, "onSegmentResults: ", )
            super.onSegmentResults(segmentResults)
        }

        override fun onLanguageDetection(results: Bundle) {
            Log.e(TAG, "onLanguageDetection: ", )
            super.onLanguageDetection(results)
        }

        override fun onPartialResults(bundle: Bundle) {
            Log.e(TAG, "onPartialResults: ", )
        }
        override fun onEvent(i: Int, bundle: Bundle?) {
            Log.e(TAG, "onEvent: ", )
        }

    })
    speechRecognizer.startListening(speechRecognizerIntent)
}

private fun checkAudioPermission(context: Context) {
    // M = 23
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        context.startActivity(intent)
        Toast.makeText(context, "Allow Microphone Permission", Toast.LENGTH_SHORT).show()
    }
}