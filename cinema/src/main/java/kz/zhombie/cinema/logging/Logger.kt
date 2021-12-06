package kz.zhombie.cinema.logging

import android.util.Log
import kz.zhombie.cinema.Cinema

internal object Logger {
    fun debug(tag: String = Cinema.TAG, message: String) {
        if (Cinema.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun error(tag: String = Cinema.TAG, message: String) {
        if (Cinema.isLoggingEnabled()) {
            Log.e(tag, message)
        }
    }
}