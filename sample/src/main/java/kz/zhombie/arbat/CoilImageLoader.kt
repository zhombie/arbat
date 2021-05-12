package kz.zhombie.arbat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.util.DebugLogger
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.component.CircularProgressDrawable

class CoilImageLoader constructor(context: Context) : PaintingLoader {

    companion object {
        private val TAG = CoilImageLoader::class.java.simpleName
    }

    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .componentRegistry {
                // Video frame
                add(VideoFrameFileFetcher(context))
                add(VideoFrameUriFetcher(context))
                add(VideoFrameDecoder(context))

                // GIF
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder(context))
                } else {
                    add(GifDecoder())
                }

                // SVG
                add(SvgDecoder(context))
            }
            .crossfade(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private val hashMap = hashMapOf<ImageView, Disposable>()

    private val circularProgressDrawable by lazy {
        val it = CircularProgressDrawable(context)
        it.setStyle(CircularProgressDrawable.LARGE)
        it.arrowEnabled = false
        it.centerRadius = 60F
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 11F
        it.setColorSchemeColors(ContextCompat.getColor(context, R.color.white))
        it
    }

    override fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri) {
        Log.d(TAG, "loadSmallImage() -> imageView: $imageView")

        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .data(uri)
            .error(R.drawable.museum_bg_black)
//            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()

        imageLoader.enqueue(request)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        Log.d(TAG, "loadFullscreenImage() -> imageView: $imageView")

        fun startProgress() {
            if (!circularProgressDrawable.isRunning) {
                circularProgressDrawable.start()
            }
        }

        fun stopProgress() {
            if (circularProgressDrawable.isRunning) {
                circularProgressDrawable.stop()
            }
        }

        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .placeholder(circularProgressDrawable)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(ViewSizeResolver(imageView))
            .listener(
                onStart = {
                    Log.d(TAG, "onStart()")
                    startProgress()
                },
                onCancel = {
                    Log.d(TAG, "onCancel()")
                    stopProgress()
                },
                onError = { _, throwable ->
                    Log.d(TAG, "onError() -> throwable: $throwable")
                    stopProgress()
                },
                onSuccess = { _, metadata: ImageResult.Metadata ->
                    Log.d(TAG, "onError() -> metadata: $metadata")
                    stopProgress()
                },
            )
            .target(imageView)
            .build()

        hashMap[imageView] = imageLoader.enqueue(request)
    }

    override fun dispose(imageView: ImageView) {
        Log.d(TAG, "dispose() -> imageView: $imageView")

        if (hashMap[imageView] != null && hashMap[imageView]?.isDisposed == false) {
            hashMap[imageView]?.dispose()
        }

        hashMap.remove(imageView)

        imageView.setImageDrawable(null)
    }

    fun clearCache() {
        Log.d(TAG, "clearCache()")

        circularProgressDrawable.stop()
        imageLoader.memoryCache.clear()
        hashMap.clear()
    }

}