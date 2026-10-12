package com.safecomm.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class WalkieTalkieManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var recorder: MediaRecorder? = null
    private var currentRecordFile: File? = null
    private var recordStartTime: Long = 0

    private var mediaPlayer: MediaPlayer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSpeaker = MutableStateFlow<String?>(null)
    val currentSpeaker: StateFlow<String?> = _currentSpeaker.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var amplitudeJob: Job? = null

    // --- Recording (Push-to-Talk) ---

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "ptt_${System.currentTimeMillis()}.m4a")
            currentRecordFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recordStartTime = System.currentTimeMillis()
            _isRecording.value = true

            // Poll amplitude for visual wave
            amplitudeJob = scope.launch {
                while (_isRecording.value) {
                    try {
                        val maxAmp = recorder?.maxAmplitude ?: 0
                        _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (_: Exception) {}
                    delay(100)
                }
            }

            Log.d("WalkieTalkie", "Started PTT recording: ${file.name}")
            return true
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Failed to start recording", e)
            _isRecording.value = false
            currentRecordFile?.delete()
            currentRecordFile = null
            return false
        }
    }

    fun stopRecording(onFinished: (file: File, durationSec: Int) -> Unit) {
        if (!_isRecording.value) return

        amplitudeJob?.cancel()
        _amplitude.value = 0f

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Error stopping recorder", e)
        } finally {
            recorder = null
            _isRecording.value = false
        }

        val file = currentRecordFile
        val durationMs = System.currentTimeMillis() - recordStartTime
        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)

        if (file != null && file.exists() && file.length() > 0) {
            Log.d("WalkieTalkie", "Stopped PTT recording: ${file.name}, size=${file.length()}, dur=${durationSec}s")
            onFinished(file, durationSec)
        } else {
            file?.delete()
        }
        currentRecordFile = null
    }

    // --- Instant Walkie-Talkie Playback ---

    fun playLiveVoiceMessage(audioUrl: String, senderNick: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                stopPlayback()

                try {
                    // Set audio routing to speakerphone for loud walkie-talkie output
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = true

                    _currentSpeaker.value = senderNick
                    _isPlaying.value = true

                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .build()
                        )
                        setDataSource(context, Uri.parse(audioUrl))
                        setOnCompletionListener {
                            stopPlayback()
                        }
                        setOnErrorListener { _, _, _ ->
                            stopPlayback()
                            true
                        }
                        prepareAsync()
                        setOnPreparedListener {
                            start()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WalkieTalkie", "Failed to play live voice", e)
                    stopPlayback()
                }
            }
        }
    }

    fun playAudioFile(urlOrPath: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        try {
            _isPlaying.value = true
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(urlOrPath))
                setOnCompletionListener {
                    stopPlayback()
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onComplete()
                    true
                }
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Error playing audio file", e)
            stopPlayback()
            onComplete()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentSpeaker.value = null
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
