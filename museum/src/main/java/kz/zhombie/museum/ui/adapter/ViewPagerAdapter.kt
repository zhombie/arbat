package kz.zhombie.museum.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.GestureController.OnStateChangeListener
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.GestureImageView
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.R
import kz.zhombie.museum.logging.Logger
import kz.zhombie.museum.model.Painting
import kotlin.math.max

internal class ViewPagerAdapter constructor(
    private val paintingLoader: PaintingLoader,
    private val callback: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ViewPagerAdapter::class.java.simpleName
    }

    var paintings: List<Painting> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var recyclerView: RecyclerView? = null

    private var isActivated = false

    fun setActivated(isActivated: Boolean) {
        if (this.isActivated != isActivated) {
            this.isActivated = isActivated
            notifyDataSetChanged()
        }
    }

    fun getItem(position: Int): Painting {
        return paintings[position]
    }

    fun getImageView(position: Int): GestureImageView? {
        val holder = recyclerView?.findViewHolderForLayoutPosition(position)
        return if (holder is ViewHolder) holder.gestureImageView else null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun getItemCount(): Int {
        return if (isActivated || !paintings.isNullOrEmpty()) {
            paintings.size
        } else {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = ViewHolder(parent)

        // Enabling smooth scrolling when image panning turns into ViewPager scrolling.
        // Otherwise ViewPager scrolling will only be possible when image is in zoomed out state.
        val controller = holder.gestureImageView.controller
        controller.addOnStateChangeListener(DynamicZoom(controller.settings))

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            paintingLoader.loadFullscreenImage(
                context = holder.itemView.context,
                imageView = holder.gestureImageView,
                uri = paintings[position].uri
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolder) {
            paintingLoader.dispose(holder.gestureImageView)
        }
    }

    private inner class ViewHolder constructor(
        container: ViewGroup
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(container.context)
            .inflate(R.layout.museum_cell_image, container, false)
    ) {

        val gestureImageView: GestureImageView = itemView.findViewById(R.id.gestureImageView)

        init {
            Logger.debug(TAG, "${ViewHolder::class.java.simpleName} created")

            // Settings
            gestureImageView.controller.settings
                .setAnimationsDuration(225L)
                .setBoundsType(Settings.Bounds.NORMAL)
                .setDoubleTapEnabled(true)
                .setExitEnabled(true)
                .setExitType(Settings.ExitType.SCROLL)
                .setFillViewport(true)
                .setFitMethod(Settings.Fit.INSIDE)
                .setFlingEnabled(true)
                .setGravity(Gravity.CENTER)
                .setMaxZoom(3.5F)
                .setMinZoom(0F)
                .setPanEnabled(true)
                .setRotationEnabled(false)
                .setRestrictRotation(true)
                .isZoomEnabled = true

            gestureImageView.setOnClickListener { callback() }
        }
    }

    // Dynamically set double tap zoom level to fill the viewport
    private class DynamicZoom constructor(private val settings: Settings) : OnStateChangeListener {
        override fun onStateChanged(state: State?) {
            updateZoomLevels()
        }

        override fun onStateReset(oldState: State?, newState: State?) {
            updateZoomLevels()
        }

        private fun updateZoomLevels() {
            val scaleX = settings.viewportW.toFloat() / settings.imageW
            val scaleY = settings.viewportH.toFloat() / settings.imageH
            settings.doubleTapZoom = max(scaleX, scaleY)
        }
    }

}