package kz.zhombie.cinema.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.cinema.R

internal class ViewHolder constructor(view: View) {

    val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
    val backgroundView: View = view.findViewById(R.id.backgroundView)
    val gestureFrameLayout: GestureFrameLayout = view.findViewById(R.id.gestureFrameLayout)
    val playerView: PlayerView = view.findViewById(R.id.playerView)
    val controllerView: FrameLayout = view.findViewById(R.id.controllerView)
    val playOrPauseButton: MaterialButton = view.findViewById(R.id.playOrPauseButton)
    val progressIndicator: CircularProgressIndicator = view.findViewById(R.id.progressIndicator)
    val footerView: LinearLayout = view.findViewById(R.id.footerView)
    val titleView: MaterialTextView = view.findViewById(R.id.titleView)
    val subtitleView: MaterialTextView = view.findViewById(R.id.subtitleView)

}