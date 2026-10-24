package com.jadzumi.video

import java.util.Locale

fun normalizedSeriesName(title: String): String = title.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Only adjacent numbered episodes in the same season are safe to auto-continue. */
fun episodeContinuationWarning(currentName: String, nextName: String): String? {
    val current = EpisodePatterns.parse(currentName)
    val next = EpisodePatterns.parse(nextName)
    if (current == null || next == null) return "Episode numbering could not be verified. Confirm the next file."
    if (normalizedSeriesName(current.seriesTitle) != normalizedSeriesName(next.seriesTitle)) return "The next file belongs to a different series."
    if (next.season != current.season) return "The next file changes season. Confirm before continuing."
    if (next.episode != current.episode + 1) return "Expected episode ${current.episode + 1}, but the next available file is episode ${next.episode}."
    return null
}

fun preferredLanguageTag(raw: String?): String? {
    if (raw.isNullOrBlank() || raw == "auto" || raw == "none") return null
    val locale = Locale.forLanguageTag(raw)
    return raw.takeIf { locale.language.length in 2..3 && locale.language != "und" && raw.length <= 35 }
}

enum class SleepMode { TIMER, END_CURRENT, EPISODES }
data class SleepPlan(val mode: SleepMode, val deadlineMs: Long = 0, val episodes: Int = 1)
fun sleepDeadline(nowMs: Long, minutes: Int): Long {
    require(minutes in 1..180 && nowMs >= 0)
    val duration = minutes * 60_000L
    return if (nowMs > Long.MAX_VALUE - duration) Long.MAX_VALUE else nowMs + duration
}
fun sleepFadeFactor(remainingMs: Long): Float = (remainingMs.coerceIn(0, 10_000).toFloat() / 10_000f)
fun episodesAfterEnd(plan: SleepPlan): Int = when (plan.mode) {
    SleepMode.END_CURRENT -> 0
    SleepMode.EPISODES -> (plan.episodes - 1).coerceAtLeast(0)
    SleepMode.TIMER -> plan.episodes
}
