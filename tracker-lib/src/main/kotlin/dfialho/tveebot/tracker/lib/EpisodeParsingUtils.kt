package dfialho.tveebot.tracker.lib

import dfialho.tveebot.tracker.api.models.VideoQuality
import dfialho.tveebot.tracker.api.models.toVideoQualityOrNull
import java.util.regex.Pattern

/**
 * Pattern for the episode number. The pattern should include the season an number separated by an 'x'. For instance,
 * "12x23", where the season is 12 and number is 23.
 */
private val episodeNumberPattern by lazy { Pattern.compile("\\d+x\\d+") }

/**
 * Set of tokens to ignore in an episode title.
 */
private val ignoredTokens: Collection<String> = setOf(
    "PROPER",
    "TBA",
    "REPACK"
)

internal data class RawEpisode(
    val title: String,
    val season: Int,
    val number: Int,
    val quality: VideoQuality
)

/**
 * Parses the episode full [title] and returns the episode and the video quality.
 *
 * @throws IllegalArgumentException If the format of [title] is invalid
 */
internal fun parseEpisodeTitle(title: String): RawEpisode {
    require(title.isNotBlank()) { "episode full title cannot be blank" }

    val tokensWithQuality = title
        .split(' ')
        .filter { it.isNotBlank() }
        .filter { it !in ignoredTokens }
        .map { it.trim() }

    var quality: VideoQuality? = tokensWithQuality.last().toVideoQualityOrNull()

    val tokens: List<String>
    if (quality == null) {
        quality = VideoQuality.SD
        tokens = tokensWithQuality
    } else {
        tokens = tokensWithQuality.dropLast(1)
    }

    // Find the index where the episode number pattern is
    val episodeNumberTokenIndex = tokens.indexOfFirst { episodeNumberPattern.matcher(it).matches() }

    if (episodeNumberTokenIndex == -1) {
        throw IllegalArgumentException("episode full title must include the token '${episodeNumberPattern.pattern()}'")
    }

    // Parse the episode number token - should be something like "12x23"
    val seasonAndNumber: List<String> = tokens[episodeNumberTokenIndex].split('x')

    return RawEpisode(
        season = seasonAndNumber.first().toInt(),
        number = seasonAndNumber.last().toInt(),
        quality = quality,

        // The title of the episode corresponds to everything after the episode number token
        title = tokens.subList(episodeNumberTokenIndex + 1, tokens.lastIndex + 1).joinToString(" ")
    )
}
