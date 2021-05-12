package kz.zhombie.cinema.model

internal data class Params constructor(
    val movie: Movie,
    val isFooterViewEnabled: Boolean = DEFAULT_FOOTER_VIEW_ENABLED
) {

    companion object {
        const val DEFAULT_FOOTER_VIEW_ENABLED = false
    }

    constructor(
        movie: Movie,
        isFooterViewEnabled: Boolean?
    ) : this(
        movie = movie,
        isFooterViewEnabled = isFooterViewEnabled ?: DEFAULT_FOOTER_VIEW_ENABLED
    )

}