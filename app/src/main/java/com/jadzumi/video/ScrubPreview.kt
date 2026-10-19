package com.jadzumi.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private val previewPermit = Semaphore(1)

/** Panel 3: Rounded thumbnail card with crisp white border and timestamp overlaid at bottom-center */
@Composable fun ScrubPreview(video: LocalVideo, position: Long) {
    val context = LocalContext.current.applicationContext
    val target = (position / 2000) * 2000
    val frame by produceState<Bitmap?>(null, video.identity, target) {
        value = null
        delay(150)
        value = withContext(Dispatchers.IO) {
            previewPermit.withPermit {
                if (Build.VERSION.SDK_INT < 27) null else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, video.uri.toUri())
                        retriever.getScaledFrameAtTime(target * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 256, 144)
                    } catch (_: Exception) { null } finally { runCatching { retriever.release() } }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.size(164.dp, 96.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF101622).copy(alpha = 0.90f),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.80f)),
        shadowElevation = 10.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            frame?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Seek preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))))
            )
            Text(
                text = compactTimecode(position / 1000),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            )
        }
    }
}
// END OF FILE
