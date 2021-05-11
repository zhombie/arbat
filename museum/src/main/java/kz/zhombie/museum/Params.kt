package kz.zhombie.museum

import kz.zhombie.museum.model.Painting

internal data class Params constructor(
    val paintings: List<Painting>,
    val isFooterViewEnabled: Boolean
)