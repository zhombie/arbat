package kz.zhombie.cinema

import android.view.View

interface CinemaDialogFragmentListener {
    fun setScreenView(view: View?): CinemaDialogFragment
}