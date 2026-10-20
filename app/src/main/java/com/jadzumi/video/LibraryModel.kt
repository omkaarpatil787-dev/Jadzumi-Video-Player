package com.jadzumi.video

import java.util.Locale

data class PreviewVideo(val id: String, val title: String, val category: String, val durationSeconds: Long, val positionSeconds: Long, val artwork: String) {
    val progress: Float get() = if (durationSeconds <= 0) 0f else (positionSeconds.toDouble() / durationSeconds).coerceIn(0.0, 1.0).toFloat()
    val remainingSeconds: Long get() = (durationSeconds.coerceAtLeast(0) - positionSeconds.coerceAtLeast(0)).coerceAtLeast(0)
}
fun timecode(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", safe / 3600, safe / 60 % 60, safe % 60)
}
fun filterVideos(videos: List<PreviewVideo>, query: String, category: String) = videos.filter {
    (category == "All videos" || it.category == category) && matchesQuery(query, it.title, it.category)
}
val previewVideos = listOf(
    PreviewVideo("movie", "Interstellar Horizons: The Odyssey", "Movies", 9258, 4152, "hero"),
    PreviewVideo("show", "Chrono Shift", "TV & Anime", 1485, 980, "anime"),
    PreviewVideo("cosmos", "Cosmos: The Invisible Architecture", "Movies", 3250, 1122, "cosmos")
)
