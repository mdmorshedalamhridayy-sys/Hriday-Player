package com.example.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.PI

class BangladeshInstrumentSynthPlayer {
    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isPlaying = false
    @Volatile
    private var currentPositionMs = 0L
    @Volatile
    private var activeSongIndex = 0

    // Pentatonic scale frequencies for different songs
    // A-Minor Pentatonic (Lalon Folk): A3(220), C4(261.6), D4(293.7), E4(329.6), G4(392)
    private val scaleLalon = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.00, 440.00)
    
    // C-Major Pentatonic (Sylheti Folk Pop): C4(261.6), D4(293.7), E4(329.6), G4(392), A4(440)
    private val scaleSylheti = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
    
    // G-Major Scale (Ekla Cholo Re): G4(392), A4(440), B4(493.9), C5(523.3), D5(587.3), E5(659.3)
    private val scaleTagore = doubleArrayOf(392.00, 440.00, 493.88, 523.25, 587.33, 659.25)
    
    // Classic Flute frequencies: F#4(370), A4(440), B4(494), C#5(554), E5(659)
    private val scaleFlute = doubleArrayOf(369.99, 440.00, 493.88, 554.37, 659.25, 739.99)
    
    // Modern Bangla Rock scale (E minor): E3(164.8), G3(196), A3(220), B3(246.9), D4(293.7)
    private val scaleRock = doubleArrayOf(164.81, 196.00, 220.00, 246.94, 293.66, 329.63)

    // Patriotic Acoustic scale (D major): D3(146.8), F#3(185.0), A3(220.0), B3(246.9), D4(293.7)
    private val scalePatriotic = doubleArrayOf(146.83, 185.00, 220.00, 246.94, 293.66, 369.99)

    fun startPlaying(songIndex: Int, initialPositionMs: Long = 0L, onProgressUpdate: (Long) -> Unit) {
        stopPlaying()
        activeSongIndex = songIndex
        currentPositionMs = initialPositionMs
        isPlaying = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playbackJob = scope.launch(Dispatchers.Default) {
            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            
            // Folk speed variables
            var beatCounter = 0
            val samplesPerBeat = sampleRate / 4 // 1/4 second per beat

            while (isActive && isPlaying) {
                // Generate procedural synthesizer audio samples
                for (i in 0 until bufferSize) {
                    val timeSec = currentPositionMs / 1000.0 + (i.toDouble() / sampleRate)
                    val beatIndex = ((currentPositionMs / 1000.0 + (i.toDouble() / sampleRate)) * 4.0).toInt()
                    
                    var sampleVal = 0.0

                    when (activeSongIndex) {
                        0 -> {
                            // --- Mon Amar: Mystic Ektara & Dotara ---
                            // Pluck synth (fast decaying harmonic decay)
                            val dotaraNoteIndex = (beatIndex * 3 / 2) % scaleLalon.size
                            val freq = scaleLalon[dotaraNoteIndex]
                            val beatProgress = (timeSec * 4.0) % 1.0
                            val envelope = (1.0 - beatProgress).coerceIn(0.0, 1.0).pow(3.0)
                            
                            // Plucked string simulation containing primary, 2nd, and 3rd harmonic
                            val synth = sin(2.0 * PI * freq * timeSec) * 0.7 + 
                                        sin(4.0 * PI * freq * timeSec) * 0.25 * envelope +
                                        sin(6.0 * PI * freq * timeSec) * 0.05
                            
                            // Low mystical background vocal hum / tambura
                            val drone = sin(2.0 * PI * 110.0 * timeSec) * 0.15 + 
                                        sin(2.0 * PI * 165.0 * timeSec) * 0.08
                            
                            sampleVal = (synth * envelope * 0.6) + drone
                        }
                        1 -> {
                            // --- Noya Daman: Upbeat Sylheti Folk Pop ---
                            // Fast 6/8 hopping pluck rhythm and high bell pluck
                            val pluckNoteIndex = (beatIndex * 7 / 3) % scaleSylheti.size
                            val freq = scaleSylheti[pluckNoteIndex]
                            val beatProgress = (timeSec * 6.0) % 1.0
                            val envelope = (1.0 - beatProgress).coerceIn(0.0, 1.0).pow(4.0)
                            
                            // Pluck and simple percussion rhythm (snare-like noise)
                            val isPercussionBeat = (beatIndex % 3 == 0) && beatProgress < 0.1
                            val noise = if (isPercussionBeat) (Math.random() - 0.5) * 0.1 else 0.0

                            val melody = sin(2.0 * PI * freq * timeSec) * envelope * 0.5
                            val bass = sin(2.0 * PI * (freq / 2.0) * timeSec) * (envelope + 0.3) * 0.2
                            sampleVal = melody + bass + noise
                        }
                        2 -> {
                            // --- Ekla Cholo Re: Majestic Marching Folk ---
                            // Marching melodic sequence (simple song arpeggiator)
                            val path = intArrayOf(0, 1, 2, 1, 2, 3, 4, 3, 4, 5, 4, 3, 2, 1)
                            val step = beatIndex % path.size
                            val freq = scaleTagore[path[step]]
                            val beatProgress = (timeSec * 3.5) % 1.0
                            val envelope = (1.0 - beatProgress).coerceIn(0.0, 1.0)
                            
                            sampleVal = sin(2.0 * PI * freq * timeSec) * envelope * 0.45 +
                                        sin(2.0 * PI * (scaleTagore[0] / 2.0) * timeSec) * 0.2 // Solid base accompaniment
                        }
                        3 -> {
                            // --- Banshiri: Traditional Flute loop ---
                            // Continuous beautiful flute notes with gentle frequency vibrato and swell envelope
                            val fluteNoteIndex = (beatIndex / 2) % scaleFlute.size
                            val baseFreq = scaleFlute[fluteNoteIndex]
                            
                            // Sinuous slow swell
                            val swell = sin(timeSec * PI) * 0.25 + 0.65
                            // Flute vibrato (FM synthesis of about 6Hz)
                            val vibrato = sin(2.0 * PI * 5.5 * timeSec) * 3.0
                            val targetFreq = baseFreq + vibrato
                            
                            val wave = sin(2.0 * PI * targetFreq * timeSec) * 0.45 +
                                       sin(4.0 * PI * targetFreq * timeSec) * 0.1 // breath overtones
                            
                            sampleVal = wave * swell
                        }
                        4 -> {
                            // --- Kothao Keu Nei: Upbeat Rock-Pop ---
                            // Sharp synth lead and steady bass note arpeggio
                            val rockIndex = (beatIndex * 2) % scaleRock.size
                            val freq = scaleRock[rockIndex]
                            val beatProgress = (timeSec * 5.0) % 1.0
                            val envelope = (1.0 - beatProgress).coerceIn(0.0, 1.0).pow(1.5)
                            
                            val drumSnare = if (beatIndex % 2 == 1 && beatProgress < 0.08) (Math.random() - 0.5) * 0.15 else 0.0
                            val drumKick = if (beatIndex % 2 == 0 && beatProgress < 0.15) sin(2.0 * PI * 70.0 * beatProgress) * 0.4 else 0.0

                            val melody = sin(2.0 * PI * freq * timeSec) * envelope * 0.35
                            sampleVal = melody + drumKick + drumSnare
                        }
                        5 -> {
                            // --- Ami Banglay Gaan Gai: Slow Fingerstyle ---
                            // Slow beautiful acoustic plucks
                            val acousticFreq = scalePatriotic[(beatIndex / 2) % scalePatriotic.size]
                            val beatProgress = (timeSec * 2.0) % 1.0
                            val envelope = (1.0 - beatProgress).coerceIn(0.0, 1.0).pow(2.0)
                            
                            // Comb wave to simulate classical string warmness
                            val stringSynth = sin(2.0 * PI * acousticFreq * timeSec) * 0.45 +
                                               sin(3.0 * PI * acousticFreq * timeSec) * 0.15 +
                                               sin(5.0 * PI * acousticFreq * timeSec) * 0.05
                            
                            sampleVal = stringSynth * envelope * 0.5
                        }
                    }

                    // Clip sample value to safe Short bounds
                    val sampleShort = (sampleVal.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                    buffer[i] = sampleShort
                }

                audioTrack?.write(buffer, 0, bufferSize)

                // Advance position
                val chunkMs = (bufferSize.toDouble() / sampleRate * 1000.0).toLong()
                currentPositionMs += chunkMs
                
                withContext(Dispatchers.Main) {
                    onProgressUpdate(currentPositionMs)
                }
            }
        }
    }

    fun stopPlaying() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("BangladeshInstrumentSynthPlayer", "Error stopping synth track", e)
        }
        audioTrack = null
    }

    fun pause() {
        isPlaying = false
        audioTrack?.pause()
    }

    fun resume(onProgressUpdate: (Long) -> Unit) {
        if (!isPlaying && audioTrack != null) {
            isPlaying = true
            audioTrack?.play()
            startPlaying(activeSongIndex, currentPositionMs, onProgressUpdate)
        }
    }

    fun seekTo(positionMs: Long) {
        currentPositionMs = positionMs
    }

    fun release() {
        stopPlaying()
        scope.cancel()
    }
}
