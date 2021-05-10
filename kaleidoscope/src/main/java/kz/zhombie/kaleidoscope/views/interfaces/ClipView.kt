package kz.zhombie.kaleidoscope.views.interfaces

import android.graphics.RectF

interface ClipView {
    /**
     * Clips view so only [rect] part (modified by view's state) will be drawn.
     *
     * @param rect Clip rectangle or `null` to turn clipping off
     * @param rotation Clip rectangle rotation
     */
    fun clipView(rect: RectF?, rotation: Float)
}