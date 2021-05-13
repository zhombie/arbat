package kz.zhombie.museum.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.commons.RecyclePagerAdapter
import com.alexvasilkov.gestures.views.GestureImageView
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.R
import kz.zhombie.museum.logging.Logger
import kz.zhombie.museum.model.Painting

internal class ViewPagerAdapter constructor(
    private val viewPager: ViewPager,
    private val paintingLoader: PaintingLoader,
    private val callback: () -> Unit
) : RecyclePagerAdapter<RecyclePagerAdapter.ViewHolder>() {

    companion object {
        private val TAG = ViewPagerAdapter::class.java.simpleName

        fun getImageView(holder: RecyclePagerAdapter.ViewHolder): GestureImageView? {
            if (holder is ViewHolder) {
                return holder.gestureImageView
            }
            return null
        }
    }

    var paintings: List<Painting> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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

    override fun getCount(): Int {
        return if (isActivated || !paintings.isNullOrEmpty()) {
            paintings.size
        } else {
            0
        }
    }

    override fun onCreateViewHolder(container: ViewGroup): RecyclePagerAdapter.ViewHolder {
        val holder = ViewHolder(container)

        // Enabling smooth scrolling when image panning turns into ViewPager scrolling.
        // Otherwise ViewPager scrolling will only be possible when image is in zoomed out state.
        holder.gestureImageView.controller.enableScrollInViewPager(viewPager)

        return holder
    }

    override fun onBindViewHolder(holder: RecyclePagerAdapter.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            paintingLoader.loadFullscreenImage(
                context = holder.itemView.context,
                imageView = holder.gestureImageView,
                uri = paintings[position].uri
            )
        }
    }

    override fun onRecycleViewHolder(holder: RecyclePagerAdapter.ViewHolder) {
        if (holder is ViewHolder) {
            paintingLoader.dispose(holder.gestureImageView)
        }
    }

    private inner class ViewHolder constructor(
        container: ViewGroup
    ) : RecyclePagerAdapter.ViewHolder(
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

}