package com.jadzumi.video

import java.util.Locale

/** A missing, non-finite or out-of-range rating must never become a placeholder score. */
fun displayRating(value: Double?): String? = value
    ?.takeIf { it.isFinite() && it in 0.0..10.0 }
    ?.let { String.format(Locale.US, "%.1f", it) }

fun measuredVideoBadge(mime: String?, width: Int, height: Int, hdr: Boolean): String? {
    val codec = when (mime) {
        "video/hevc" -> "HEVC"
        "video/avc" -> "AVC"
        "video/av01" -> "AV1"
        "video/x-vnd.on2.vp9" -> "VP9"
        else -> null
    }
    return listOfNotNull(
        if (width > 0 && height > 0) "$width × $height" else null,
        codec, if (hdr) "HDR" else null
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

fun isWatchHistoryKey(key: String): Boolean = Regex("[a-f0-9]{64}(\\.done)?").matches(key)
