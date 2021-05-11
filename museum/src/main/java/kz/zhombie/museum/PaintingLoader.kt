package kz.zhombie.museum

import android.content.Context
import android.net.Uri
import android.widget.ImageView

interface PaintingLoader {
    fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri)
    fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri)

    fun dispose(imageView: ImageView)
}