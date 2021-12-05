package kz.zhombie.museum

import android.content.Context
import kz.zhombie.museum.exception.PaintingLoaderNullException
import kz.zhombie.museum.logging.Logger

object Museum {
    internal const val TAG = "Museum"

    private var isLoggingEnabled: Boolean = false

    private var configuration: MuseumDialogFragment.Factory.Configuration? = null
    private var configurationFactory: MuseumDialogFragment.Factory? = null

    private var paintingLoader: PaintingLoader? = null
    private var paintingLoaderFactory: PaintingLoader.Factory? = null

    @Synchronized
    fun isLoggingEnabled(): Boolean = configuration?.isLoggingEnabled ?: isLoggingEnabled

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
    fun getConfiguration(context: Context?): MuseumDialogFragment.Factory.Configuration =
        configuration ?: setConfigurationFactory(context)

    @Synchronized
    fun setConfiguration(configuration: MuseumDialogFragment.Factory.Configuration?) {
        configurationFactory = null
        this.configuration = configuration
        isLoggingEnabled = configuration?.isLoggingEnabled ?: false
    }

    @Synchronized
    fun setConfiguration(factory: MuseumDialogFragment.Factory) {
        configuration = null
        isLoggingEnabled = false
        configurationFactory = factory
    }

    @Synchronized
    fun setConfigurationFactory(context: Context?): MuseumDialogFragment.Factory.Configuration {
        configuration?.let { return it }

        configuration = configurationFactory?.getMuseumConfiguration()
            ?: (context?.applicationContext as? MuseumDialogFragment.Factory)?.getMuseumConfiguration()
            ?: MuseumDialogFragment.Factory.Configuration(false)

        Logger.debug(TAG, "setConfigurationFactory() -> $configuration")

        configurationFactory = null

        isLoggingEnabled = configuration?.isLoggingEnabled ?: false

        return requireNotNull(configuration)
    }

    @Synchronized
    fun clear() {
        Logger.debug(TAG, "clear()")

        configuration = null
        configurationFactory = null

        paintingLoader = null
        paintingLoaderFactory = null

        isLoggingEnabled = false
    }

}
