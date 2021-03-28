package kz.zhombie.museum

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

class CoilImageLoader : ArtworkLoader {

    override fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()

        Coil.enqueue(request)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(1280, 960)
            .target(imageView)
            .build()

        Coil.enqueue(request)
    }

}