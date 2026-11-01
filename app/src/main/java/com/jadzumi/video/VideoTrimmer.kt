package com.jadzumi.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object LosslessClipExporter {

    @SuppressLint("WrongConstant")
    suspend fun exportClip(
        context: Context,
        video: LocalVideo,
        startMs: Long,
        endMs: Long
    ): File? = withContext(Dispatchers.IO) {
        val durationMs = (endMs - startMs).coerceAtLeast(1000)
        val clipsDir = File(context.cacheDir, "clips").apply { mkdirs() }
        val outputFile = File(clipsDir, "Jadzumi_Clip_${System.currentTimeMillis()}.mp4")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, video.uri.toUri(), null)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val indexMap = mutableMapOf<Int, Int>()
            var maxBufferSize = 1024 * 1024 // 1 MB default buffer

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    indexMap[i] = muxerTrackIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val bufSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (bufSize > maxBufferSize) maxBufferSize = bufSize
                    }
                }
            }

            if (indexMap.isEmpty()) return@withContext null

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                val muxerTrackIndex = indexMap[trackIndex]
                if (muxerTrackIndex != null) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0)
                    val sampleFlags = extractor.sampleFlags
                    var flags = 0
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                        flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                    }
                    bufferInfo.flags = flags
                    bufferInfo.offset = 0

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                if (!extractor.advance()) break
            }

            outputFile
        } catch (_: Exception) {
            outputFile.delete()
            null
        } finally {
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    fun shareClip(context: Context, clipFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            clipFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Jadzumi Video Clip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Cinema Clip"))
    }
}

@Composable
fun VideoTrimmerDialog(
    video: LocalVideo,
    currentPosMs: Long,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoDurMs = video.duration.coerceAtLeast(5000)

    // Clip defaults to 30s window around current playback position
    val initialStart = (currentPosMs - 5000).coerceIn(0, (videoDurMs - 10000).coerceAtLeast(0))
    val initialEnd = (initialStart + 30000).coerceAtMost(videoDurMs)

    var startMs by remember { mutableLongStateOf(initialStart) }
    var endMs by remember { mutableLongStateOf(initialEnd) }
    var isExporting by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.ContentCut,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Instant Scene Clipper",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Lossless 1080p/4K extraction without re-encoding",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Time Capsule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timecode(startMs / 1000), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timecode(endMs / 1000), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Clip Length", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(endMs - startMs) / 1000}s", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4CAF50))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Start Slider
            Text("Adjust Start Position", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = startMs.toFloat(),
                onValueChange = {
                    startMs = it.toLong().coerceAtMost(endMs - 1000)
                },
                valueRange = 0f..videoDurMs.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            // End Slider
            Text("Adjust End Position", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = endMs.toFloat(),
                onValueChange = {
                    endMs = it.toLong().coerceAtLeast(startMs + 1000)
                },
                valueRange = 0f..videoDurMs.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        val file = LosslessClipExporter.exportClip(context, video, startMs, endMs)
                        isExporting = false
                        if (file != null) {
                            onMessage("Clip ready! Opening share sheet...")
                            LosslessClipExporter.shareClip(context, file)
                            onDismiss()
                        } else {
                            onMessage("Failed to export clip. Container format not supported for stream copy.")
                        }
                    }
                },
                enabled = !isExporting && endMs > startMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Exporting Lossless Stream...")
                } else {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export & Share Clip")
                }
            }
        }
    }
}
