package com.example.data.scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility inside Settings -> Library Configuration that can write real, hardware-encoded MP4 video files
 * (with sidecar chapter files and vertical Shorts) and PCM WAV audio files to a real folder on device storage
 * so users on a fresh emulator with empty storage can test real folder scanning and playback.
 */
object LocalMediaGenerator {

    suspend fun generateRealMediaFolderOnDisk(context: Context): File = withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "LocalTube_Media")
        val techDir = File(baseDir, "Technology").apply { mkdirs() }
        val devDir = File(baseDir, "Development").apply { mkdirs() }
        val shortsDir = File(baseDir, "Shorts").apply { mkdirs() }
        val audioDir = File(baseDir, "Audio").apply { mkdirs() }

        // 1. Landscape HD Feature Video with real sidecar chapters (Forest Teal tone)
        val featureVideo = File(techDir, "Android_15_What_Actually_Changed.mp4")
        if (!featureVideo.exists() || featureVideo.length() == 0L) {
            encodeRealMp4Video(
                outputFile = featureVideo,
                width = 640,
                height = 360,
                durationSec = 16,
                fps = 15,
                primaryColor = Color.rgb(36, 58, 52),
                secondaryColor = Color.rgb(26, 42, 37),
                bannerTitle = "ANDROID 15 ARCHITECTURE"
            )
        }
        val featureChapters = File(techDir, "Android_15_What_Actually_Changed.chapters.txt")
        if (!featureChapters.exists()) {
            featureChapters.writeText(
                """
                00:00 System Overview & Core Changes
                00:04 Edge-to-Edge Enforcement
                00:08 Media3 & Hardware Decoding
                00:12 Storage Access & Privacy
                """.trimIndent()
            )
        }

        // 2. Second Landscape Video (Warm Umber/Bronze tone from photo)
        val natureVideo = File(devDir, "Building_a_Local_First_App_Notes.mp4")
        if (!natureVideo.exists() || natureVideo.length() == 0L) {
            encodeRealMp4Video(
                outputFile = natureVideo,
                width = 640,
                height = 360,
                durationSec = 12,
                fps = 15,
                primaryColor = Color.rgb(61, 50, 35),
                secondaryColor = Color.rgb(43, 35, 24),
                bannerTitle = "BUILDING A LOCAL-FIRST APP"
            )
        }

        // 3. Vertical Short #1 (9:16 aspect ratio for Shorts feed)
        val short1 = File(shortsDir, "Compose_Fluid_Gestures_Short.mp4")
        if (!short1.exists() || short1.length() == 0L) {
            encodeRealMp4Video(
                outputFile = short1,
                width = 360,
                height = 640,
                durationSec = 10,
                fps = 15,
                primaryColor = Color.rgb(36, 58, 52),
                secondaryColor = Color.rgb(18, 30, 27),
                bannerTitle = "FLUID GESTURES"
            )
        }

        // 4. Vertical Short #2 (9:16 aspect ratio for Shorts feed)
        val short2 = File(shortsDir, "Minimal_Dark_UI_Design_Short.mp4")
        if (!short2.exists() || short2.length() == 0L) {
            encodeRealMp4Video(
                outputFile = short2,
                width = 360,
                height = 640,
                durationSec = 9,
                fps = 15,
                primaryColor = Color.rgb(61, 50, 35),
                secondaryColor = Color.rgb(30, 24, 17),
                bannerTitle = "DARK UI DESIGN"
            )
        }

        // 5. Real Stereo WAV Audio File #1 with chapters
        val audio1 = File(audioDir, "Synthwave_Horizon_Original_Mix.wav")
        if (!audio1.exists() || audio1.length() == 0L) {
            writeStereoWavFile(
                outputFile = audio1,
                durationSec = 14,
                baseFreqHz = 220.0,
                harmonyFreqHz = 330.0
            )
        }
        val audio1Chapters = File(audioDir, "Synthwave_Horizon_Original_Mix.chapters.txt")
        if (!audio1Chapters.exists()) {
            audio1Chapters.writeText(
                """
                00:00 Analog Warmup Intro
                00:05 Arpeggio Drive
                00:10 Horizon Outro
                """.trimIndent()
            )
        }

        // 6. Real Stereo WAV Audio File #2
        val audio2 = File(audioDir, "Deep_Focus_Ambient_Session.wav")
        if (!audio2.exists() || audio2.length() == 0L) {
            writeStereoWavFile(
                outputFile = audio2,
                durationSec = 12,
                baseFreqHz = 174.61,
                harmonyFreqHz = 261.63
            )
        }

        baseDir
    }

    private fun encodeRealMp4Video(
        outputFile: File,
        width: Int,
        height: Int,
        durationSec: Int,
        fps: Int,
        primaryColor: Int,
        secondaryColor: Int,
        bannerTitle: String
    ) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 1_200_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mime)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false

        val totalFrames = durationSec * fps
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (width > height) 26f else 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }

        try {
            for (frameIdx in 0 until totalFrames) {
                val canvas: Canvas = inputSurface.lockCanvas(null)
                val progress = frameIdx.toFloat() / totalFrames.toFloat()

                val shader = LinearGradient(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    primaryColor,
                    secondaryColor,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Animated orbital rings
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.argb(90, 255, 255, 255)
                val centerX = width / 2f
                val centerY = height / 2f
                val pulseRadius = (minOf(width, height) * 0.22f) +
                    (sin(progress * 2 * PI * 3).toFloat() * 24f)
                canvas.drawCircle(centerX, centerY, pulseRadius, paint)

                // Moving progress bar at bottom
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(180, 255, 255, 255)
                canvas.drawRect(0f, height - 12f, width * progress, height.toFloat(), paint)

                // Title & live timecode
                canvas.drawText(bannerTitle, centerX, centerY - 10f, textPaint)
                val currentSec = frameIdx / fps
                val remFrames = frameIdx % fps
                val timecode = String.format("00:%02d.%02d • CINEFLOW HD", currentSec, remFrames)
                canvas.drawText(timecode, centerX, centerY + 26f, subPaint)

                inputSurface.unlockCanvasAndPost(canvas)
                drainEncoder(encoder, muxer, bufferInfo, endOfStream = false, trackIndex, muxerStarted) { newTrack, started ->
                    trackIndex = newTrack
                    muxerStarted = started
                }
            }

            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, endOfStream = true, trackIndex, muxerStarted) { newTrack, started ->
                trackIndex = newTrack
                muxerStarted = started
            }
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { inputSurface.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private inline fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        currentTrackIndex: Int,
        currentMuxerStarted: Boolean,
        onStateUpdate: (Int, Boolean) -> Unit
    ) {
        var trackIndex = currentTrackIndex
        var muxerStarted = currentMuxerStarted
        while (true) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
                onStateUpdate(trackIndex, muxerStarted)
            } else if (status >= 0) {
                val encodedData = encoder.getOutputBuffer(status)
                if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        bufferInfo.presentationTimeUs = System.nanoTime() / 1000L
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun writeStereoWavFile(
        outputFile: File,
        durationSec: Int,
        baseFreqHz: Double,
        harmonyFreqHz: Double
    ) {
        val sampleRate = 22050
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSec
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)

        FileOutputStream(outputFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // PCM format
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((numChannels * (bitsPerSample / 8)).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())

            val chunkSamples = 2048
            val pcmBuffer = ByteBuffer.allocate(chunkSamples * 4).order(ByteOrder.LITTLE_ENDIAN)
            var sampleIndex = 0
            while (sampleIndex < totalSamples) {
                pcmBuffer.clear()
                val count = minOf(chunkSamples, totalSamples - sampleIndex)
                for (i in 0 until count) {
                    val t = (sampleIndex + i).toDouble() / sampleRate.toDouble()
                    val envelope = sin(PI * (sampleIndex + i).toDouble() / totalSamples.toDouble())
                    val lfo = 0.7 + 0.3 * sin(2.0 * PI * 0.5 * t)
                    val leftWave = sin(2.0 * PI * baseFreqHz * t) * 0.5 +
                        sin(2.0 * PI * harmonyFreqHz * t) * 0.3
                    val rightWave = sin(2.0 * PI * (baseFreqHz * 1.005) * t) * 0.5 +
                        sin(2.0 * PI * (harmonyFreqHz * 1.5) * t) * 0.3

                    val leftSample = (leftWave * envelope * lfo * 14000).toInt().coerceIn(-32767, 32767).toShort()
                    val rightSample = (rightWave * envelope * lfo * 14000).toInt().coerceIn(-32767, 32767).toShort()
                    pcmBuffer.putShort(leftSample)
                    pcmBuffer.putShort(rightSample)
                }
                fos.write(pcmBuffer.array(), 0, count * 4)
                sampleIndex += count
            }
        }
    }

    fun toFileUriString(dir: File): String = Uri.fromFile(dir).toString()
}
