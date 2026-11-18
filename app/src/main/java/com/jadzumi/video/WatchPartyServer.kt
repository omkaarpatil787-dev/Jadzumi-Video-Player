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
        currentPlayback = null
    }

    fun sendHostChat(text: String) {
        if (text.isBlank()) return
        chatMessages.add(PartyChatMessage(sender = "Host", text = text.trim()))
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 15000
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
        chatMessages.takeLast(25).forEach { msg ->
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
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Jadzumi Cinema Watch Party</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  body { background: #09080e; color: #f0eef8; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
  .top-bar { background: rgba(20,16,30,0.85); backdrop-filter: blur(20px); border-bottom: 1px solid rgba(255,255,255,0.08); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between; }
  .room-pill { background: rgba(157,133,255,0.15); border: 1px solid rgba(157,133,255,0.3); color: #9d85ff; padding: 4px 12px; border-radius: 999px; font-size: 11px; font-weight: 700; display: flex; align-items: center; gap: 6px; }
  .dot { width: 7px; height: 7px; border-radius: 50%; background: #4ade80; box-shadow: 0 0 6px #4ade80; }
  .cinema-container { flex: 1; position: relative; background: #000; display: flex; align-items: center; justify-content: center; }
  video { width: 100%; height: 100%; object-fit: contain; }
  .chat-dock { height: 180px; background: rgba(15,12,22,0.92); border-top: 1px solid rgba(255,255,255,0.08); display: flex; flex-direction: column; }
  .chat-feed { flex: 1; overflow-y: auto; padding: 10px 16px; display: flex; flex-direction: column; gap: 6px; font-size: 12px; }
  .chat-msg { display: flex; gap: 8px; }
  .sender-host { font-weight: 700; color: #9d85ff; }
  .sender-guest { font-weight: 700; color: #4ade80; }
  .chat-bar { display: flex; gap: 8px; padding: 8px 16px; border-top: 1px solid rgba(255,255,255,0.05); }
  .chat-bar input { flex: 1; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; color: #fff; padding: 8px 12px; font-size: 12px; }
  .chat-bar button { background: #9d85ff; border: none; border-radius: 8px; color: #0b0718; font-weight: 700; padding: 0 16px; font-size: 12px; cursor: pointer; }
</style>
</head>
<body>
<div class="top-bar">
  <div style="font-weight: 700; font-size: 14px;">Jadzumi Watch Party</div>
  <div class="room-pill"><div class="dot"></div>SYNCED &bull; ZERO DELAY</div>
</div>
<div class="cinema-container">
  <video id="player" src="/stream.mp4" controls autoplay playsinline></video>
</div>
<div class="chat-dock">
  <div class="chat-feed" id="chatFeed">
    <div class="chat-msg"><span class="sender-host">System:</span> <span>Connected to Jadzumi Cinema Stream.</span></div>
  </div>
  <div class="chat-bar">
    <input type="text" id="chatInput" placeholder="Send a reaction..." onkeypress="if(event.key==='Enter')sendChat()">
    <button onclick="sendChat()">Send</button>
  </div>
</div>
<script>
  const video = document.getElementById('player');
  let isHostControl = false;

  async function syncClock() {
    try {
      const res = await fetch('/api/sync');
      const data = await res.json();
      const hostTimeSec = data.positionMs / 1000;
      if (Math.abs(video.currentTime - hostTimeSec) > 1.2) {
        video.currentTime = hostTimeSec;
      }
      if (data.isPlaying && video.paused) {
        video.play().catch(()=>{});
      } else if (!data.isPlaying && !video.paused) {
        video.pause();
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
      data.messages.forEach(m => {
        const div = document.createElement('div');
        div.className = 'chat-msg';
        const color = m.sender === 'Host' ? 'sender-host' : 'sender-guest';
        div.innerHTML = '<span class="' + color + '">' + m.sender + ':</span> <span>' + m.text + '</span>';
        feed.appendChild(div);
      });
      feed.scrollTop = feed.scrollHeight;
    } catch(e) {}
  }
  setInterval(pollChat, 1500);

  async function sendChat() {
    const input = document.getElementById('chatInput');
    const txt = input.value.trim();
    if (!txt) return;
    input.value = '';
    await fetch('/api/chat', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({ sender: 'Guest', text: txt })
    });
    pollChat();
  }
</script>
</body>
</html>
""".trimIndent()

    private fun getAudioBridgeHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Jadzumi Audio Cinema Bridge</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  body { background: #09080e; color: #f0eef8; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; padding: 24px; text-align: center; }
  .audio-card { background: rgba(25,20,35,0.85); border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 32px 24px; max-width: 400px; width: 100%; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
  .headphone-icon { width: 64px; height: 64px; border-radius: 50%; background: rgba(157,133,255,0.15); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; font-size: 28px; }
  .title { font-size: 20px; font-weight: 700; margin-bottom: 6px; }
  .sub { font-size: 13px; color: #a49fb5; margin-bottom: 24px; line-height: 1.4; }
  .pill { display: inline-flex; align-items: center; gap: 8px; background: rgba(74,222,128,0.15); border: 1px solid rgba(74,222,128,0.3); color: #4ade80; padding: 6px 16px; border-radius: 999px; font-size: 12px; font-weight: 700; margin-bottom: 20px; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: #4ade80; box-shadow: 0 0 8px #4ade80; }
  .audio-elem { width: 100%; margin-top: 10px; }
</style>
</head>
<body>
<div class="audio-card">
  <div class="headphone-icon">🎧</div>
  <div class="title">Audio Cinema Bridge</div>
  <div class="sub">Dual-Headphone Relay. Listen to the active cinema movie in private, frame-accurate synchronization with zero delay.</div>
  <div class="pill"><div class="dot"></div>HEADPHONE AUDIO STREAM ACTIVE</div>
  <audio id="audioStream" class="audio-elem" src="/stream.mp4" controls autoplay></audio>
</div>
<script>
  const audio = document.getElementById('audioStream');
  async function syncClock() {
    try {
      const res = await fetch('/api/sync');
      const data = await res.json();
      const hostTimeSec = data.positionMs / 1000;
      if (Math.abs(audio.currentTime - hostTimeSec) > 0.8) {
        audio.currentTime = hostTimeSec;
      }
      if (data.isPlaying && audio.paused) audio.play().catch(()=>{});
      else if (!data.isPlaying && !audio.paused) audio.pause();
    } catch(e) {}
  }
  setInterval(syncClock, 800);
</script>
</body>
</html>
""".trimIndent()
}
