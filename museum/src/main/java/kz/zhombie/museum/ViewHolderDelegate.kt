package kz.zhombie.museum

import android.widget.ImageView

interface ViewHolderDelegate {
    fun getImageView(): ImageView?
}