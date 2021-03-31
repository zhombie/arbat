package kz.zhombie.museum

internal object Settings {

    private var artworkLoader: ArtworkLoader? = null
    private var isLoggingEnabled: Boolean = false

    fun hasArtworkLoader(): Boolean {
        return artworkLoader != null
    }

    fun getArtworkLoader(): ArtworkLoader {
        return requireNotNull(artworkLoader) { ArtworkLoaderNullException() }
    }

    fun setArtworkLoader(artworkLoader: ArtworkLoader) {
        this.artworkLoader = artworkLoader
    }

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled
    }

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

}