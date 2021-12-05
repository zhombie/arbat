package kz.zhombie.museum.exception

import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.Museum

class PaintingLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${PaintingLoader::class.java.simpleName} not initialized at ${Museum::class.java.simpleName}"

}