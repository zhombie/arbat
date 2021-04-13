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

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_URL = "https://images.pexels.com/photos/2246476/pexels-photo-2246476.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260"

        private const val VIDEO_THUMBNAIL_URL = "https://i.ytimg.com/vi/2vgZTTLW81k/hqdefault.jpg?sqp=-oaymwEjCNACELwBSFryq4qpAxUIARUAAAAAGAElAADIQj0AgKJDeAE=&rs=AOn4CLAR_mpN_wJtXsfcZpTvUgX5WLUdGQ"
        private const val VIDEO_URL = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"

        private const val AUDIO_URL = "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3"
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
        radio?.let { lifecycle.removeObserver(it) }
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

    private fun setupRadio() {
        Radio.init(true)

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

            radio?.let { lifecycle.addObserver(it) }
        }

        resetButton?.setOnClickListener {
            radio?.release()
            radio?.let { lifecycle.removeObserver(it) }
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

            override fun onPlaybackPositionChanged(position: Long) {
                currentPositionView?.text = "Current position: ${radio?.formatToDigitalClock(position)}"
            }

            override fun onPlayerError(cause: Throwable?) {
                cause?.printStackTrace()
            }
        }
    }

}