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

/**
 * True when any visible view at the raw point that is smaller than [PointerPlayback.isContainerSized] is
 * clickable or focusable. Every overlapping view is checked, so a transparent screen-sized view stacked above
 * a Leanback control button does not hide the button from the test.
 */
fun isControlAt(root: View, rawX: Float, rawY: Float): Boolean {
	if (root.visibility != View.VISIBLE || root.alpha == 0f || !root.containsRawPoint(rawX, rawY)) return false
	val isContainer = PointerPlayback.isContainerSized(root.width, root.height, root.rootView.width, root.rootView.height)
	if (!isContainer && (root.isClickable || root.isFocusable)) return true
	if (root is ViewGroup) {
		for (i in 0 until root.childCount) {
			if (isControlAt(root.getChildAt(i), rawX, rawY)) return true
		}
	}
	return false
}

private fun View.containsRawPoint(rawX: Float, rawY: Float): Boolean {
	val loc = IntArray(2)
	getLocationOnScreen(loc)
	return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height).contains(rawX.toInt(), rawY.toInt())
}
