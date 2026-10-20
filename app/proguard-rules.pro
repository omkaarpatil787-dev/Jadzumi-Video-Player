# ============================================================================
# Jadzumi Player - Production R8 / ProGuard Optimization Rules
# Version: 1.0.0 (Release)
# ============================================================================

# --- Source and Line Information for Stack Traces ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Universal Native Method Bindings ---
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- NextLib Media3 Extensions & FFmpeg Native Audio/Video Decoders ---
-keep class io.github.anilbeesetti.nextlib.** { *; }
-dontwarn io.github.anilbeesetti.nextlib.**
-keepclasseswithmembernames class io.github.anilbeesetti.nextlib.** {
    native <methods>;
}
-keep public class io.github.anilbeesetti.nextlib.media3ext.** {
    public <init>(...);
    public *;
}

# --- AndroidX Media3 (ExoPlayer, Session, UI, Common) ---
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# --- Jetpack Compose Runtime & State ---
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Standard Enum Rules ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Jadzumi Video Domain Models & State ---
-keep class com.jadzumi.video.LocalVideo { *; }
-keep class com.jadzumi.video.LocalFolder { *; }
-keep class com.jadzumi.video.LocalSidecar { *; }
-keep class com.jadzumi.video.TvSeries { *; }
-keep class com.jadzumi.video.TvSeason { *; }
-keep class com.jadzumi.video.TvEpisode { *; }
-keep class com.jadzumi.video.MovieMetadata { *; }
-keep class com.jadzumi.video.PreviewVideo { *; }
-keep class com.jadzumi.video.AudioTrackInfo { *; }
-keep class com.jadzumi.video.SubtitleTrackInfo { *; }
-keep class com.jadzumi.video.SubtitlePreferences { *; }
-keep class com.jadzumi.video.WatchedState { *; }
-keep class com.jadzumi.video.VideoContentBounds { *; }
-keep class com.jadzumi.video.EpisodeHint { *; }
-keep class com.jadzumi.video.EpisodeInfo { *; }
-keep class com.jadzumi.video.NfoMetadata { *; }

# --- Android Entry Points ---
-keep public class com.jadzumi.video.JadzumiApplication { *; }
-keep public class com.jadzumi.video.MainActivity { *; }
