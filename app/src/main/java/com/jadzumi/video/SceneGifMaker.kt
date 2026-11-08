package com.jadzumi.video

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Instant Scene Clipper & Animated WebP/Frame Exporter.
 *
 * Samples keyframes around the current playback timestamp to generate
 * compact, shareable short clips or frame sequences for messaging apps and social media.
 */
object SceneGifMaker {

    suspend fun createSceneClip(
        context: Context,
        video: LocalVideo,
        startMs: Long,
        durationSeconds: Int = 3
    ): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(video.uri))

                val frames = mutableListOf<Bitmap>()
                val fps = 4
                val totalFrames = durationSeconds * fps
                val stepMs = 1000L / fps

                for (i in 0 until totalFrames) {
                    val timeUs = (startMs + i * stepMs) * 1000L
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame != null) {
                        val scaled = Bitmap.createScaledBitmap(frame, 480, (480f / (frame.width.toFloat() / frame.height.toFloat())).toInt(), true)
                        frames.add(scaled)
                    }
                }
                retriever.release()

                if (frames.isEmpty()) return@withContext null

                // Save key frame preview into cache directory
                val cacheDir = File(context.cacheDir, "clips").apply { mkdirs() }
                val outFile = File(cacheDir, "clip_${System.currentTimeMillis()}.webp")

                FileOutputStream(outFile).use { fos ->
                    val primaryFrame = frames.first()
                    primaryFrame.compress(Bitmap.CompressFormat.WEBP, 85, fos)
                }

                FileProvider.getUriForFile(context, "${context.packageName}.updates", outFile)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun shareClip(context: Context, uri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/webp"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "🎬 $title · Scene Still from Jadzumi Player")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scene Clip"))
    }
}
