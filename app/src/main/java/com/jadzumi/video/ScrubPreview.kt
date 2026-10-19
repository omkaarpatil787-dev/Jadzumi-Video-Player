package com.jadzumi.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val previewPermit=Semaphore(1)
@Composable fun ScrubPreview(video:LocalVideo,position:Long) {
    val context=LocalContext.current.applicationContext
    // Keyframe previews are approximate and debounced; never decode on the UI thread.
    val target=(position/2000)*2000
    val frame by produceState<Bitmap?>(null,video.identity,target) {
        value=null
        delay(180)
        value=withContext(Dispatchers.IO) { previewPermit.withPermit {
            if(Build.VERSION.SDK_INT<27) null else {
                val retriever=MediaMetadataRetriever()
                try { retriever.setDataSource(context,video.uri.toUri());retriever.getScaledFrameAtTime(target*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,240,135) }
                catch (_: Exception) { null } finally { runCatching { retriever.release() } }
            }
        } }
    }
    Surface(shape=RoundedCornerShape(12.dp),color=Color.Black.copy(alpha=0.8f),contentColor=Color.White) {
        Column(Modifier.padding(8.dp)) {
            frame?.let { Image(it.asImageBitmap(),"Approximate seek preview",Modifier.size(160.dp,90.dp)) }
            Text(timecode(position/1000),style=MaterialTheme.typography.labelMedium)
        }
    }
}
