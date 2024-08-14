package io.github.luiisca.floating.views.helpers

import android.content.res.Resources

internal fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
internal fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
internal fun Float.toPx(): Float = (this * Resources.getSystem().displayMetrics.density)
