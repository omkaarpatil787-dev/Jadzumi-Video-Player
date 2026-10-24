package com.jadzumi.video

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AppRelease(val tag: String, val notes: String, val apk: String, val size: Long)

/** Explicit update consent is separate from metadata consent. No scheduler or background service. */
class AppUpdates(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("updates", 0)
    var automatic by mutableStateOf(prefs.getBoolean("automatic", false)); private set
    var release by mutableStateOf<AppRelease?>(null); private set
    var message by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var progress by mutableFloatStateOf(0f); private set
    var downloaded by mutableStateOf(false); private set
    private var job: Job? = null
    private var automaticRequest = false
    private val apk get() = File(getApplication<Application>().cacheDir, "updates/update.apk")

    fun updateAutomatic(enabled: Boolean) {
        automatic = enabled
        prefs.edit().putBoolean("automatic", enabled).apply()
        if (!enabled && automaticRequest) job?.cancel()
    }
    fun check(manual: Boolean) {
        if (busy || (!manual && (!automatic || System.currentTimeMillis() - prefs.getLong("last_check", 0) < 86_400_000L))) return
        job = viewModelScope.launch {
            automaticRequest = !manual
            busy = true; message = "Checking GitHub Releases…"
            prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
            try {
                val found = withContext(Dispatchers.IO) {
                    val json = JSONObject(request("https://api.github.com/repos/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest", 1_048_576).decodeToString())
                    val tag = json.getString("tag_name")
                    if (json.optBoolean("prerelease") || json.optBoolean("draft") || !newerStableRelease(tag, BuildConfig.VERSION_NAME)) null
                    else {
                        val assets = json.getJSONArray("assets")
                        val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }
                            .filter { it.getString("name").endsWith(".apk", true) && !it.getString("name").contains("debug", true) }
                        require(candidates.size == 1) { "Release must contain exactly one production APK." }
                        val asset = candidates.single()
                        val link = asset.getString("browser_download_url")
                        require(link.startsWith("https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/download/"))
                        AppRelease(tag, json.optString("body").take(6000), link, asset.getLong("size"))
                    }
                }
                if (found != null && (manual || found.tag != prefs.getString("skipped", null))) {
                    release = found; downloaded = false
                    message = "Update ${found.tag} is available."
                } else message = "No newer stable update is available."
            } catch (e: CancellationException) { message = "Canceled."; throw e }
            catch (e: Exception) { message = "Update check failed. Check your connection or try again later." }
            finally { busy = false; automaticRequest = false }
        }
    }
    fun dismiss(skip: Boolean = false) {
        if (skip) prefs.edit().putString("skipped", release?.tag).apply()
        release = null
    }
    fun cancel() { job?.cancel() }
    fun download() {
        val target = release ?: return
        if (busy) return
        job = viewModelScope.launch {
            busy = true; downloaded = false; progress = 0f
            message = "Downloading ${target.tag}…"
            try {
                withContext(Dispatchers.IO) {
                    require(target.size in 1..157_286_400L) { "Invalid APK size." }
                    apk.parentFile?.mkdirs()
                    val partial = File(apk.parentFile, "update.part")
                    try {
                        currentCoroutineContext().ensureActive()
                        connection(target.apk).let { c ->
                            try {
                                c.inputStream.use { input -> partial.outputStream().use { output ->
                                    val buffer = ByteArray(65536); var total = 0L
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val n = input.read(buffer); if (n < 0) break
                                        total += n; require(total <= target.size) { "Unexpected APK size." }
                                        output.write(buffer, 0, n)
                                        withContext(Dispatchers.Main) { progress = total.toFloat() / target.size }
                                    }
                                    require(total == target.size) { "Incomplete APK download." }
                                } }
                            } finally { c.disconnect() }
                        }
                        verify(partial, target.tag)
                        apk.delete(); require(partial.renameTo(apk))
                    } finally { partial.delete() }
                }
                downloaded = true; message = "APK verified. Ready to install."
            } catch (e: CancellationException) { message = "Download canceled."; throw e }
            catch (e: Exception) { apk.delete(); message = "Update could not be verified or downloaded. ${e.message.orEmpty().take(180)}" }
            finally { busy = false }
        }
    }
    @Suppress("DEPRECATION")
    private fun verify(file: File, tag: String) {
        val app = getApplication<Application>(); val pm = app.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = requireNotNull(pm.getPackageArchiveInfo(file.path, flags)) { "Unreadable APK." }
        val installed = pm.getPackageInfo(app.packageName, flags)
        fun code(p: android.content.pm.PackageInfo) = if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()
        fun signers(p: android.content.pm.PackageInfo): Set<String> =
            (if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.apkContentsSigners else p.signatures)
                .orEmpty().map { it.toCharsString() }.toSet()
        requireUpdateIdentity(
            UpdateIdentity(candidate.packageName, candidate.versionName, code(candidate), signers(candidate)),
            UpdateIdentity(installed.packageName, installed.versionName, code(installed), signers(installed)), tag
        )
    }
    fun install(context: android.content.Context) {
        if (!downloaded || busy) return
        job = viewModelScope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) { verify(apk, requireNotNull(release).tag) }
                if (!context.packageManager.canRequestPackageInstalls()) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                    message = "Allow installation, return here, then tap Install again."
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            } catch (e: Exception) { message = "Could not open installer. ${e.message.orEmpty().take(160)}" }
            finally { busy = false }
        }
    }
    private suspend fun request(url: String, max: Int): ByteArray {
        currentCoroutineContext().ensureActive()
        val c = connection(url)
        return try { c.inputStream.use { stream ->
            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) { currentCoroutineContext().ensureActive(); val n = stream.read(buffer); if (n < 0) break
                require(out.size() + n <= max); out.write(buffer, 0, n) }
            out.toByteArray()
        } } finally { c.disconnect() }
    }
    private fun connection(address: String): HttpsURLConnection {
        var next = address
        repeat(6) {
            val url = URL(next)
            require(url.protocol == "https" && url.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"))
            val c = url.openConnection() as HttpsURLConnection
            c.instanceFollowRedirects = false; c.connectTimeout = 15_000; c.readTimeout = 20_000
            c.setRequestProperty("User-Agent", OnlineMetadataFetcher.USER_AGENT)
            val status = try { c.responseCode } catch (e: Exception) { c.disconnect(); throw e }
            if (status in 300..399) {
                val location = c.getHeaderField("Location"); c.disconnect()
                next = URL(url, requireNotNull(location)).toString()
            } else {
                if (status != 200) { c.disconnect(); error("Server returned HTTP $status.") }
                return c
            }
        }
        error("Too many redirects.")
    }
}
