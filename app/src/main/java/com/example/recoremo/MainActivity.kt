package com.example.recoremo

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private var textToSpeechConverter: TextToSpeechConverter? = null

    private val mp3Recorder = RecorderAndText()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTTS()

        findViewById<Button>(R.id.btn_speak_english).setOnClickListener {
//            requestForTextSpeak("Hello!, How are you?","en")

            mp3Recorder.startRecording(this@MainActivity, File(filesDir,"Hello${System.currentTimeMillis()}.mp3")){

            }
//            mp3Recorder.startSpeechToTextP(this@MainActivity)

        }

        findViewById<Button>(R.id.btn_speak_french).setOnClickListener {
//            requestForTextSpeak("Bonjour comment allez-vous?","fr")
            mp3Recorder.stopRecording()
        }
    }

    private fun initTTS() {
        textToSpeechConverter = TextToSpeechConverter(this, object : OnTTSListener {
            override fun onReadyForSpeak() {}
            override fun onError(error: String) {
                showToast(error)
            }
        })
    }

    private fun requestForTextSpeak(text: String, langCode: String) {
        textToSpeechConverter?.speakText(text, langCode)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        textToSpeechConverter?.onStopTTS()
        super.onStop()
    }

    override fun onDestroy() {
        textToSpeechConverter?.onShutdownTTS()
        super.onDestroy()
    }
}