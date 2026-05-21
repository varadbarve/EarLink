package com.example.earlink.actions

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorder(private val context: Context) {
    companion object {
        private const val TAG = "EarLinkVoiceRecorder"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        try {
            outputFile = File(context.cacheDir, "earlink_voice.m4a").apply {
                if (exists()) delete()
            }

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            Log.d(TAG, "MediaRecorder started recording to ${outputFile!!.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder", e)
            stopRecording()
            return null
        }
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }
        return outputFile
    }
}
