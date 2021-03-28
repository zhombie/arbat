package kz.zhombie.museum

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition
import kz.zhombie.museum.MuseumDialogFragment

interface MuseumDialogFragmentListener {
    fun onTrackViewPosition(view: View)
    fun onTrackViewPosition(viewPosition: ViewPosition)

    fun setArtworkView(view: View): MuseumDialogFragment
}