package kz.zhombie.museum.ui.base

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment

open class BaseDialogFragment constructor(
    @LayoutRes contentLayoutId: Int
) : DialogFragment(contentLayoutId) {

    companion object {
        private val TAG = BaseDialogFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    /**
     * Checks if system UI (status bar and navigation bar) is shown or we are in fullscreen mode.
     */
    protected fun isSystemUiShown(): Boolean {
        return true
    }

    /**
     * Shows or hides system UI (status bar and navigation bar).
     */
    protected fun showSystemUi(show: Boolean) {
    }

}