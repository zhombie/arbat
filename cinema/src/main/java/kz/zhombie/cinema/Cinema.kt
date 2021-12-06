package kz.zhombie.cinema

import android.content.Context
import kz.zhombie.cinema.logging.Logger

object Cinema {
    internal const val TAG = "Cinema"

    data class Configuration constructor(
        val isLoggingEnabled: Boolean
    )

    interface Factory {
        fun getCinemaConfiguration(): Configuration
    }

    private var configuration: Configuration? = null
    private var configurationFactory: Factory? = null

    fun isLoggingEnabled(): Boolean = configuration?.isLoggingEnabled ?: false

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

        configuration = configurationFactory?.getCinemaConfiguration()
            ?: (context?.applicationContext as? Factory)?.getCinemaConfiguration()
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
    }

}
