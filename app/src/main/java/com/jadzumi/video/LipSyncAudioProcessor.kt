package com.jadzumi.video

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * High-precision circular delay audio processor for real-time lip-sync calibration.
 * Inserts calibrated delay (0ms to 500ms) into the PCM audio stream to perfectly
 * synchronize dialogue with Bluetooth audio headsets and soundbars.
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class LipSyncAudioProcessor : BaseAudioProcessor() {
    private var delayMs: Long = 0L
    private var pendingDelayBytes: Int = 0
    private var delayBuffer: ByteArray = ByteArray(0)
    private var writePos: Int = 0
    private var readPos: Int = 0

    @Synchronized
    fun setDelayMs(ms: Long) {
        val clamped = ms.coerceIn(0L, 500L)
        if (delayMs != clamped) {
            delayMs = clamped
            reconfigureDelayBuffer()
        }
    }

    fun getDelayMs(): Long = delayMs

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        reconfigureDelayBuffer()
        return inputAudioFormat
    }

    private fun reconfigureDelayBuffer() {
        if (inputAudioFormat.sampleRate <= 0 || inputAudioFormat.channelCount <= 0) return
        val bytesPerFrame = inputAudioFormat.bytesPerFrame
        if (delayMs > 0) {
            val targetBytes = ((delayMs * inputAudioFormat.sampleRate / 1000) * bytesPerFrame).toInt()
            if (delayBuffer.size != targetBytes) {
                delayBuffer = ByteArray(targetBytes)
                writePos = 0
                readPos = 0
                pendingDelayBytes = targetBytes
            }
        } else {
            delayBuffer = ByteArray(0)
            writePos = 0
            readPos = 0
            pendingDelayBytes = 0
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val size = inputBuffer.remaining()

        if (delayMs <= 0 || delayBuffer.isEmpty()) {
            val output = replaceOutputBuffer(size)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val output = replaceOutputBuffer(size)
        while (inputBuffer.hasRemaining()) {
            val b = inputBuffer.get()
            if (pendingDelayBytes > 0) {
                output.put(0.toByte())
                pendingDelayBytes--
            } else {
                output.put(delayBuffer[readPos])
                readPos = (readPos + 1) % delayBuffer.size
            }
            delayBuffer[writePos] = b
            writePos = (writePos + 1) % delayBuffer.size
        }
        output.flip()
    }

    override fun onReset() {
        writePos = 0
        readPos = 0
        pendingDelayBytes = 0
        delayBuffer = ByteArray(0)
    }
}
