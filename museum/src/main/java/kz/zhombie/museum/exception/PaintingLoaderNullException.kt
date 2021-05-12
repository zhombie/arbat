package kz.zhombie.museum.exception

import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.Settings

class PaintingLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${PaintingLoader::class.java.simpleName} not initialized at ${Settings::class.java.simpleName}"

}