package kz.zhombie.arbat.sample

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import coil.ImageLoader
import coil.clear
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.*
import coil.util.CoilUtils
import coil.util.DebugLogger
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.component.CircularProgressDrawable
import okhttp3.Cache

class CoilImageLoader constructor(
    private val context: Context
) : PaintingLoader, DefaultLifecycleObserver {

    companion object {
        private val TAG = CoilImageLoader::class.java.simpleName
    }

    private val imageLoader: ImageLoader = ImageLoader.Builder(context)
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

    private var cache: Cache? = null

    private var circularProgressDrawable: CircularProgressDrawable? = null

    init {
        try {
            cache = CoilUtils.createDefaultCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCircularProgressDrawable(): CircularProgressDrawable? {
        Logger.debug(TAG, "getCircularProgressDrawable() -> $circularProgressDrawable")
        if (circularProgressDrawable == null) {
            circularProgressDrawable = CircularProgressDrawable(context).apply {
                setStyle(CircularProgressDrawable.LARGE)
                arrowEnabled = false
                centerRadius = 60F
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 11F
                setColorSchemeColors(ContextCompat.getColor(context, R.color.white))
            }
        }
        return circularProgressDrawable
    }

    private fun startProgress() {
        Logger.debug(TAG, "startProgress() -> $circularProgressDrawable")
        if (circularProgressDrawable?.isRunning == false) {
            circularProgressDrawable?.start()
        }
    }

    private fun stopProgress() {
        Logger.debug(TAG, "stopProgress() -> $circularProgressDrawable")
        if (circularProgressDrawable?.isRunning == true) {
            circularProgressDrawable?.stop()
        }
    }

    override fun enqueue(request: PaintingLoader.Request) {
        request.map().enqueueInternally()
    }

    override suspend fun execute(request: PaintingLoader.Request) {
        request.map().executeInternally()
    }

    private fun PaintingLoader.Request.map(): ImageRequest =
        ImageRequest.Builder(context)
            .bitmapConfig(bitmapConfig)
            .data(data)
            .placeholder(placeholderDrawable ?: getCircularProgressDrawable())
            .apply {
                if (crossfade.isEnabled) {
                    crossfade(crossfade.isEnabled)
                    crossfade(crossfade.duration)
                }

                if (errorDrawable == null) {
                    error(R.drawable.museum_bg_black)
                } else {
                    error(errorDrawable)
                }

                when (scale) {
                    PaintingLoader.Request.Scale.FILL ->
                        scale(Scale.FILL)
                    PaintingLoader.Request.Scale.FIT ->
                        scale(Scale.FIT)
                }

                when (val size = size) {
                    PaintingLoader.Request.Size.Inherit -> {
                        precision(Precision.AUTOMATIC)
                        size(ViewSizeResolver(imageView))
                    }
                    PaintingLoader.Request.Size.Original -> {
                        precision(Precision.AUTOMATIC)
                        size(OriginalSize)
                    }
                    is PaintingLoader.Request.Size.Pixel -> {
                        precision(Precision.EXACT)
                        size(size.width, size.height)
                    }
                }
            }
            .listener(
                onStart = {
                    Logger.debug(TAG, "onStart()")
                    startProgress()
                    listener?.onStart(this)
                },
                onCancel = {
                    Logger.debug(TAG, "onCancel()")
                    stopProgress()
                    listener?.onCancel(this)
                },
                onError = { _, throwable ->
                    Logger.debug(TAG, "onError() -> throwable: $throwable")
                    stopProgress()
                    listener?.onError(this, throwable)
                },
                onSuccess = { _, metadata: ImageResult.Metadata ->
                    Logger.debug(TAG, "onError() -> metadata: $metadata")
                    stopProgress()
                    listener?.onSuccess(this)
                }
            )
            .target(imageView)
            .build()

    private fun ImageRequest.enqueueInternally(): Disposable =
        imageLoader.enqueue(this)

    private suspend fun ImageRequest.executeInternally(): ImageResult =
        imageLoader.execute(this)

    override fun dispose(imageView: ImageView) {
        Logger.debug(TAG, "dispose() -> imageView: $imageView")

        imageView.clear()
        imageView.setImageDrawable(null)
    }

    override fun clearCache() {
        Logger.debug(TAG, "clearCache()")

        try {
            cache?.directory()?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopProgress()

        imageLoader.memoryCache.clear()
    }

    /**
     * [androidx.lifecycle.DefaultLifecycleObserver] implementation
     */

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Logger.debug(TAG, "onDestroy()")
        clearCache()
    }

}