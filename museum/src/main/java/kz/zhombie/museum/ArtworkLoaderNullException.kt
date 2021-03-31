package kz.zhombie.museum

class ArtworkLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${ArtworkLoader::class.java.simpleName} not initialized at ${Settings::class.java.simpleName}"

}