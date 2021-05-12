package kz.zhombie.museum.model

internal data class Position constructor(
    val itemPosition: Int,
    val imagePosition: Int
) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Position) return false
        return itemPosition == other.itemPosition && imagePosition == other.imagePosition
    }

    override fun hashCode(): Int {
        return 31 * itemPosition + imagePosition
    }

}