package kz.zhombie.radio

import java.util.concurrent.TimeUnit

fun Radio.formatToDigitalClock(milliseconds: Long?): String {
    if (isReleased()) return "00:00"
    if (milliseconds == null) return "00:00"
    return try {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds).toInt() % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).toInt() % 60
        when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            seconds > 0 -> String.format("00:%02d", seconds)
            else -> "00:00"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "00:00"
    }
}

fun Radio.getDisplayDuration(): String {
    return formatToDigitalClock(duration)
}

fun Radio.getDisplayCurrentPosition(): String {
    return formatToDigitalClock(currentPosition)
}