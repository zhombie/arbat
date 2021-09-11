package kz.zhombie.arbat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
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
import coil.util.CoilUtils
import coil.util.DebugLogger
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.component.CircularProgressDrawable

class CoilImageLoader constructor(
    private val context: Context,
    private val isGlobalInstance: Boolean = true
) : PaintingLoader, LifecycleObserver {

    companion object {
        private val TAG = CoilImageLoader::class.java.simpleName
    }

    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .allowHardware(true)
            .availableMemoryPercentage(0.25)
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
            }
            .crossfade(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private val cache by lazy {
        try {
            CoilUtils.createDefaultCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private var hashMap: HashMap<ImageView, Disposable>? = null

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

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")
        clearCache()
    }

    override fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri) {
        Logger.debug(TAG, "loadSmallImage() -> imageView: $imageView")

        ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .data(uri)
            .error(R.drawable.museum_bg_black)
//            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()
            .hashMap(imageView)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        Logger.debug(TAG, "loadFullscreenImage() -> imageView: $imageView")

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

        ImageRequest.Builder(context)
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
                    Logger.debug(TAG, "onStart()")
                    startProgress()
                },
                onCancel = {
                    Logger.debug(TAG, "onCancel()")
                    stopProgress()
                },
                onError = { _, throwable ->
                    Logger.debug(TAG, "onError() -> throwable: $throwable")
                    stopProgress()
                },
                onSuccess = { _, metadata: ImageResult.Metadata ->
                    Logger.debug(TAG, "onError() -> metadata: $metadata")
                    stopProgress()
                },
            )
            .target(imageView)
            .build()
            .hashMap(imageView)
    }

    private fun ImageRequest.hashMap(imageView: ImageView) {
        if (isGlobalInstance) {
            imageLoader.enqueue(this)
        } else {
            if (hashMap == null) {
                hashMap = hashMapOf()
            }
            if (hashMap?.isNotEmpty() == true) {
                if ((hashMap?.size ?: 0) > 50) {
                    hashMap?.remove(hashMap?.entries?.first()?.key)
                }
            }
            hashMap?.set(imageView, imageLoader.enqueue(this))
        }
    }

    override fun dispose(imageView: ImageView) {
        Logger.debug(TAG, "dispose() -> imageView: $imageView")

        if (isGlobalInstance) {
            imageView.setImageDrawable(null)
        } else {
            if (hashMap == null) {
                imageView.setImageDrawable(null)
            } else {
                if (hashMap?.get(imageView) != null && hashMap?.get(imageView)?.isDisposed == false) {
                    hashMap?.get(imageView)?.dispose()
                }

                hashMap?.remove(imageView)

                imageView.setImageDrawable(null)
            }
        }
    }

    fun clearCache() {
        Logger.debug(TAG, "clearCache()")

        try {
            cache?.directory()?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        circularProgressDrawable.stop()

        imageLoader.memoryCache.clear()

        hashMap?.forEach { dispose(it.key) }
        hashMap?.clear()
        hashMap = null
    }

}