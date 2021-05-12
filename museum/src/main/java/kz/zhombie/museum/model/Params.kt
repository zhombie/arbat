package kz.zhombie.museum.model

internal data class Params constructor(
    val paintings: List<Painting>,
    val startPosition: Int = DEFAULT_START_POSITION,
    val isFooterViewEnabled: Boolean = DEFAULT_FOOTER_VIEW_ENABLED
) {

    companion object {
        const val DEFAULT_START_POSITION = 0
        const val DEFAULT_FOOTER_VIEW_ENABLED = false
    }

    constructor(
        paintings: List<Painting>,
        startPosition: Int?,
        isFooterViewEnabled: Boolean?
    ) : this(
        paintings = paintings,
        startPosition = startPosition ?: DEFAULT_START_POSITION,
        isFooterViewEnabled = isFooterViewEnabled ?: DEFAULT_FOOTER_VIEW_ENABLED
    )

}