package kz.zhombie.radio.logging

import android.util.Log
import kz.zhombie.radio.Settings

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