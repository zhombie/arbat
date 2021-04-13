package kz.zhombie.museum.logging

import android.util.Log
import kz.zhombie.museum.Settings

internal object Logger {
    private const val TAG = "Museum"

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