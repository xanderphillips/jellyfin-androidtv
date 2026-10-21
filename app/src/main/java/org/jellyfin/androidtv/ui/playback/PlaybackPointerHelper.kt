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
 * The smallest visible clickable or focusable view at the raw point that is not screen-sized, or null. Every
 * overlapping view is checked, so a transparent screen-sized view stacked above a Leanback control button does
 * not hide the button, and children are preferred over their parents.
 */
fun controlAt(root: View, rawX: Float, rawY: Float): View? {
	if (root.visibility != View.VISIBLE || root.alpha == 0f || !root.containsRawPoint(rawX, rawY)) return null
	if (root is ViewGroup) {
		for (i in 0 until root.childCount) {
			controlAt(root.getChildAt(i), rawX, rawY)?.let { return it }
		}
	}
	val isContainer = PointerPlayback.isContainerSized(root.width, root.height, root.rootView.width, root.rootView.height)
	return root.takeIf { !isContainer && (it.isClickable || it.isFocusable) }
}

private fun View.containsRawPoint(rawX: Float, rawY: Float): Boolean {
	val loc = IntArray(2)
	getLocationOnScreen(loc)
	return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height).contains(rawX.toInt(), rawY.toInt())
}
