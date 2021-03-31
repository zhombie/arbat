package kz.zhombie.arbat

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment

class MainActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1200px-Image_created_with_a_mobile_phone.png"

        private const val VIDEO_THUMBNAIL_URL = "https://i.ytimg.com/vi/2vgZTTLW81k/hqdefault.jpg?sqp=-oaymwEjCNACELwBSFryq4qpAxUIARUAAAAAGAElAADIQj0AgKJDeAE=&rs=AOn4CLAR_mpN_wJtXsfcZpTvUgX5WLUdGQ"
        private const val VIDEO_URL = "https://datanapps.com/public/dnarestapi/media/videos/MyExerciseMotivation.mp4"
    }

    private var imageView: ImageView? = null
    private var imageView2: ImageView? = null

    private val imageLoader by lazy { CoilImageLoader() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        imageView2 = findViewById(R.id.imageView2)

        imageView?.let { imageView ->
            imageLoader.loadSmallImage(this, imageView, Uri.parse(IMAGE_URL))
        }

        imageView2?.let { imageView ->
            imageLoader.loadSmallImage(this, imageView, Uri.parse(VIDEO_THUMBNAIL_URL))
        }

        imageView?.setOnClickListener {
            val dialogFragment = MuseumDialogFragment.Builder()
                .setUri(Uri.parse(IMAGE_URL))
                .setTitle("Image")
                .setSubtitle("Subtitle")
                .setStartViewPosition(it)
                .setArtworkView(it)
                .setArtworkLoader(CoilImageLoader())
                .setCallback(object : MuseumDialogFragment.Callback {
                    override fun onPictureShow(delay: Long) {
                        it.visibility = View.VISIBLE
                    }

                    override fun onPictureHide(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.INVISIBLE }, delay)
                    }
                })
                .build()

            dialogFragment.show(supportFragmentManager, MuseumDialogFragment::class.java.simpleName)
        }

        imageView2?.setOnClickListener {
            val dialogFragment = CinemaDialogFragment.Builder()
                .setUri(Uri.parse(VIDEO_URL))
                .setTitle("Video")
                .setSubtitle("Subtitle")
                .setStartViewPosition(it)
                .setScreenView(it)
                .setCallback(object : CinemaDialogFragment.Callback {
                    override fun onMovieShow(delay: Long) {
                        it.visibility = View.VISIBLE
                    }

                    override fun onMovieHide(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.INVISIBLE }, delay)
                    }
                })
                .build()

            dialogFragment.show(supportFragmentManager, CinemaDialogFragment::class.java.simpleName)
        }
    }

}