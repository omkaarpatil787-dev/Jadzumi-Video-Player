package com.jadzumi.video

import java.util.Base64

data class SceneBookmark(val id: String, val positionMs: Long, val title: String, val note: String = "")
object SceneBookmarkCodec {
    const val HEADER = "JadzumiBookmarks/1\n"
    const val MAX_BYTES = 65_536
    private fun validate(value: SceneBookmark) {
        require(value.id.matches(Regex("[A-Za-z0-9-]{1,64}")) && value.positionMs >= 0)
        require(value.title.isNotBlank() && value.title.length <= 80 && value.note.length <= 500)
    }
    fun encode(values: List<SceneBookmark>): String {
        require(values.size <= 200 && values.map { it.id }.distinct().size == values.size)
        fun encoded(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
        val result = HEADER + values.sortedBy { it.positionMs }.joinToString("\n") {
            validate(it)
            "${it.id}|${it.positionMs}|${encoded(it.title)}|${encoded(it.note)}"
        }
        require(result.toByteArray().size <= MAX_BYTES)
        return result
    }
    fun decode(text: String): List<SceneBookmark> {
        require(text.startsWith(HEADER) && text.toByteArray().size <= MAX_BYTES)
        fun decoded(text: String) = String(Base64.getUrlDecoder().decode(text), Charsets.UTF_8)
        val values = text.removePrefix(HEADER).lineSequence().filter { it.isNotBlank() }.map {
            val p = it.split('|'); require(p.size == 4)
            SceneBookmark(p[0], p[1].toLong(), decoded(p[2]), decoded(p[3])).also(::validate)
        }.toList()
        require(values.size <= 200 && values.map { it.id }.distinct().size == values.size)
        return values.sortedBy { it.positionMs }
    }
    fun notesExport(videoName: String, values: List<SceneBookmark>): String = buildString {
        append("Jadzumi scene bookmarks\n$videoName\n\n")
        values.sortedBy { it.positionMs }.forEach {
            append("${timecode(it.positionMs / 1000)} — ${it.title}\n")
            if (it.note.isNotBlank()) append("${it.note}\n")
            append('\n')
        }
    }
}
