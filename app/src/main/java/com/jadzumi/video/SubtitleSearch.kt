package com.jadzumi.video

/** Searchable local text cues; embedded tracks, bitmap subtitles and ASS are not indexed. */
data class SubtitleSearchCue(val startMs: Long, val endMs: Long, val text: String)

object SubtitleSearch {
    const val MAX_BYTES = 2_097_152
    private val timing = Regex("^((?:\\d{1,3}:)?\\d{2}:\\d{2}[,.]\\d{3})\\s+-->\\s+((?:\\d{1,3}:)?\\d{2}:\\d{2}[,.]\\d{3})(?:\\s+.*)?$")
    private fun timestamp(raw: String): Long? {
        val parts = raw.replace(',', '.').split(':', '.')
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        val hours = if (numbers.size == 4) numbers[0] else 0
        val minutes = numbers[numbers.size - 3]
        val seconds = numbers[numbers.size - 2]
        val millis = numbers.last()
        if (minutes !in 0..59 || seconds !in 0..59 || millis !in 0..999) return null
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis
    }
    fun parse(source: String): List<SubtitleSearchCue> {
        require(source.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Subtitle file exceeds 2 MB" }
        val cues = mutableListOf<SubtitleSearchCue>()
        val normalized = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        normalized.split(Regex("\n[\\t ]*\n")).forEach { block ->
            val lines = block.lines().map { it.trim() }
            if (lines.firstOrNull()?.let { it.startsWith("NOTE") || it == "STYLE" || it == "REGION" } == true) return@forEach
            val index = lines.indexOfFirst { timing.matches(it) }
            if (index < 0) return@forEach
            val match = timing.matchEntire(lines[index]) ?: return@forEach
            val start = timestamp(match.groupValues[1]) ?: return@forEach
            val end = timestamp(match.groupValues[2]) ?: return@forEach
            val text = lines.drop(index + 1).joinToString(" ").replace(Regex("<[^>]*>"), "")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&amp;", "&").trim()
            if (end > start && text.isNotBlank()) cues.add(SubtitleSearchCue(start, end, text))
            require(cues.size <= 50_000) { "Too many subtitle cues" }
        }
        return cues.sortedBy { it.startMs }
    }
    fun search(cues: List<SubtitleSearchCue>, query: String): List<SubtitleSearchCue> {
        val needle = query.trim().take(120)
        if (needle.isEmpty()) return emptyList()
        return cues.asSequence().filter { it.text.contains(needle, ignoreCase = true) }.take(100).toList()
    }
}
