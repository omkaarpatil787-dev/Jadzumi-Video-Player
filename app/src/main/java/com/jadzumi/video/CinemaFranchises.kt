package com.jadzumi.video

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class CinemaFranchise(
    val id: String,
    val name: String,
    val description: String,
    val tagKeywords: List<String>,
    val films: List<LocalVideo> = emptyList(),
    val posterUri: String? = null
) {
    val totalDurationMs: Long get() = films.sumOf { it.duration }
    val nextUnwatchedFilm: LocalVideo? get() = films.firstOrNull()
}

/**
 * Intelligent Movie Franchises & Box Set Curating Engine.
 *
 * Automatically detects and groups movies into sequential sagas and box sets,
 * computing total saga runtime, completion progress, and chronological film order.
 */
object CinemaFranchiseManager {

    private val YEAR_REGEX = Regex("""\b(19\d\d|20\d\d)\b""")

    private fun extractYear(name: String): Int {
        return YEAR_REGEX.find(name)?.value?.toIntOrNull() ?: 0
    }

    private val FRANCHISE_DEFINITIONS = listOf(
        Triple("harry_potter", "Harry Potter Saga", listOf("harry potter", "philosopher's stone", "sorcerer's stone", "chamber of secrets", "prisoner of azkaban", "goblet of fire", "order of the phoenix", "half-blood prince", "deathly hallows")),
        Triple("resident_evil", "Resident Evil Universe", listOf("resident evil", "apocalypse", "extinction", "afterlife", "retribution", "final chapter", "welcome to raccoon city")),
        Triple("marvel_cinematic", "Marvel Cinematic Universe", listOf("iron man", "captain america", "avengers", "thor", "guardians of the galaxy", "black panther", "doctor strange", "infinity war", "endgame", "spider-man", "ant-man")),
        Triple("star_wars", "Star Wars Skywalker Saga", listOf("star wars", "phantom menace", "attack of the clones", "revenge of the sith", "a new hope", "empire strikes back", "return of the jedi", "force awakens", "last jedi", "rise of skywalker", "rogue one")),
        Triple("lotr", "The Lord of the Rings & Hobbit", listOf("lord of the rings", "fellowship of the ring", "two towers", "return of the king", "the hobbit", "unexpected journey", "desolation of smaug", "battle of the five armies")),
        Triple("fast_furious", "Fast & Furious Saga", listOf("fast & furious", "fast and furious", "2 fast 2 furious", "tokyo drift", "fast five", "furious 7", "fate of the furious", "f9", "fast x")),
        Triple("dark_knight", "The Dark Knight Trilogy", listOf("batman begins", "the dark knight", "dark knight rises")),
        Triple("john_wick", "John Wick Adjudication", listOf("john wick", "parabellum", "chapter 4")),
        Triple("mission_impossible", "Mission: Impossible Series", listOf("mission: impossible", "ghost protocol", "rogue nation", "fallout", "dead reckoning")),
        Triple("pirates", "Pirates of the Caribbean", listOf("pirates of the caribbean", "black pearl", "dead man's chest", "world's end", "on stranger tides", "dead men tell no tales")),
        Triple("matrix", "The Matrix Franchise", listOf("the matrix", "matrix reloaded", "matrix revolutions", "matrix resurrections")),
        Triple("alien", "Alien & Predator Universe", listOf("alien", "aliens", "alien 3", "alien resurrection", "prometheus", "alien: covenant", "predator", "prey")),
        Triple("jurassic", "Jurassic Park & World", listOf("jurassic park", "the lost world", "jurassic park iii", "jurassic world", "fallen kingdom", "dominion"))
    )

    fun detectFranchises(videos: List<LocalVideo>): List<CinemaFranchise> {
        val moviesOnly = videos.filter { it.episode == null }
        val franchises = mutableListOf<CinemaFranchise>()

        for ((id, name, keywords) in FRANCHISE_DEFINITIONS) {
            val matchingFilms = moviesOnly.filter { video ->
                val titleLower = (video.title + " " + video.name).lowercase(Locale.US)
                keywords.any { keyword -> titleLower.contains(keyword) }
            }.sortedBy { extractYear(it.name) }

            if (matchingFilms.size >= 2) {
                franchises.add(
                    CinemaFranchise(
                        id = id,
                        name = name,
                        description = "${matchingFilms.size} feature films in chronological saga order",
                        tagKeywords = keywords,
                        films = matchingFilms,
                        posterUri = matchingFilms.firstOrNull()?.uri
                    )
                )
            }
        }

        return franchises
    }
}
