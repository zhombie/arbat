package kz.zhombie.radio

fun Radio.getPositionByProgress(progress: Int): Long {
    return (progress * duration) / 100L
}

fun Radio?.getDurationOrZeroIfUnset(): Long {
    if (this == null) return 0L
    return if (duration < 0) {
        0L
    } else {
        duration
    }
}