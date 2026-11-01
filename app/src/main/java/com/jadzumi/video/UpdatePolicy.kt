package com.jadzumi.video

/** Stable GitHub releases; supports 3-component (x.y.z) and 4-component (x.y.z.w) semantic versions. */
internal fun newerStableRelease(tag: String, current: String): Boolean {
    fun numbers(s: String): List<Int>? = s.removePrefix("v").substringBefore('-').split('.')
        .takeIf { it.size in 3..4 }
        ?.map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    if (!tag.removePrefix("v").matches(Regex("[0-9]+(\\.[0-9]+){2,3}"))) return false
    val a = numbers(tag) ?: return false
    val b = numbers(current) ?: return false
    val maxLen = maxOf(a.size, b.size)
    val aPadded = a + List(maxLen - a.size) { 0 }
    val bPadded = b + List(maxLen - b.size) { 0 }
    for (i in 0 until maxLen) {
        if (aPadded[i] != bPadded[i]) return aPadded[i] > bPadded[i]
    }
    return current.contains('-')
}

internal data class UpdateIdentity(val packageName: String, val versionName: String?, val versionCode: Long, val signers: Set<String>)
internal fun requireUpdateIdentity(candidate: UpdateIdentity, installed: UpdateIdentity, tag: String) {
    require(candidate.packageName == installed.packageName) { "Wrong application package." }
    require(candidate.versionName == tag.removePrefix("v")) { "Release version mismatch." }
    require(candidate.versionCode > installed.versionCode) { "APK is not a newer build." }
    require(installed.signers.isNotEmpty() && installed.signers == candidate.signers) { "Signing key differs. Update rejected." }
}
