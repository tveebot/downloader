package dfialho.tveebot.downloader

import dfialho.tveebot.downloader.libtorrent.LibTorrentDownloadEngine
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

@SpringBootApplication
@EnableConfigurationProperties(DownloaderConfig::class)
class DownloaderApplication {

    @Bean
    fun downloaderInitializer(downloader: Downloader) = CommandLineRunner {
        downloader.download()
    }

    @Bean
    fun downloaderManager(config: DownloaderConfig): DownloadManager {

        if (!Files.isDirectory(config.savePath)) {
            throw IllegalArgumentException("The download directory does not exist: ${config.savePath}")
        }

        return DownloadManager(LibTorrentDownloadEngine(config.savePath))
    }

}

@ConfigurationProperties("downloader")
class DownloaderConfig {

    lateinit var savePath: Path
    lateinit var magnetLink: String
}
@Component
class Downloader(private val config: DownloaderConfig, private val downloadManager: DownloadManager) {

    companion object {
        val logger = LoggerFactory.getLogger(Downloader::class.java)
    }

    val exitLatch = CountDownLatch(1)

    init {
        downloadManager.start()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            logger.info("Closing downloader")
            downloadManager.stop()
            logger.info("Downloader closed")
        })

        val listener: EventListener = object : EventListener {
            override fun onDownloadFinished(reference: DownloadReference) {
                logger.info("Download is complete")
                exitLatch.countDown()
            }
        }

        downloadManager.engine.addListener(listener)
    }

    fun download() {
        val downloadHandle = downloadManager.engine.add(config.magnetLink)

        while (!exitLatch.await(1, TimeUnit.SECONDS)) {
            val status = downloadHandle.getStatus()

            logger.info("${status.name} (${status.state}): %.2f%% - ${status.rate / 1000} kB/s".format(status.progress * 100))
        }

        exitProcess(0)
    }

}

fun main(args: Array<String>) {
    runApplication<DownloaderApplication>()
}
