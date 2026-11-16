package com.jadzumi.video

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import org.json.JSONArray

object StreamHistoryManager {
    private const val PREF_NAME = "direct_stream_prefs"
    private const val KEY_HISTORY = "recent_stream_urls"

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val current = getHistory(context).toMutableList()
        current.remove(url)
        current.add(0, url)
        val trimmed = current.take(12)
        val arr = JSONArray(trimmed)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_HISTORY, arr.toString())
        }
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_HISTORY)
        }
    }
}

@Composable
fun DirectStreamDialog(
    onDismiss: () -> Unit,
    onPlayStream: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var urlText by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(StreamHistoryManager.getHistory(context)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun play(targetUrl: String) {
        val cleaned = targetUrl.trim()
        if (!cleaned.startsWith("http://", ignoreCase = true) && !cleaned.startsWith("https://", ignoreCase = true)) {
            errorMessage = "Please enter a valid http:// or https:// stream URL."
            return
        }
        StreamHistoryManager.addUrl(context, cleaned)
        val inferredTitle = cleaned.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() } ?: "Network Stream"
        onPlayStream(cleaned, inferredTitle)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1A25),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LavenderAccent.copy(alpha = 0.20f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Language, null, tint = LavenderAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Direct Stream", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            Text("Native Media3 ExoPlayer Engine", fontSize = 11.5.sp, color = MoonlitSecondaryText)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, "Close", tint = MoonlitSecondaryText, modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    "Play compatible direct HTTP/HTTPS media (HLS .m3u8, MPEG-DASH .mpd, Progressive MP4/MKV) directly in Cinema HUD 4.0.",
                    fontSize = 12.sp,
                    color = MoonlitSecondaryText,
                    lineHeight = 16.sp
                )

                // Input + Paste
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://domain.com/live/stream.m3u8", fontSize = 12.5.sp, color = MoonlitSecondaryText.copy(alpha = 0.6f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF292535),
                        unfocusedContainerColor = Color(0xFF292535),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let {
                                    urlText = it.trim()
                                    errorMessage = null
                                }
                            }
                        ) {
                            Text("Paste", color = LavenderAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )

                errorMessage?.let { err ->
                    Text(err, color = Color(0xFFF28B82), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                }

                // Play Button
                Button(
                    onClick = { play(urlText) },
                    enabled = urlText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderAccent,
                        contentColor = Obsidian,
                        disabledContainerColor = Color(0xFF292535),
                        disabledContentColor = MoonlitSecondaryText
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Stream", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                // Recent Streams section
                if (history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Streams", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MoonlitSecondaryText)
                        Text(
                            "Clear",
                            fontSize = 11.sp,
                            color = LavenderAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                StreamHistoryManager.clearHistory(context)
                                history = emptyList()
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history) { histUrl ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF292535),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.fillMaxWidth().clickable { play(histUrl) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.History, null, tint = MoonlitSecondaryText, modifier = Modifier.size(14.dp))
                                    Text(
                                        histUrl,
                                        fontSize = 11.5.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Rounded.PlayArrow, null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
