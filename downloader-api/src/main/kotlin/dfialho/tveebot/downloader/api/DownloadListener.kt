package dfialho.tveebot.downloader.api

/**
 * A [DownloadListener] is notified by the [DownloadEngine] with which it is registered about relevant events regarding
 * the downloads, such as, when a download has finished.
 */
interface DownloadListener {

    /**
     * Invoked with the [handle] of a download when that download has finished downloading.
     */
    fun onFinishedDownload(handle: DownloadHandle)
}
