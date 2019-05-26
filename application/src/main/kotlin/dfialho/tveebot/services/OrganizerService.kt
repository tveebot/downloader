package dfialho.tveebot.services

import dfialho.tveebot.library.api.TVShowLibrary
import dfialho.tveebot.library.lib.EpisodeDownloadPackage
import dfialho.tveebot.toPrettyString
import dfialho.tveebot.tracker.api.models.EpisodeFile
import mu.KLogging
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OrganizerService(private val library: TVShowLibrary) : Service {
    companion object : KLogging()

    override val name: String = OrganizerService::class.simpleName!!

    private val executor = Executors.newSingleThreadExecutor()

    override fun start() = logStart(logger)

    override fun stop() = logStop(logger) {
        with(executor) {
            shutdown()

            try {
                awaitTermination(30, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                logger.warn { "Failed to stop Organizer Service cleanly: Timed out waiting for task to finish." }
            }
        }
    }

    fun store(episodeFile: EpisodeFile, fileLocation: Path) {
        executor.submit {
            try {
                logger.debug { "Storing episodeFile in library: ${episodeFile.toPrettyString()}" }
                library.store(episodeFile.episode, EpisodeDownloadPackage(fileLocation))
                logger.info { "Stored episodeFile ${episodeFile.toPrettyString()} in library" }

            } catch (e: Throwable) {
                logger.error(e) { "Failed to store episodeFile in library" }
            }
        }
    }
}