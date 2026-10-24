package com.jadzumi.video

/** Stable GitHub releases only; a stable version outranks a preview of the same version. */
internal fun newerStableRelease(tag: String, current: String): Boolean {
    fun numbers(s: String): List<Int>? = s.removePrefix("v").substringBefore('-').split('.').takeIf { it.size == 3 }
        ?.map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    if (!tag.removePrefix("v").matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) return false
    val a = numbers(tag) ?: return false
    val b = numbers(current) ?: return false
    for (i in 0..2) if (a[i] != b[i]) return a[i] > b[i]
    return current.contains('-')
}

internal data class UpdateIdentity(val packageName: String, val versionName: String?, val versionCode: Long, val signers: Set<String>)
internal fun requireUpdateIdentity(candidate: UpdateIdentity, installed: UpdateIdentity, tag: String) {
    require(candidate.packageName == installed.packageName) { "Wrong application package." }
    require(candidate.versionName == tag.removePrefix("v")) { "Release version mismatch." }
    require(candidate.versionCode > installed.versionCode) { "APK is not a newer build." }
    require(installed.signers.isNotEmpty() && installed.signers == candidate.signers) { "Signing key differs. Update rejected." }
}
