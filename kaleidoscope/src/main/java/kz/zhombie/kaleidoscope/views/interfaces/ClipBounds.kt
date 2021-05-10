package kz.zhombie.kaleidoscope.views.interfaces

import android.graphics.RectF

interface ClipBounds {
    /**
     * Clips view so only [rect] part will be drawn.
     *
     * @param rect Rectangle to clip view bounds, or `null` to turn clipping off
     */
    fun clipBounds(rect: RectF?)
}