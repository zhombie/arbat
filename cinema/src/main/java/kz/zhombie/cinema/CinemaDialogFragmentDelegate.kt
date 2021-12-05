package kz.zhombie.cinema

import android.view.View

interface CinemaDialogFragmentDelegate {
    fun setScreenView(view: View?): CinemaDialogFragment
}