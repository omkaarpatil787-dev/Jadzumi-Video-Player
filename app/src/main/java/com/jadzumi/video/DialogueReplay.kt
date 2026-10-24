package com.jadzumi.video

/** A replay ends at the original position, independent of playback speed. */
data class DialogueReplayWindow(val startMs: Long, val endMs: Long) {
    fun reachedEnd(positionMs: Long): Boolean = positionMs >= endMs
}

fun dialogueReplayWindow(positionMs: Long, durationMs: Long): DialogueReplayWindow? {
    val end = boundedPosition(positionMs, durationMs)
    if (end <= 0) return null
    return DialogueReplayWindow((end - 10_000L).coerceAtLeast(0L), end)
}
