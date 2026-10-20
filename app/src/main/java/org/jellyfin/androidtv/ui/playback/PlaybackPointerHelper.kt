package org.jellyfin.androidtv.ui.playback

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/** Finds the visible view with [id] whose on-screen bounds contain the raw point, or null. */
fun findVisibleViewById(root: View, id: Int, rawX: Float, rawY: Float): View? {
	if (root.visibility != View.VISIBLE) return null
	if (root.id == id && root.containsRawPoint(rawX, rawY)) return root
	if (root is ViewGroup) {
		for (i in root.childCount - 1 downTo 0) {
			findVisibleViewById(root.getChildAt(i), id, rawX, rawY)?.let { return it }
		}
	}
	return null
}

/** True when the topmost visible view at the raw point, or one of its parents, is clickable. */
fun isClickableAt(root: View, rawX: Float, rawY: Float): Boolean {
	var view: View? = topViewAt(root, rawX, rawY)
	while (view != null && view !== root) {
		if (view.isClickable) return true
		view = view.parent as? View
	}
	return false
}

private fun topViewAt(view: View, rawX: Float, rawY: Float): View? {
	if (view.visibility != View.VISIBLE || view.alpha == 0f || !view.containsRawPoint(rawX, rawY)) return null
	if (view is ViewGroup) {
		for (i in view.childCount - 1 downTo 0) {
			topViewAt(view.getChildAt(i), rawX, rawY)?.let { return it }
		}
	}
	return view
}

private fun View.containsRawPoint(rawX: Float, rawY: Float): Boolean {
	val loc = IntArray(2)
	getLocationOnScreen(loc)
	return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height).contains(rawX.toInt(), rawY.toInt())
}
