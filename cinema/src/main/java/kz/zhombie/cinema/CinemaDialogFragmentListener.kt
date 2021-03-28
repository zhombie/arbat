package kz.zhombie.cinema

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition
import kz.zhombie.cinema.CinemaDialogFragment

interface CinemaDialogFragmentListener {
    fun onTrackViewPosition(view: View)
    fun onTrackViewPosition(viewPosition: ViewPosition)

    fun setScreenView(view: View): CinemaDialogFragment
}