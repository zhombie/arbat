package kz.zhombie.radio

import android.net.Uri

interface TrackInformation : TrackStateInformation {
    val currentSource: Uri?

    val duration: Long

    val currentPosition: Long
    val currentPercentage: Float

    val bufferedPosition: Long
    val bufferedPercentage: Int
    val totalBufferedDuration: Long
}