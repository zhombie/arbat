package kz.zhombie.arbat.sample

import android.app.Application
import kz.zhombie.cinema.Cinema
import kz.zhombie.museum.Museum
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.radio.Radio

class App : Application(),
    Cinema.Factory,
    Museum.Factory,
    PaintingLoader.Factory,
    Radio.Factory {

    private var imageLoader: PaintingLoader? = null

    override fun getCinemaConfiguration(): Cinema.Configuration {
        val isLoggingEnabled = BuildConfig.DEBUG
        return Cinema.Configuration(isLoggingEnabled)
    }

    override fun getMuseumConfiguration(): Museum.Configuration {
        val isLoggingEnabled = BuildConfig.DEBUG
        return Museum.Configuration(isLoggingEnabled)
    }

    override fun getPaintingLoader(): PaintingLoader {
        if (imageLoader == null) {
            imageLoader = CoilImageLoader(this)
        }
        return requireNotNull(imageLoader)
    }

    override fun getRadioConfiguration(): Radio.Configuration {
        val isLoggingEnabled = BuildConfig.DEBUG
        return Radio.Configuration(isLoggingEnabled)
    }

}