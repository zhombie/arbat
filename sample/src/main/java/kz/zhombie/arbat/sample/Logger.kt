package kz.zhombie.arbat.sample

import android.util.Log

internal object Logger {
    private const val TAG = "Sample"

    fun debug(tag: String = TAG, message: String) {
        Log.d(tag, message)
    }

    fun error(tag: String = TAG, message: String) {
        Log.e(tag, message)
    }
}