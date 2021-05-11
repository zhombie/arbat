package kz.zhombie.museum

import com.alexvasilkov.gestures.animation.ViewPosition
import kz.zhombie.museum.model.Painting

internal data class Params constructor(
    val paintings: List<Painting>,
    val isFooterViewEnabled: Boolean
)