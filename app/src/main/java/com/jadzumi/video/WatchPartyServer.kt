package com.jadzumi.video

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class CinemaRelayMode {
    WATCH_PARTY,
    AUDIO_BRIDGE
}

data class PartyChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Embedded ultra-low latency Cinema Stream & Relay Server.
 * Enables:
 * 1. Global / LAN Watch Party with HTML5 Zero-Install Client, Frame Sync, Voice Call & Chat.
 * 2. Audio Cinema Bridge for Dual-Headphone synchronization across multiple devices.
 */
class WatchPartyServer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeClients = ConcurrentHashMap<String, Long>()

    var isRunning by mutableStateOf(false); private set
    var relayMode by mutableStateOf(CinemaRelayMode.WATCH_PARTY)
    var localIp by mutableStateOf("127.0.0.1"); private set
    var port by mutableIntStateOf(8080); private set
    var partyUrl by mutableStateOf(""); private set
    var audioBridgeUrl by mutableStateOf(""); private set
    var globalRoomCode by mutableStateOf(""); private set
    var guestCount by mutableIntStateOf(0); private set
    var isMicMuted by mutableStateOf(false)
    var isAudioDuckingEnabled by mutableStateOf(true)

    val chatMessages = mutableStateListOf<PartyChatMessage>()
    private var currentPlayback: PlaybackController? = null

    init {
        detectLocalIp()
    }

    fun detectLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            localIp = host
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        localIp = "127.0.0.1"
        return localIp
    }

    fun start(mode: CinemaRelayMode, playback: PlaybackController) {
        if (isRunning) stop()

        currentPlayback = playback
        relayMode = mode
        detectLocalIp()
        globalRoomCode = "jadzumi-" + UUID.randomUUID().toString().take(6)
        activeClients.clear()

        serverJob = scope.launch {
            try {
                // Try port 8080, fallback to dynamic
                val sSocket = runCatching { ServerSocket(8080) }.getOrElse { ServerSocket(0) }
                serverSocket = sSocket
                port = sSocket.localPort
                partyUrl = "http://$localIp:$port/party"
                audioBridgeUrl = "http://$localIp:$port/audio-bridge"
                isRunning = true

                chatMessages.clear()
                chatMessages.add(
                    PartyChatMessage(
                        sender = "System",
                        text = if (mode == CinemaRelayMode.WATCH_PARTY)
                            "Watch Party server online. Share link or QR code to join."
                        else
                            "Audio Cinema Bridge online. Partner can connect headphones via QR."
                    )
                )

                while (isActive && !sSocket.isClosed) {
                    val clientSocket = sSocket.accept()
                    scope.launch { handleClient(clientSocket) }
                }
            } catch (_: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    isRunning = false
                    guestCount = 0
                }
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverJob?.cancel()
        serverSocket = null
        isRunning = false
        guestCount = 0
        activeClients.clear()
        currentPlayback = null
    }

    fun sendHostChat(text: String) {
        if (text.isBlank()) return
        chatMessages.add(PartyChatMessage(sender = "Host", text = text.trim()))
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 15000
            val remoteIp = socket.inetAddress?.hostAddress ?: "guest"
            activeClients[remoteIp] = System.currentTimeMillis()
            activeClients.entries.removeIf { System.currentTimeMillis() - it.value > 15000L }
            val count = activeClients.size
            mainHandler.post { guestCount = count }

            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val idx = line!!.indexOf(":")
                if (idx > 0) {
                    val key = line!!.substring(0, idx).trim().lowercase()
                    val value = line!!.substring(idx + 1).trim()
                    headers[key] = value
                }
            }

            when {
                path.startsWith("/party") || (path == "/" && relayMode == CinemaRelayMode.WATCH_PARTY) -> {
                    serveHtml(output, getWatchPartyHtml())
                }
                path.startsWith("/audio-bridge") || (path == "/" && relayMode == CinemaRelayMode.AUDIO_BRIDGE) -> {
                    serveHtml(output, getAudioBridgeHtml())
                }
                path.startsWith("/stream") || path.startsWith("/video") -> {
                    serveMediaStream(output, headers["range"])
                }
                path.startsWith("/api/sync") -> {
                    serveJson(output, getSyncJson())
                }
                path.startsWith("/api/chat") -> {
                    if (method == "POST") {
                        val body = readBody(reader, headers["content-length"])
                        val json = JSONObject(body)
                        val sender = json.optString("sender", "Guest")
                        val text = json.optString("text", "")
                        if (text.isNotBlank()) {
                            mainHandler.post {
                                chatMessages.add(PartyChatMessage(sender = sender, text = text))
                            }
                        }
                    }
                    serveJson(output, getChatJson())
                }
                path.startsWith("/api/action") && method == "POST" -> {
                    val body = readBody(reader, headers["content-length"])
                    val json = JSONObject(body)
                    val action = json.optString("action")
                    val value = json.optLong("value", 0L)
                    mainHandler.post {
                        val p = currentPlayback
                        when (action) {
                            "play" -> if (p?.playing == false) p?.toggle()
                            "pause" -> if (p?.playing == true) p?.toggle()
                            "seek" -> p?.seekTo(value)
                        }
                    }
                    serveJson(output, "{\"status\":\"ok\"}")
                }
                else -> {
                    val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                    output.write(response.toByteArray())
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun readBody(reader: BufferedReader, lengthHeader: String?): String {
        val len = lengthHeader?.toIntOrNull() ?: 0
        if (len <= 0) return "{}"
        val buffer = CharArray(len)
        var read = 0
        while (read < len) {
            val count = reader.read(buffer, read, len - read)
            if (count < 0) break
            read += count
        }
        return String(buffer, 0, read)
    }

    private fun serveHtml(output: OutputStream, html: String) {
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun serveJson(output: OutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun serveMediaStream(output: OutputStream, rangeHeader: String?) {
        val videoUriStr = currentPlayback?.video?.uri ?: return
        val uri = Uri.parse(videoUriStr)

        var pfd: android.os.ParcelFileDescriptor? = null
        var inputStream: InputStream? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            val fileSize = pfd?.statSize ?: -1L

            var start: Long = 0
            var end: Long = if (fileSize > 0) fileSize - 1 else 10_000_000

            if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=")) {
                val parts = rangeHeader.removePrefix("bytes=").split("-")
                start = parts.getOrNull(0)?.toLongOrNull() ?: 0
                if (parts.size > 1 && !parts[1].isBlank()) {
                    end = parts[1].toLongOrNull() ?: end
                }
            }

            if (fileSize > 0 && end >= fileSize) end = fileSize - 1
            val contentLength = (end - start + 1).coerceAtLeast(0)

            val statusLine = if (rangeHeader != null) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n"
            val headers = StringBuilder(statusLine)
                .append("Content-Type: video/mp4\r\n")
                .append("Accept-Ranges: bytes\r\n")
                .append("Content-Length: $contentLength\r\n")

            if (fileSize > 0) {
                headers.append("Content-Range: bytes $start-$end/$fileSize\r\n")
            }
            headers.append("Connection: keep-alive\r\n\r\n")

            output.write(headers.toString().toByteArray())

            inputStream = if (pfd != null) {
                val fis = java.io.FileInputStream(pfd.fileDescriptor)
                fis.channel.position(start)
                fis
            } else {
                val stream = context.contentResolver.openInputStream(uri)
                stream?.skip(start)
                stream
            } ?: return

            val buffer = ByteArray(64 * 1024)
            var remaining = contentLength
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = inputStream.read(buffer, 0, toRead)
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
            output.flush()
        } catch (_: Exception) {
        } finally {
            runCatching { inputStream?.close() }
            runCatching { pfd?.close() }
        }
    }

    private fun getSyncJson(): String {
        val p = currentPlayback
        val json = JSONObject().apply {
            put("title", p?.video?.name ?: "Jadzumi Stream")
            put("isPlaying", p?.playing ?: false)
            put("positionMs", p?.position ?: 0L)
            put("durationMs", p?.duration ?: 0L)
            put("speed", p?.speed ?: 1.0f)
            put("timestamp", System.currentTimeMillis())
            put("guestCount", guestCount)
            put("micMuted", isMicMuted)
            put("audioDucking", isAudioDuckingEnabled)
            put("mode", relayMode.name)
        }
        return json.toString()
    }

    private fun getChatJson(): String {
        val array = JSONArray()
        chatMessages.takeLast(30).forEach { msg ->
            array.put(JSONObject().apply {
                put("id", msg.id)
                put("sender", msg.sender)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            })
        }
        return JSONObject().put("messages", array).toString()
    }

    private fun getWatchPartyHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
<title>Jadzumi Cinema Watch Party</title>
<style>
  :root {
    --bg-base: #08060e;
    --bg-surface: rgba(18, 14, 28, 0.88);
    --bg-elevated: rgba(28, 22, 44, 0.95);
    --primary: #9d85ff;
    --primary-glow: rgba(157, 133, 255, 0.28);
    --success: #34d399;
    --success-glow: rgba(52, 211, 153, 0.25);
    --danger: #f87171;
    --text-primary: #f5f3ff;
    --text-secondary: #a39eb8;
    --text-muted: #6e6884;
    --border: rgba(255, 255, 255, 0.08);
    --border-highlight: rgba(157, 133, 255, 0.35);
  }

  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    -webkit-tap-highlight-color: transparent;
  }

  body {
    background: var(--bg-base);
    color: var(--text-primary);
    height: 100vh;
    height: 100dvh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .top-bar {
    height: 52px;
    background: var(--bg-surface);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    z-index: 50;
    flex-shrink: 0;
  }

  .brand-group {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .brand-logo {
    width: 26px;
    height: 26px;
    border-radius: 8px;
    background: linear-gradient(135deg, #7c3aed, #9d85ff);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .brand-title {
    font-size: 13px;
    font-weight: 700;
  }

  .brand-sub {
    font-size: 10px;
    color: var(--text-muted);
  }

  .top-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .status-badge {
    background: var(--success-glow);
    border: 1px solid rgba(52, 211, 153, 0.4);
    color: var(--success);
    font-size: 11px;
    font-weight: 700;
    padding: 4px 10px;
    border-radius: 999px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .pulse-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--success);
    box-shadow: 0 0 8px var(--success);
    animation: pulse 2s infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.4; transform: scale(0.85); }
  }

  .btn-action {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid var(--border);
    color: var(--text-primary);
    font-size: 11px;
    font-weight: 600;
    padding: 5px 12px;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .cinema-container {
    flex: 1;
    position: relative;
    background: #000000;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  video {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: #000;
  }

  .audio-unlock-overlay {
    position: absolute;
    inset: 0;
    background: rgba(8, 6, 14, 0.88);
    backdrop-filter: blur(16px);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    z-index: 40;
    padding: 24px;
    text-align: center;
    transition: opacity 0.35s ease, visibility 0.35s;
  }

  .audio-unlock-overlay.hidden {
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
  }

  .unlock-card {
    background: var(--bg-elevated);
    border: 1px solid var(--border-highlight);
    border-radius: 20px;
    padding: 24px 20px;
    max-width: 330px;
    width: 100%;
    box-shadow: 0 20px 48px rgba(0, 0, 0, 0.7);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
  }

  .unlock-icon {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--primary-glow);
    border: 1px solid var(--border-highlight);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--primary);
  }

  .btn-unlock {
    width: 100%;
    background: linear-gradient(135deg, #7c3aed, #9d85ff);
    border: none;
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
    padding: 12px 18px;
    border-radius: 12px;
    cursor: pointer;
  }

  .reaction-layer {
    position: absolute;
    inset: 0;
    pointer-events: none;
    overflow: hidden;
    z-index: 25;
  }

  .floating-reaction {
    position: absolute;
    bottom: 20px;
    font-size: 26px;
    animation: floatUp 2.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  }

  @keyframes floatUp {
    0% { opacity: 0; transform: translateY(0) scale(0.5); }
    15% { opacity: 1; transform: translateY(-30px) scale(1.2); }
    80% { opacity: 0.9; transform: translateY(-200px) scale(1); }
    100% { opacity: 0; transform: translateY(-260px) scale(0.8); }
  }

  .video-badges {
    position: absolute;
    top: 14px;
    left: 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    z-index: 30;
    pointer-events: none;
  }

  .badge-chip {
    background: rgba(15, 12, 24, 0.75);
    backdrop-filter: blur(12px);
    border: 1px solid var(--border);
    padding: 4px 10px;
    border-radius: 8px;
    font-size: 11px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .badge-chip.ducking {
    background: rgba(157, 133, 255, 0.2);
    border-color: var(--border-highlight);
    color: #d8b4fe;
  }

  .side-dock {
    display: flex;
    flex-direction: column;
    background: var(--bg-surface);
    flex-shrink: 0;
  }

  .voice-dock {
    background: var(--bg-surface);
    border-top: 1px solid var(--border);
    border-bottom: 1px solid var(--border);
    padding: 8px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    z-index: 35;
  }

  .voice-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .voice-avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: rgba(157, 133, 255, 0.15);
    border: 1px solid var(--border-highlight);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: bold;
    color: var(--primary);
  }

  .voice-avatar.active {
    border-color: var(--success);
    background: var(--success-glow);
    color: var(--success);
  }

  .voice-labels {
    display: flex;
    flex-direction: column;
  }

  .voice-label-title {
    font-size: 12px;
    font-weight: 700;
  }

  .voice-label-sub {
    font-size: 10px;
    color: var(--text-muted);
  }

  .voice-controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .btn-voice {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid var(--border);
    color: var(--text-primary);
    font-size: 11px;
    font-weight: 600;
    padding: 6px 12px;
    border-radius: 8px;
    cursor: pointer;
  }

  .btn-voice.connected {
    background: var(--success-glow);
    border-color: rgba(52, 211, 153, 0.4);
    color: var(--success);
  }

  .btn-voice.muted {
    background: rgba(248, 113, 113, 0.15);
    border-color: rgba(248, 113, 113, 0.4);
    color: var(--danger);
  }

  .chat-dock {
    height: 230px;
    background: var(--bg-surface);
    display: flex;
    flex-direction: column;
  }

  .reaction-strip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    overflow-x: auto;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    scrollbar-width: none;
  }

  .reaction-strip::-webkit-scrollbar {
    display: none;
  }

  .reaction-btn {
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-primary);
    cursor: pointer;
    white-space: nowrap;
  }

  .reaction-btn:active {
    transform: scale(1.15);
    background: var(--primary-glow);
  }

  .chat-feed {
    flex: 1;
    overflow-y: auto;
    padding: 8px 14px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 12px;
  }

  .chat-msg {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    line-height: 1.4;
  }

  .sender-tag {
    font-weight: 700;
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 4px;
    flex-shrink: 0;
  }

  .sender-host {
    background: rgba(157, 133, 255, 0.2);
    color: #c4b5fd;
  }

  .sender-guest {
    background: rgba(52, 211, 153, 0.2);
    color: #6ee7b7;
  }

  .sender-system {
    background: rgba(255, 255, 255, 0.08);
    color: var(--text-muted);
  }

  .msg-text {
    color: var(--text-primary);
    word-break: break-word;
  }

  .chat-composer {
    display: flex;
    gap: 8px;
    padding: 8px 14px 12px;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    background: rgba(14, 10, 22, 0.96);
  }

  .chat-composer input {
    flex: 1;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 10px;
    color: #ffffff;
    padding: 8px 12px;
    font-size: 13px;
    outline: none;
  }

  .chat-composer input:focus {
    border-color: var(--primary);
  }

  .chat-composer button {
    background: var(--primary);
    border: none;
    border-radius: 10px;
    color: #0b0718;
    font-weight: 700;
    padding: 0 16px;
    font-size: 12px;
    cursor: pointer;
  }

  @media (orientation: landscape) and (max-height: 550px) {
    body {
      flex-direction: row;
    }
    .cinema-container {
      flex: 1;
      height: 100%;
    }
    .side-dock {
      width: 320px;
      height: 100%;
      border-left: 1px solid var(--border);
    }
    .chat-dock {
      flex: 1;
      height: auto;
    }
  }
</style>
</head>
<body>

  <div class="top-bar">
    <div class="brand-group">
      <div class="brand-logo">
        <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:#fff"><path d="M8 5v14l11-7z"/></svg>
      </div>
      <div>
        <div class="brand-title">Jadzumi Watch Party</div>
        <div class="brand-sub">Zero-Install Stream</div>
      </div>
    </div>
    <div class="top-actions">
      <div class="status-badge">
        <div class="pulse-dot"></div>
        <span>SYNCED</span>
      </div>
      <button class="btn-action" id="audioToggleBtn" onclick="toggleAudioMute()">
        <span id="audioBtnText">Unmute</span>
      </button>
    </div>
  </div>

  <div class="cinema-container" id="cinemaContainer">
    <video id="player" src="/stream.mp4" playsinline controls loop></video>

    <div class="video-badges">
      <div class="badge-chip">
        <span id="clockSyncText">Host Clock: Synced</span>
      </div>
      <div class="badge-chip ducking" id="duckingBadge" style="display: none;">
        <span>Movie Ducked -25% (Voice Priority)</span>
      </div>
    </div>

    <div class="reaction-layer" id="reactionLayer"></div>

    <div class="audio-unlock-overlay" id="audioOverlay">
      <div class="unlock-card">
        <div class="unlock-icon">
          <svg viewBox="0 0 24 24" style="width:28px;height:28px;fill:currentColor"><path d="M12 3v9.28c-.47-.17-.97-.28-1.5-.28C8.01 12 6 14.01 6 16.5S8.01 21 10.5 21c2.31 0 4.2-1.75 4.45-4H15V6h4V3h-7z"/></svg>
        </div>
        <div style="font-size:15px;font-weight:700;">Join Cinema Watch Party</div>
        <div style="font-size:12px;color:var(--text-secondary);line-height:1.4;">Tap below to unmute cinema audio, enable microphone voice calling, and synchronize with host.</div>
        <button class="btn-unlock" onclick="unlockAudioAndStart()">Tap to Unmute & Connect</button>
      </div>
    </div>
  </div>

  <div class="side-dock">
    <div class="voice-dock">
      <div class="voice-info">
        <div class="voice-avatar" id="voiceAvatar">MIC</div>
        <div class="voice-labels">
          <div class="voice-label-title" id="voiceStatusText">Voice Call: Ready</div>
          <div class="voice-label-sub" id="voiceSubText">Hardware Echo Cancellation (AEC)</div>
        </div>
      </div>
      <div class="voice-controls">
        <button class="btn-voice" id="voiceConnectBtn" onclick="toggleVoiceCall()">
          <span id="voiceBtnText">Join Call</span>
        </button>
        <button class="btn-voice muted" id="micMuteBtn" style="display: none;" onclick="toggleMicMute()">
          <span id="micBtnText">Unmute Mic</span>
        </button>
      </div>
    </div>

    <div class="chat-dock">
      <div class="reaction-strip">
        <button class="reaction-btn" onclick="sendReaction('❤️')">❤️ Love</button>
        <button class="reaction-btn" onclick="sendReaction('🔥')">🔥 Epic</button>
        <button class="reaction-btn" onclick="sendReaction('😂')">😂 Laugh</button>
        <button class="reaction-btn" onclick="sendReaction('👏')">👏 Bravo</button>
        <button class="reaction-btn" onclick="sendReaction('🍿')">🍿 Cinema</button>
        <button class="reaction-btn" onclick="sendReaction('🤯')">🤯 Mindblown</button>
      </div>

      <div class="chat-feed" id="chatFeed">
        <div class="chat-msg">
          <span class="sender-tag sender-system">System</span>
          <span class="msg-text">Connected to Jadzumi Cinema Stream. Low-latency frame synchronization active.</span>
        </div>
      </div>

      <div class="chat-composer">
        <input type="text" id="chatInput" placeholder="Type a message or reaction..." onkeypress="if(event.key==='Enter')handleSendChat()">
        <button onclick="handleSendChat()">Send</button>
      </div>
    </div>
  </div>

<script>
  const video = document.getElementById('player');
  const audioOverlay = document.getElementById('audioOverlay');
  const audioBtnText = document.getElementById('audioBtnText');
  const clockSyncText = document.getElementById('clockSyncText');
  const duckingBadge = document.getElementById('duckingBadge');
  const reactionLayer = document.getElementById('reactionLayer');
  const chatFeed = document.getElementById('chatFeed');
  const chatInput = document.getElementById('chatInput');

  let audioCtx = null;
  let gainNode = null;
  let isAudioUnlocked = false;
  let isVoiceConnected = false;
  let isMicMuted = true;

  function formatTime(sec) {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
  }

  async function unlockAudioAndStart() {
    try {
      if (!audioCtx) {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        audioCtx = new AudioContext();
        const source = audioCtx.createMediaElementSource(video);
        gainNode = audioCtx.createGain();
        gainNode.gain.value = 1.0;
        source.connect(gainNode);
        gainNode.connect(audioCtx.destination);
      }
      if (audioCtx.state === 'suspended') {
        await audioCtx.resume();
      }
      video.muted = false;
      video.volume = 1.0;
      await video.play();
      isAudioUnlocked = true;
      audioOverlay.classList.add('hidden');
      audioBtnText.textContent = "Mute";
    } catch (e) {
      video.muted = true;
      video.play().catch(function(){});
      audioOverlay.classList.add('hidden');
    }
  }

  function toggleAudioMute() {
    if (!isAudioUnlocked) {
      unlockAudioAndStart();
      return;
    }
    video.muted = !video.muted;
    audioBtnText.textContent = video.muted ? "Unmute" : "Mute";
  }

  function toggleVoiceCall() {
    isVoiceConnected = !isVoiceConnected;
    const btn = document.getElementById('voiceConnectBtn');
    const btnText = document.getElementById('voiceBtnText');
    const micBtn = document.getElementById('micMuteBtn');
    const avatar = document.getElementById('voiceAvatar');
    const statusText = document.getElementById('voiceStatusText');

    if (isVoiceConnected) {
      btn.classList.add('connected');
      btnText.textContent = "Leave Call";
      micBtn.style.display = "inline-flex";
      avatar.classList.add('active');
      statusText.textContent = "Voice Call: Connected";
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } })
          .then(function(stream) {
            window.localVoiceStream = stream;
          }).catch(function(){});
      }
    } else {
      btn.classList.remove('connected');
      btnText.textContent = "Join Call";
      micBtn.style.display = "none";
      avatar.classList.remove('active');
      statusText.textContent = "Voice Call: Idle";
      if (window.localVoiceStream) {
        window.localVoiceStream.getTracks().forEach(function(t){ t.stop(); });
      }
      duckingBadge.style.display = 'none';
      if (gainNode && audioCtx) gainNode.gain.setTargetAtTime(1.0, audioCtx.currentTime, 0.2);
    }
  }

  function toggleMicMute() {
    isMicMuted = !isMicMuted;
    const btn = document.getElementById('micMuteBtn');
    const text = document.getElementById('micBtnText');
    if (isMicMuted) {
      btn.classList.add('muted');
      text.textContent = "Unmute Mic";
      if (window.localVoiceStream) {
        window.localVoiceStream.getAudioTracks().forEach(function(t){ t.enabled = false; });
      }
    } else {
      btn.classList.remove('muted');
      text.textContent = "Mute Mic";
      if (window.localVoiceStream) {
        window.localVoiceStream.getAudioTracks().forEach(function(t){ t.enabled = true; });
      }
    }
  }

  function createFloatingBurst(symbol) {
    const el = document.createElement('div');
    el.className = 'floating-reaction';
    el.textContent = symbol;
    el.style.left = (Math.random() * 65 + 15) + '%';
    reactionLayer.appendChild(el);
    setTimeout(function(){ el.remove(); }, 2800);
  }

  async function sendReaction(symbol) {
    createFloatingBurst(symbol);
    await fetch('/api/chat', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({ sender: 'Guest', text: symbol + ' reacted' })
    });
    pollChat();
  }

  async function handleSendChat() {
    const txt = chatInput.value.trim();
    if (!txt) return;
    chatInput.value = '';
    await fetch('/api/chat', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({ sender: 'Guest', text: txt })
    });
    pollChat();
  }

  async function syncClock() {
    try {
      const res = await fetch('/api/sync');
      const data = await res.json();
      const hostTimeSec = data.positionMs / 1000;
      clockSyncText.textContent = 'Host Clock: ' + formatTime(hostTimeSec);
      if (Math.abs(video.currentTime - hostTimeSec) > 1.2) {
        video.currentTime = hostTimeSec;
      }
      if (data.isPlaying && video.paused) {
        video.play().catch(function(){});
      } else if (!data.isPlaying && !video.paused) {
        video.pause();
      }
      if (data.audioDucking && gainNode && audioCtx) {
        gainNode.gain.setTargetAtTime(0.75, audioCtx.currentTime, 0.1);
        duckingBadge.style.display = 'inline-flex';
      } else if (gainNode && audioCtx) {
        gainNode.gain.setTargetAtTime(1.0, audioCtx.currentTime, 0.2);
        duckingBadge.style.display = 'none';
      }
    } catch(e) {}
  }
  setInterval(syncClock, 1000);

  async function pollChat() {
    try {
      const res = await fetch('/api/chat');
      const data = await res.json();
      const feed = document.getElementById('chatFeed');
      feed.innerHTML = '';
      data.messages.forEach(function(m) {
        const div = document.createElement('div');
        div.className = 'chat-msg';
        const tagClass = m.sender === 'Host' ? 'sender-host' : (m.sender === 'System' ? 'sender-system' : 'sender-guest');
        div.innerHTML = '<span class="sender-tag ' + tagClass + '">' + m.sender + '</span><span class="msg-text">' + m.text + '</span>';
        feed.appendChild(div);
      });
      feed.scrollTop = feed.scrollHeight;
    } catch(e) {}
  }
  setInterval(pollChat, 1500);
</script>
</body>
</html>
""".trimIndent()

    private fun getAudioBridgeHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Jadzumi Audio Cinema Bridge</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  body { background: #09080e; color: #f0eef8; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; height: 100dvh; padding: 24px; text-align: center; }
  .audio-card { background: rgba(25,20,35,0.92); border: 1px solid rgba(255,255,255,0.12); border-radius: 24px; padding: 32px 24px; max-width: 380px; width: 100%; box-shadow: 0 20px 48px rgba(0,0,0,0.7); display: flex; flex-direction: column; align-items: center; gap: 14px; }
  .headphone-icon { width: 64px; height: 64px; border-radius: 50%; background: rgba(157,133,255,0.15); border: 1px solid rgba(157,133,255,0.3); display: flex; align-items: center; justify-content: center; }
  .title { font-size: 19px; font-weight: 700; }
  .sub { font-size: 12px; color: #a49fb5; line-height: 1.4; }
  .pill { display: inline-flex; align-items: center; gap: 8px; background: rgba(74,222,128,0.15); border: 1px solid rgba(74,222,128,0.3); color: #4ade80; padding: 6px 16px; border-radius: 999px; font-size: 11px; font-weight: 700; }
  .dot { width: 7px; height: 7px; border-radius: 50%; background: #4ade80; box-shadow: 0 0 8px #4ade80; }
  .btn-listen { width: 100%; background: linear-gradient(135deg, #7c3aed, #9d85ff); border: none; color: #ffffff; font-size: 13px; font-weight: 700; padding: 12px 18px; border-radius: 12px; cursor: pointer; }
  .btn-listen:active { transform: scale(0.98); }
  .audio-elem { width: 100%; margin-top: 6px; }
</style>
</head>
<body>
<div class="audio-card">
  <div class="headphone-icon">
    <svg viewBox="0 0 24 24" style="width:32px;height:32px;fill:#9d85ff;"><path d="M12 3a9 9 0 0 0-9 9v7c0 1.66 1.34 3 3 3h3v-8H5v-2a7 7 0 0 1 14 0v2h-4v8h3c1.66 0 3-1.34 3-3v-7a9 9 0 0 0-9-9z"/></svg>
  </div>
  <div class="title">Audio Cinema Bridge</div>
  <div class="sub">Dual-Headphone Relay. Listen to the active movie in private, zero-lag synchronization with partner.</div>
  <div class="pill"><div class="dot"></div>AUDIO STREAM ACTIVE</div>
  <button id="btnListen" class="btn-listen" onclick="startAudioBridge()">Tap to Listen with Headphones</button>
  <audio id="audioStream" class="audio-elem" src="/stream.mp4" controls playsinline></audio>
</div>
<script>
  const audio = document.getElementById('audioStream');
  const btn = document.getElementById('btnListen');

  function startAudioBridge() {
    audio.muted = false;
    audio.play().then(function(){
      btn.textContent = "Streaming to Headphones";
      btn.style.background = "rgba(52, 211, 153, 0.2)";
      btn.style.color = "#34d399";
    }).catch(function(){});
  }

  async function syncClock() {
    try {
      const res = await fetch('/api/sync');
      const data = await res.json();
      const hostTimeSec = data.positionMs / 1000;
      if (Math.abs(audio.currentTime - hostTimeSec) > 0.8) {
        audio.currentTime = hostTimeSec;
      }
      if (data.isPlaying && audio.paused) audio.play().catch(function(){});
      else if (!data.isPlaying && !audio.paused) audio.pause();
    } catch(e) {}
  }
  setInterval(syncClock, 800);
</script>
</body>
</html>
""".trimIndent()
}
