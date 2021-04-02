package kz.zhombie.radio

import android.util.Log

internal object Logger {
    private const val TAG = "Radio"

    fun debug(tag: String = TAG, message: String) {
        if (Settings.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun error(tag: String = TAG, message: String) {
        if (Settings.isLoggingEnabled()) {
            Log.e(tag, message)
        }
    }
}