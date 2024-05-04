package com.example.recoremo

interface OnTTSListener {
    fun onReadyForSpeak()
    fun onError(error: String)
}