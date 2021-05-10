package kz.zhombie.kaleidoscope.views.interfaces

import kz.zhombie.kaleidoscope.animation.ViewPositionAnimator

/**
 * Common interface for views supporting position animation.
 */
interface AnimatorView {
    /**
     * @return [ViewPositionAnimator] instance to control animation from other view position.
     */
    fun getPositionAnimator(): ViewPositionAnimator
}