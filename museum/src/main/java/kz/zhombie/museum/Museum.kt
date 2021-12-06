package kz.zhombie.museum

import android.content.Context
import kz.zhombie.museum.exception.PaintingLoaderNullException
import kz.zhombie.museum.logging.Logger

object Museum {
    internal const val TAG = "Museum"

    data class Configuration constructor(
        val isLoggingEnabled: Boolean
    )

    interface Factory {
        fun getMuseumConfiguration(): Configuration
    }

    private var configuration: Configuration? = null
    private var configurationFactory: Factory? = null

    private var paintingLoader: PaintingLoader? = null
    private var paintingLoaderFactory: PaintingLoader.Factory? = null

    fun isLoggingEnabled(): Boolean = configuration?.isLoggingEnabled ?: false

    @Synchronized
    fun getPaintingLoader(context: Context?): PaintingLoader =
        paintingLoader ?: setPaintingLoaderFactory(context)

    @Synchronized
    fun setPaintingLoader(loader: PaintingLoader) {
        paintingLoaderFactory = null
        paintingLoader = loader
    }

    @Synchronized
    fun setPaintingLoader(factory: PaintingLoader.Factory) {
        paintingLoaderFactory = factory
        paintingLoader = null
    }

    @Synchronized
    fun setPaintingLoaderFactory(context: Context?): PaintingLoader {
        paintingLoader?.let { return it }

        paintingLoader = paintingLoaderFactory?.getPaintingLoader()
            ?: (context?.applicationContext as? PaintingLoader.Factory)?.getPaintingLoader()

        paintingLoaderFactory = null

        return paintingLoader ?: throw PaintingLoaderNullException()
    }

    @Synchronized
    fun getConfiguration(context: Context?): Configuration =
        configuration ?: setConfigurationFactory(context)

    @Synchronized
    fun setConfiguration(configuration: Configuration?) {
        configurationFactory = null
        this.configuration = configuration
    }

    @Synchronized
    fun setConfiguration(factory: Factory) {
        configuration = null
        configurationFactory = factory
    }

    @Synchronized
    fun setConfigurationFactory(context: Context?): Configuration {
        configuration?.let { return it }

        configuration = configurationFactory?.getMuseumConfiguration()
            ?: (context?.applicationContext as? Factory)?.getMuseumConfiguration()
            ?: Configuration(false)

        Logger.debug(TAG, "setConfigurationFactory() -> $configuration")

        configurationFactory = null

        return requireNotNull(configuration)
    }

    @Synchronized
    fun clear() {
        Logger.debug(TAG, "clear()")

        configuration = null
        configurationFactory = null

        paintingLoader = null
        paintingLoaderFactory = null
    }

}
