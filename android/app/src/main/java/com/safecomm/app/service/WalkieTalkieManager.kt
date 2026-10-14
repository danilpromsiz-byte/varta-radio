package com.safecomm.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.media.audiofx.LoudnessEnhancer
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

    data class QueuedVoice(
        val audioUrl: String,
        val senderNick: String,
        val durationSec: Int = 0,
        val messageId: Long = 0L,
        val onFinished: () -> Unit = {}
    )

    private val voiceQueue = java.util.concurrent.ConcurrentLinkedQueue<QueuedVoice>()
    private val _queueCount = MutableStateFlow(0)
    val queueCount: StateFlow<Int> = _queueCount.asStateFlow()
    @Volatile private var isProcessingQueue = false
    private val queueLock = Any()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var amplitudeJob: Job? = null

    // --- Volume Boost / Hardware Gain Amplifier (LoudnessEnhancer) ---
    private val audioPrefs = context.getSharedPreferences("varta_audio_prefs", Context.MODE_PRIVATE)

    var isVolumeBoostEnabled: Boolean
        get() = audioPrefs.getBoolean("volume_boost", true)
        set(value) = audioPrefs.edit().putBoolean("volume_boost", value).apply()

    var volumeBoostGainMb: Int
        get() = audioPrefs.getInt("volume_boost_gain", 1200) // +12 dB
        set(value) = audioPrefs.edit().putInt("volume_boost_gain", value).apply()

    private var loudnessEnhancer: LoudnessEnhancer? = null

    // --- Audio Cues (Start Chirp, Incoming Call Signal & Roger Beep) ---

    fun playPttStartChirp() {
        scope.launch {
            try {
                if (isVolumeBoostEnabled) {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                }
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                delay(130)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Start chirp failed", e)
            }
        }
    }

    fun playIncomingRadioChirp() {
        scope.launch {
            try {
                if (isVolumeBoostEnabled) {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                }
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                // Crisp dual tactical tone (call cue)
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 170)
                delay(200)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Incoming chirp failed", e)
            }
        }
    }

    fun playRogerBeep() {
        scope.launch {
            try {
                if (isVolumeBoostEnabled) {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                }
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                delay(210)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Roger beep failed", e)
            }
        }
    }

    // --- Recording (Push-to-Talk) ---

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        try {
            playPttStartChirp()

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

        playRogerBeep()

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

    // --- Instant Walkie-Talkie Playback with Smart Queue & Xiaomi DAC Protection ---

    private fun setupWalkieTalkieAudioRouting() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true

            if (isVolumeBoostEnabled) {
                try {
                    val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                    val maxVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, 0)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w("WalkieTalkie", "Error setting audio routing", e)
        }
    }

    private suspend fun playIncomingRadioChirpDirect() {
        try {
            setupWalkieTalkieAudioRouting()
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 170)
            delay(200)
            toneGen.release()
            // Xiaomi/MIUI DAC & PA wake-up settling delay:
            // Prevents clipping the first 250ms of incoming speech
            delay(120)
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Incoming chirp failed", e)
        }
    }

    fun playLiveVoiceMessage(
        audioUrl: String,
        senderNick: String,
        durationSec: Int = 0,
        messageId: Long = 0L,
        onFinished: () -> Unit = {}
    ) {
        val item = QueuedVoice(audioUrl, senderNick, durationSec, messageId, onFinished)
        synchronized(queueLock) {
            voiceQueue.offer(item)
            _queueCount.value = voiceQueue.size
            Log.d("WalkieTalkie", "Enqueued voice from $senderNick, total in queue: ${voiceQueue.size}")
            if (!isProcessingQueue) {
                isProcessingQueue = true
                scope.launch {
                    processQueueLoop()
                }
            }
        }
    }

    private suspend fun processQueueLoop() {
        while (true) {
            val item = synchronized(queueLock) {
                val next = voiceQueue.poll()
                _queueCount.value = voiceQueue.size
                if (next == null) {
                    isProcessingQueue = false
                    return@synchronized null
                }
                next
            } ?: break

            try {
                playSingleVoiceItem(item)
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Error playing queued voice item", e)
                item.onFinished()
            }

            // Radio separator gap between sequential messages
            delay(150)
        }

        withContext(Dispatchers.Main) {
            if (voiceQueue.isEmpty() && !isProcessingQueue) {
                stopPlayback(resetHardwareMode = true)
            }
        }
    }

    private suspend fun playSingleVoiceItem(item: QueuedVoice) {
        setupWalkieTalkieAudioRouting()
        _currentSpeaker.value = item.senderNick
        _isPlaying.value = true

        // Play tactical cue and warm up Xiaomi DAC hardware pipeline
        playIncomingRadioChirpDirect()

        val completionDeferred = CompletableDeferred<Unit>()

        withContext(Dispatchers.Main) {
            try {
                cleanupMediaPlayer()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(item.audioUrl))
                    setOnCompletionListener {
                        scope.launch {
                            playRogerBeep()
                            delay(220)
                            try { item.onFinished() } catch (_: Exception) {}
                            completionDeferred.complete(Unit)
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("WalkieTalkie", "MediaPlayer playback error: what=$what, extra=$extra")
                        try { item.onFinished() } catch (_: Exception) {}
                        completionDeferred.complete(Unit)
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener {
                        if (isVolumeBoostEnabled) {
                            try {
                                loudnessEnhancer?.release()
                                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                                    setTargetGain(volumeBoostGainMb)
                                    enabled = true
                                }
                            } catch (e: Exception) {
                                Log.w("WalkieTalkie", "LoudnessEnhancer error", e)
                            }
                        }
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Failed to start media player", e)
                try { item.onFinished() } catch (_: Exception) {}
                completionDeferred.complete(Unit)
            }
        }

        completionDeferred.await()
    }

    fun skipCurrentVoice() {
        scope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                }
            } catch (_: Exception) {}
        }
    }

    fun clearQueue() {
        synchronized(queueLock) {
            voiceQueue.clear()
            _queueCount.value = 0
        }
    }

    fun playAudioFile(urlOrPath: String, onComplete: () -> Unit = {}) {
        stopPlayback(resetHardwareMode = false)
        try {
            setupWalkieTalkieAudioRouting()
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
                    scope.launch {
                        playRogerBeep()
                        delay(220)
                        withContext(Dispatchers.Main) {
                            stopPlayback(resetHardwareMode = true)
                            onComplete()
                        }
                    }
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback(resetHardwareMode = true)
                    onComplete()
                    true
                }
                prepareAsync()
                setOnPreparedListener {
                    if (isVolumeBoostEnabled) {
                        try {
                            loudnessEnhancer?.release()
                            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                                setTargetGain(volumeBoostGainMb)
                                enabled = true
                            }
                        } catch (e: Exception) {
                            Log.w("WalkieTalkie", "LoudnessEnhancer error", e)
                        }
                    }
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Error playing audio file", e)
            stopPlayback(resetHardwareMode = true)
            onComplete()
        }
    }

    private fun cleanupMediaPlayer() {
        try {
            loudnessEnhancer?.apply {
                enabled = false
                release()
            }
        } catch (_: Exception) {}
        loudnessEnhancer = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun stopPlayback(resetHardwareMode: Boolean = true) {
        cleanupMediaPlayer()
        _isPlaying.value = false
        _currentSpeaker.value = null

        if (resetHardwareMode) {
            synchronized(queueLock) {
                voiceQueue.clear()
                _queueCount.value = 0
                isProcessingQueue = false
            }
            try {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
        }
    }
}
