package kz.zhombie.kaleidoscope.internal

internal class GestureDebug private constructor() {

    companion object {
        private var debugFps = false
        private var debugAnimator = false
        private var drawDebugOverlay = false

        fun isDebugFps(): Boolean {
            return debugFps
        }

        fun setDebugFps(debug: Boolean) {
            debugFps = debug
        }

        fun isDebugAnimator(): Boolean {
            return debugAnimator
        }

        fun setDebugAnimator(debug: Boolean) {
            debugAnimator = debug
        }

        fun isDrawDebugOverlay(): Boolean {
            return drawDebugOverlay
        }

        fun setDrawDebugOverlay(draw: Boolean) {
            drawDebugOverlay = draw
        }
    }

}