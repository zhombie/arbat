package kz.zhombie.museum

import kz.zhombie.museum.exception.PaintingLoaderNullException

internal object Settings {

    private var paintingLoader: PaintingLoader? = null
    private var isLoggingEnabled: Boolean = false

    fun hasPaintingLoader(): Boolean {
        return paintingLoader != null
    }

    fun getPaintingLoader(): PaintingLoader {
        return requireNotNull(paintingLoader) { PaintingLoaderNullException() }
    }

    fun setPaintingLoader(paintingLoader: PaintingLoader) {
        this.paintingLoader = paintingLoader
    }

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled
    }

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

}