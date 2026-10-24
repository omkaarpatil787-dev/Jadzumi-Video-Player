package com.jadzumi.video

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Reads headers only, off the UI thread. No playback, decoder changes or HTTP requests. */
object LocalMediaProbe {
    private data class Key(val uri: String, val bytes: Long, val modified: Long)
    private val gate = Semaphore(1)
    private val cache = LinkedHashMap<Key, MediaInspection>(32, 0.75f, true)

    suspend fun inspect(context: Context, video: LocalVideo, refresh: Boolean = false): MediaInspection =
        withContext(Dispatchers.IO) {
            gate.withPermit {
                val key = Key(video.uri, video.bytes, video.modified)
                if (!refresh) cache[key]?.let { return@withPermit it }
                val uri = Uri.parse(video.uri)
                if (uri.scheme !in setOf("content", "file")) {
                    return@withPermit MediaInspection(note = "Only local library files can be inspected.")
                }
                val result = extract(context.applicationContext, uri)
                currentCoroutineContext().ensureActive()
                if (result.tracksRead) {
                    cache[key] = result
                    while (cache.size > 32) cache.remove(cache.keys.first())
                }
                result
            }
        }

    private fun MediaFormat.int(key: String): Int? = runCatching { getInteger(key) }.getOrNull()?.takeIf { it > 0 }
    private fun MediaFormat.text(key: String): String? = runCatching { getString(key) }.getOrNull()?.takeIf { it.isNotBlank() }
    private fun MediaFormat.number(key: String): Double? =
        runCatching { getFloat(key).toDouble() }.getOrNull()?.takeIf { it.isFinite() && it > 0 }
            ?: int(key)?.toDouble()

    private fun extract(context: Context, uri: Uri): MediaInspection {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var video: VideoInspection? = null
            val audio = mutableListOf<AudioInspection>()
            val subtitles = mutableListOf<SubtitleInspection>()
            for (index in 0 until extractor.trackCount.coerceAtMost(128)) {
                val f = extractor.getTrackFormat(index)
                val mime = f.text(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") && video == null -> video = VideoInspection(
                        mime, f.int(MediaFormat.KEY_WIDTH), f.int(MediaFormat.KEY_HEIGHT),
                        f.number(MediaFormat.KEY_FRAME_RATE), f.int(MediaFormat.KEY_COLOR_STANDARD),
                        f.int(MediaFormat.KEY_COLOR_TRANSFER), f.int(MediaFormat.KEY_COLOR_RANGE),
                        f.int("bit-depth")?.takeIf { it in 8..16 }
                    )
                    mime.startsWith("audio/") -> audio.add(AudioInspection(mime,
                        f.int(MediaFormat.KEY_CHANNEL_COUNT), f.int(MediaFormat.KEY_SAMPLE_RATE), f.text(MediaFormat.KEY_LANGUAGE)))
                    mime.startsWith("text/") || mime in setOf("application/x-subrip", "application/ttml+xml", "application/x-ass", "application/pgs", "application/dvbsubs", "application/cea-608", "application/cea-708") ->
                        subtitles.add(SubtitleInspection(mime, f.text(MediaFormat.KEY_LANGUAGE)))
                }
            }
            return MediaInspection(video, audio, subtitles, tracksRead = extractor.trackCount <= 128,
                note = if (extractor.trackCount > 128) "Only the first 128 tracks were inspected." else null)
        } catch (_: Exception) {
            // Some containers supported by Media3 are not exposed by Android's extractor.
            return fallback(context, uri)
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun fallback(context: Context, uri: Uri): MediaInspection {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            fun value(key: Int) = retriever.extractMetadata(key)?.toIntOrNull()?.takeIf { it > 0 }
            MediaInspection(video = VideoInspection(width = value(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
                height = value(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)),
                note = "Android exposes limited track information for this file. Playback may report more details.")
        } catch (_: Exception) {
            MediaInspection(note = "Couldn't read this file. Check folder access, then retry or start playback.")
        } finally { runCatching { retriever.release() } }
    }
}
