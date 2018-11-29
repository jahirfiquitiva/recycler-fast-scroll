package com.pluscubed.recyclerfastscroll

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@Suppress("DEPRECATION")
internal fun View.setViewBackground(background: Drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        setBackground(background)
    } else {
        setBackgroundDrawable(background)
    }
}

internal val Context.isRTL: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    } else {
        false
    }

@ColorInt
internal fun Context.resolveColor(@AttrRes color: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(color))
    val resId = a.getColor(0, 0)
    a.recycle()
    return resId
}

internal inline val Float.dpToPx: Float
    get() = this * Resources.getSystem().displayMetrics.density

internal inline val Int.dpToPx: Int
    get() = toFloat().dpToPx.toInt()