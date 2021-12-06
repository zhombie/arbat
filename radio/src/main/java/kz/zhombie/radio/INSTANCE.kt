package kz.zhombie.radio

import android.content.Context
import kz.zhombie.radio.logging.Logger

internal object INSTANCE {

    private var configuration: Radio.Configuration? = null
    private var configurationFactory: Radio.Factory? = null

    fun isLoggingEnabled(): Boolean = configuration?.isLoggingEnabled ?: false

    @Synchronized
    fun getConfiguration(context: Context?): Radio.Configuration =
        configuration ?: setConfigurationFactory(context)

    @Synchronized
    fun setConfiguration(configuration: Radio.Configuration?) {
        configurationFactory = null
        this.configuration = configuration
    }

    @Synchronized
    fun setConfiguration(factory: Radio.Factory) {
        configuration = null
        configurationFactory = factory
    }

    @Synchronized
    fun setConfigurationFactory(context: Context?): Radio.Configuration {
        configuration?.let { return it }

        configuration = configurationFactory?.getRadioConfiguration()
            ?: (context?.applicationContext as? Radio.Factory)?.getRadioConfiguration()
            ?: Radio.Configuration(false)

        Logger.debug(Radio.TAG, "setConfigurationFactory() -> $configuration")

        configurationFactory = null

        return requireNotNull(configuration)
    }

    @Synchronized
    fun clear() {
        Logger.debug(Radio.TAG, "clear()")

        configuration = null
        configurationFactory = null
    }

}