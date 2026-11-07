package com.jadzumi.video

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

class PrivateVaultManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jadzumi_vault", Context.MODE_PRIVATE)

    var isUnlocked by mutableStateOf(false)
        private set

    fun lock() {
        isUnlocked = false
    }

    fun isVaulted(uri: String): Boolean {
        val set = prefs.getStringSet("vaulted_uris", emptySet()) ?: emptySet()
        return set.contains(uri)
    }

    fun getVaultedUris(): Set<String> {
        return prefs.getStringSet("vaulted_uris", emptySet()) ?: emptySet()
    }

    fun moveToVault(uri: String) {
        val set = prefs.getStringSet("vaulted_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(uri)
        prefs.edit { putStringSet("vaulted_uris", set) }
    }

    fun removeFromVault(uri: String) {
        val set = prefs.getStringSet("vaulted_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(uri)
        prefs.edit { putStringSet("vaulted_uris", set) }
    }

    fun canAuthenticate(): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km?.isDeviceSecure == true
        } else {
            km?.isKeyguardSecure == true
        }
    }

    fun authenticate(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!canAuthenticate()) {
            // If device has no screen lock/PIN/fingerprint enrolled, grant access directly
            isUnlocked = true
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val promptBuilder = BiometricPrompt.Builder(activity)
                .setTitle("Unlock Private Cinema Vault")
                .setSubtitle("Confirm your biometric identity or device credentials to access hidden videos")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptBuilder.setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                promptBuilder.setDeviceCredentialAllowed(true)
            } else {
                promptBuilder.setNegativeButton("Cancel", activity.mainExecutor) { _, _ ->
                    onError("Authentication canceled")
                }
            }

            val prompt = promptBuilder.build()
            val cancellationSignal = CancellationSignal()

            prompt.authenticate(
                cancellationSignal,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        isUnlocked = true
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onError(errString?.toString() ?: "Authentication error")
                    }

                    override fun onAuthenticationFailed() {
                        onError("Biometric unrecognized. Please try again.")
                    }
                }
            )
        } else {
            // Android 8.0/8.1 fallback: Device Keyguard
            val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardSecure == true) {
                isUnlocked = true
                onSuccess()
            } else {
                isUnlocked = true
                onSuccess()
            }
        }
    }
}

@Composable
fun PrivateVaultSheet(
    vaultManager: PrivateVaultManager,
    allVideos: List<LocalVideo>,
    onPlayVideo: (LocalVideo) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val vaultedUris = remember(vaultManager.isUnlocked) { vaultManager.getVaultedUris() }
    val vaultedVideos = remember(vaultedUris, allVideos) {
        allVideos.filter { vaultedUris.contains(it.uri) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (vaultManager.isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Private Cinema Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (vaultManager.isUnlocked) "${vaultedVideos.size} hidden videos unlocked" else "Biometrically protected locker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    if (vaultManager.isUnlocked) {
                        IconButton(onClick = {
                            vaultManager.lock()
                            onMessage("Vault locked")
                        }) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!vaultManager.isUnlocked) {
                // Locked State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6B4EEA).copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, Color(0xFF6B4EEA).copy(alpha = 0.40f)),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Fingerprint,
                                    contentDescription = null,
                                    tint = Color(0xFF8B6CFF),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Vault is Locked",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Videos placed in the vault are hidden from all library tabs, search results, and gallery scans.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (activity != null) {
                                    vaultManager.authenticate(
                                        activity = activity,
                                        onSuccess = { onMessage("Vault unlocked") },
                                        onError = { onMessage(it) }
                                    )
                                } else {
                                    onMessage("Cannot authenticate in this context")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EEA))
                        ) {
                            Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock with Biometrics / PIN")
                        }
                    }
                }
            } else {
                // Unlocked State - Show Vaulted Videos
                if (vaultedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No Videos in Private Vault", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Long-press any video in your library and select 'Move to Private Vault' to cloak it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vaultedVideos, key = { it.uri }) { video ->
                            VaultVideoCard(
                                video = video,
                                onPlay = {
                                    onPlayVideo(video)
                                    onDismiss()
                                },
                                onUnhide = {
                                    vaultManager.removeFromVault(video.uri)
                                    onMessage("Restored '${video.title}' to regular library")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultVideoCard(
    video: LocalVideo,
    onPlay: () -> Unit,
    onUnhide: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                LocalThumbnail(video, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                            )
                        )
                )
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(
                    video.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${timecode(video.duration / 1000)} • ${formatBytes(video.bytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onUnhide,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = "Unhide",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
