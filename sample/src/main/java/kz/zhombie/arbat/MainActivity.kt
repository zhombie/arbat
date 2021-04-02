package kz.zhombie.arbat

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.radio.Radio
import kz.zhombie.radio.formatToDigitalClock

class MainActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1200px-Image_created_with_a_mobile_phone.png"

        private const val VIDEO_THUMBNAIL_URL = "https://i.ytimg.com/vi/2vgZTTLW81k/hqdefault.jpg?sqp=-oaymwEjCNACELwBSFryq4qpAxUIARUAAAAAGAElAADIQj0AgKJDeAE=&rs=AOn4CLAR_mpN_wJtXsfcZpTvUgX5WLUdGQ"
        private const val VIDEO_URL = "https://datanapps.com/public/dnarestapi/media/videos/MyExerciseMotivation.mp4"

        private const val AUDIO_URL = "https://cdn9.sefon.pro/prev/BJnG5DGWT87c2aPcXdhxaQ/1617434569/55/%D0%9C%D0%B8%D1%85%D0%B0%D0%B8%D0%BB%20%D0%9A%D1%80%D1%83%D0%B3%20-%20%D0%92%D0%BB%D0%B0%D0%B4%D0%B8%D0%BC%D0%B8%D1%80%D1%81%D0%BA%D0%B8%D0%B9%20%D0%A6%D0%B5%D0%BD%D1%82%D1%80%D0%B0%D0%BB%20%28192kbps%29.mp3"
    }

    private var imageView: ImageView? = null
    private var imageView2: ImageView? = null
    private var statusView: MaterialTextView? = null
    private var currentPositionView: MaterialTextView? = null
    private var durationView: MaterialTextView? = null
    private var createButton: MaterialButton? = null
    private var resetButton: MaterialButton? = null
    private var playButton: MaterialButton? = null
    private var pauseButton: MaterialButton? = null
    private var playOrPauseButton: MaterialButton? = null

    private var imageLoader: CoilImageLoader? = null

    private var dialogFragment: DialogFragment? = null

    private var radio: Radio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        imageView2 = findViewById(R.id.imageView2)
        statusView = findViewById(R.id.statusView)
        currentPositionView = findViewById(R.id.currentPositionView)
        durationView = findViewById(R.id.durationView)
        createButton = findViewById(R.id.createButton)
        resetButton = findViewById(R.id.resetButton)
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        playOrPauseButton = findViewById(R.id.playOrPauseButton)

        imageLoader = CoilImageLoader(this)

        setupMuseum()
        setupCinema()
        setupRadio()
    }

    override fun onDestroy() {
        super.onDestroy()

        imageLoader?.clearCache()
        imageLoader = null

        radio?.release()
        radio = null
    }

    private fun setupMuseum() {
        MuseumDialogFragment.init(requireNotNull(imageLoader), true)

        imageView?.let { imageView ->
            imageLoader?.loadSmallImage(this, imageView, Uri.parse(IMAGE_URL))
        }

        imageView?.setOnClickListener {
            dialogFragment?.dismiss()
            dialogFragment = null
            dialogFragment = MuseumDialogFragment.Builder()
                .setArtworkLoader(requireNotNull(imageLoader))
                .setArtworkView(it)
                .setUri(Uri.parse(IMAGE_URL))
                .setTitle("Image")
                .setSubtitle("Subtitle")
                .setStartViewPosition(it)
                .setFooterViewEnabled(true)
                .setCallback(object : MuseumDialogFragment.Callback {
                    override fun onPictureShow(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.VISIBLE }, delay)
                    }

                    override fun onPictureHide(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.INVISIBLE }, delay)
                    }
                })
                .show(supportFragmentManager)
        }
    }

    private fun setupCinema() {
        imageView2?.let { imageView ->
            imageLoader?.loadSmallImage(this, imageView, Uri.parse(VIDEO_THUMBNAIL_URL))
        }

        imageView2?.setOnClickListener {
            dialogFragment?.dismiss()
            dialogFragment = null
            dialogFragment = CinemaDialogFragment.Builder()
                .setScreenView(it)
                .setUri(Uri.parse(VIDEO_URL))
                .setTitle("Video")
                .setSubtitle("Subtitle")
                .setStartViewPosition(it)
                .setFooterViewEnabled(true)
                .setCallback(object : CinemaDialogFragment.Callback {
                    override fun onMovieShow(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.VISIBLE }, delay)
                    }

                    override fun onMovieHide(delay: Long) {
                        HandlerCompat.createAsync(Looper.getMainLooper())
                            .postDelayed({ it.visibility = View.INVISIBLE }, delay)
                    }
                })
                .show(supportFragmentManager)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupRadio() {
        fun ensureExistence(block: () -> Unit) {
            if (radio == null) {
                Toast.makeText(this, "Create at first! Click on \"Create\" button", Toast.LENGTH_SHORT).show()
            } else {
                block()
            }
        }

        createButton?.setOnClickListener {
            radio?.release()
            radio = null
            radio = Radio.Builder(this)
                .create(listener)
                .start(AUDIO_URL)
        }

        resetButton?.setOnClickListener {
            radio?.release()
            radio = null

            statusView?.text = "Status: UNKNOWN"
            currentPositionView?.text = "Current position: UNKNOWN"
            durationView?.text = "Duration: UNKNOWN"
        }

        playButton?.setOnClickListener {
            ensureExistence {
                radio?.play()
            }
        }

        pauseButton?.setOnClickListener {
            ensureExistence {
                radio?.pause()
            }
        }

        playOrPauseButton?.setOnClickListener {
            ensureExistence {
                radio?.playOrPause()
            }
        }
    }

    private val listener by lazy {
        object : Radio.Listener {
            override fun onPlayingStateChanged(isPlaying: Boolean) {
            }

            @SuppressLint("SetTextI18n")
            override fun onPlaybackStateChanged(state: Radio.PlaybackState) {
                when (state) {
                    Radio.PlaybackState.IDLE -> {
                        statusView?.text = "Status: IDLE"
                    }
                    Radio.PlaybackState.BUFFERING -> {
                        statusView?.text = "Status: BUFFERING"
                    }
                    Radio.PlaybackState.READY -> {
                        statusView?.text = "Status: READY"
                        currentPositionView?.text = "Current position: ${radio?.formatToDigitalClock(radio?.getCurrentPosition())}"
                        durationView?.text = "Duration: ${radio?.formatToDigitalClock(radio?.getDuration())}"
                    }
                    Radio.PlaybackState.ENDED -> {
                        statusView?.text = "Status: ENDED"
                    }
                }
            }
        }
    }

}