package com.jadzumi.video

/**
 * Color Studio adjustment parameters for real-time GPU video enhancement.
 *
 * All values default to neutral (bit-exact passthrough).
 *
 * @param contrast 0.6x to 1.6x (neutral = 1.0f)
 * @param shadowLift -25% to +25% (-0.25f to +0.25f, neutral = 0.0f).
 *                   Uses an exponential power curve rather than flat brightness boost,
 *                   brightening dark scene shadows without clipping highlights.
 * @param saturation 0% to 200% (0.0f to 2.0f, neutral = 1.0f). 0% = pure monochrome/grayscale.
 * @param vibrance -25% to +25% (-0.25f to +0.25f, neutral = 0.0f).
 *                 Selectively boosts less saturated tones while protecting already vivid colors and skin tones.
 * @param amoledDeepBlack Experimental near-black clipping filter (default = false).
 *                        Clamps near-black noise directly to true pitch black for OLED displays.
 */
data class ColorStudioParams(
    val contrast: Float = 1.0f,
    val shadowLift: Float = 0.0f,
    val saturation: Float = 1.0f,
    val vibrance: Float = 0.0f,
    val amoledDeepBlack: Boolean = false
) {
    /** Returns true if all parameters are at their neutral default values. */
    fun isNeutral(): Boolean =
        contrast == 1.0f &&
        shadowLift == 0.0f &&
        saturation == 1.0f &&
        vibrance == 0.0f &&
        !amoledDeepBlack

    companion object {
        val NEUTRAL = ColorStudioParams()

        const val AMOLED_WARNING =
            "Notice: AMOLED Deep Black clamps near-black background luminance to true black (#000000). " +
            "This can permanently obscure subtle shadow details in dark film scenes."
    }
}

/**
 * Master color profiles for 1-tap switching.
 */
enum class ColorEnhancementProfile(
    val displayName: String,
    val description: String,
    val params: ColorStudioParams
) {
    NATURAL(
        displayName = "Natural (Studio Master)",
        description = "100% untouched source video stream with direct zero-copy SurfaceView pass-through.",
        params = ColorStudioParams.NEUTRAL
    ),
    CINEMA(
        displayName = "Cinema (Film Look)",
        description = "Subtle cinematic contrast with gentle shadow clarity and natural skin tones.",
        params = ColorStudioParams(
            contrast = 1.06f,
            shadowLift = 0.04f,
            saturation = 1.02f,
            vibrance = 0.00f,
            amoledDeepBlack = false
        )
    ),
    VIVID(
        displayName = "Vivid (Pop & Depth)",
        description = "Richer colors and deeper dynamics with selective vibrance protection.",
        params = ColorStudioParams(
            contrast = 1.04f,
            shadowLift = 0.00f,
            saturation = 1.20f,
            vibrance = 0.10f,
            amoledDeepBlack = false
        )
    ),
    ANIME(
        displayName = "Anime & Animation",
        description = "High vibrancy and edge contrast optimized for cell shading and animated artwork.",
        params = ColorStudioParams(
            contrast = 1.08f,
            shadowLift = 0.00f,
            saturation = 1.15f,
            vibrance = 0.18f,
            amoledDeepBlack = false
        )
    ),
    CUSTOM(
        displayName = "Custom Calibration",
        description = "User-defined sliders with live real-time GPU grading.",
        params = ColorStudioParams.NEUTRAL
    );

    companion object {
        fun fromName(name: String?): ColorEnhancementProfile =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NATURAL
    }
}
