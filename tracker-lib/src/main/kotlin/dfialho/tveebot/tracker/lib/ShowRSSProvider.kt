package dfialho.tveebot.tracker.lib

import dfialho.tveebot.tracker.api.TVShowIDMapper
import dfialho.tveebot.tracker.api.TVShowProvider
import dfialho.tveebot.tracker.api.models.Episode
import dfialho.tveebot.tracker.api.models.EpisodeFile
import dfialho.tveebot.tracker.api.models.ID
import dfialho.tveebot.tracker.api.models.TVShow
import dfialho.tveebot.utils.rssfeed.RSSFeedException
import dfialho.tveebot.utils.rssfeed.RSSFeedItem
import dfialho.tveebot.utils.rssfeed.RSSFeedReader
import mu.KLogging
import org.jsoup.Jsoup
import java.net.URL

/**
 * [TVShowProvider] backed by the "showrss.info" website.
 *
 * @author David Fialho (dfialho@protonmail.com)
 */
class ShowRSSProvider(private val idMapper: TVShowIDMapper) : TVShowProvider {

    companion object : KLogging() {
        private const val SHOWRSS_URL = "https://showrss.info/browse"
    }

    /**
     * Reader used to read the feed obtained from showRSS and find the episodes available.
     */
    private val feedReader = RSSFeedReader()

    override fun fetchTVShows(): List<TVShow> = Jsoup.connect(SHOWRSS_URL).get()
        .select("option")
        .map {
            TVShow(
                id = idMapper.getTVShowID(providerID = it.attr("value")),
                title = it.text()
            )
        }

    override fun fetchEpisodes(tvShow: TVShow): List<EpisodeFile> {
        val showID: String = idMapper[tvShow.id] ?: throw IllegalArgumentException("Not found: $tvShow")
        val showURL = URL("https://showrss.info/show/$showID.rss")

        val rssFeed = feedReader.read(showURL)
        return rssFeed.items.mapNotNull { it.parseEpisodeOrNull(tvShow) }
            .distinctByMostRecent()
    }

    private fun RSSFeedItem.parseEpisodeOrNull(tvShow: TVShow): EpisodeFile? {
        return try {
            this.parseEpisode(tvShow)
        } catch (e: RSSFeedException) {
            logger.warn { "Failed to obtain episode information from RSS item: $this" }
            null
        }
    }
}

/**
 * Converts this [RSSFeedItem] into an [EpisodeFile] and returns the result.
 *
 * @throws RSSFeedException if it fails to find the episode information from the feed
 * @author David Fialho (dfialho@protonmail.com)
 */
internal fun RSSFeedItem.parseEpisode(tvShow: TVShow): EpisodeFile {
    val episode = try {
        parseEpisodeTitle(this.title)
    } catch (e: IllegalArgumentException) {
        throw RSSFeedException("Failed to parse episode information from RSS item: $this", e)
    }

    return EpisodeFile(
        episode = Episode(
            tvShow = tvShow,
            season = episode.season,
            number = episode.number,
            title = episode.title
        ),
        quality = episode.quality,
        link = this.link,
        publishDate = this.publishedDate
    )
}

/**
 * Returns a list containing only episode files from the given iterable having distinct episodes files based on
 * its ID, selecting the most recent episode file.
 *
 * @author David Fialho (dfialho@protonmail.com)
 */
private fun Iterable<EpisodeFile>.distinctByMostRecent(): List<EpisodeFile> {
    val distinctEpisodes = mutableMapOf<ID, EpisodeFile>()

    for (episodeFile in this) {

        distinctEpisodes.merge(EpisodeIDGenerator.getID(episodeFile), episodeFile) { oldFile, newFile ->
            // Update existing episode only if the new one is more recent
            if (newFile.publishDate.isAfter(oldFile.publishDate)) {
                newFile
            } else {
                oldFile
            }
        }
    }

    return distinctEpisodes.values.toList()
}

