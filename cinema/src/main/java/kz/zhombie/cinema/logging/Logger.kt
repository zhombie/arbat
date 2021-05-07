package kz.zhombie.cinema.logging

import android.util.Log
import kz.zhombie.cinema.Settings

internal object Logger {
    private const val TAG = "Cinema"

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