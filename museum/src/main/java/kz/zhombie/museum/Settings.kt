package kz.zhombie.museum

import kz.zhombie.museum.exception.PaintingLoaderNullException

internal object Settings {

    private var permanentPaintingLoader: PaintingLoader? = null
    private var temporaryPaintingLoader: PaintingLoader? = null
    private var isLoggingEnabled: Boolean = false

    fun getPaintingLoader(): PaintingLoader =
        getTemporaryPaintingLoader() ?: getPermanentPaintingLoader()

    fun hasPermanentPaintingLoader(): Boolean =
        permanentPaintingLoader != null

    fun getPermanentPaintingLoader(): PaintingLoader =
        requireNotNull(permanentPaintingLoader) { PaintingLoaderNullException() }

    fun setPermanentPaintingLoader(paintingLoader: PaintingLoader) {
        this.permanentPaintingLoader = paintingLoader
    }

    fun hasTemporaryPaintingLoader(): Boolean =
        temporaryPaintingLoader != null

    fun getTemporaryPaintingLoader(): PaintingLoader? =
        temporaryPaintingLoader

    fun setTemporaryPaintingLoader(paintingLoader: PaintingLoader) {
        this.temporaryPaintingLoader = paintingLoader
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

    fun cleanupTemporarySettings() {
        temporaryPaintingLoader = null
    }

    fun cleanupSettings() {
        permanentPaintingLoader = null
        temporaryPaintingLoader = null
        isLoggingEnabled = false
    }

}