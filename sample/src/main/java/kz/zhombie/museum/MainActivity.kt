package kz.zhombie.museum

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1200px-Image_created_with_a_mobile_phone.png"
    }

    private var imageView: ImageView? = null

    private val imageLoader by lazy { CoilImageLoader() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)

        imageView?.let { imageView ->
            imageLoader.loadFullscreenImage(this, imageView, Uri.parse(URL))
        }

        imageView?.setOnClickListener {
            val dialogFragment = MuseumDialogFragment.Builder()
                .setUri(Uri.parse(URL))
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
    }

}