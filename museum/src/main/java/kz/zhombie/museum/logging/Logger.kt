package kz.zhombie.museum.logging

import android.util.Log
import kz.zhombie.museum.Museum

internal object Logger {
    fun debug(tag: String = Museum.TAG, message: String) {
        if (Museum.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun error(tag: String = Museum.TAG, message: String) {
        if (Museum.isLoggingEnabled()) {
            Log.e(tag, message)
        }
    }
}