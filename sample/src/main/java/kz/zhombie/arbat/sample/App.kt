package kz.zhombie.arbat.sample

import android.app.Application
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.PaintingLoader

class App : Application(), PaintingLoader.Factory, MuseumDialogFragment.Factory {

    private var imageLoader: PaintingLoader? = null

    override fun getPaintingLoader(): PaintingLoader {
        if (imageLoader == null) {
            imageLoader = CoilImageLoader(this)
        }
        return requireNotNull(imageLoader)
    }

    override fun getMuseumConfiguration(): MuseumDialogFragment.Factory.Configuration {
        val isLoggingEnabled = BuildConfig.DEBUG
        return MuseumDialogFragment.Factory.Configuration(isLoggingEnabled)
    }

}