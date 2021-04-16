package kz.zhombie.radio

interface RemoteControl {
    fun play()
    fun pause()
    fun playOrPause()
    fun stop(reset: Boolean)

    fun seekTo(position: Long)  // milliseconds
}