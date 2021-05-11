package kz.zhombie.museum

import android.widget.ImageView

interface MuseumDialogFragmentListener {
    fun setCanvasView(imageView: ImageView?): MuseumDialogFragment
}