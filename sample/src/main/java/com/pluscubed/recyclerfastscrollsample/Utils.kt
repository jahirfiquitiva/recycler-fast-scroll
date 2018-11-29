package com.pluscubed.recyclerfastscrollsample

import android.content.res.Resources

internal inline val Float.dpToPx: Float
    get() = this * Resources.getSystem().displayMetrics.density

internal inline val Int.dpToPx: Int
    get() = toFloat().dpToPx.toInt()