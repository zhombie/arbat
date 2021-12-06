package kz.zhombie.radio.logging

import android.util.Log
import kz.zhombie.radio.Radio
import kz.zhombie.radio.INSTANCE

internal object Logger {
    fun debug(tag: String = Radio.TAG, message: String) {
        if (INSTANCE.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun error(tag: String = Radio.TAG, message: String) {
        if (INSTANCE.isLoggingEnabled()) {
            Log.e(tag, message)
        }
    }
}