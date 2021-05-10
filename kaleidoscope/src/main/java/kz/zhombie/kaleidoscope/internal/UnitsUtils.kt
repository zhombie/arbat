package kz.zhombie.kaleidoscope.internal

import android.content.Context
import android.util.TypedValue

internal object UnitsUtils {

    fun toPixels(context: Context, value: Float): Float {
        return toPixels(context, TypedValue.COMPLEX_UNIT_DIP, value)
    }

    fun toPixels(context: Context, type: Int, value: Float): Float {
        return TypedValue.applyDimension(type, value, context.resources.displayMetrics)
    }

}