package kz.zhombie.museum

import android.widget.ImageView

interface MuseumDialogFragmentDelegate {
    fun setImageView(imageView: ImageView?): MuseumDialogFragment
}