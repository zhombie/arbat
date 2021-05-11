package kz.zhombie.museum

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition
import kz.zhombie.museum.MuseumDialogFragment

interface MuseumDialogFragmentListener {
    fun setCanvasView(view: View): MuseumDialogFragment
}