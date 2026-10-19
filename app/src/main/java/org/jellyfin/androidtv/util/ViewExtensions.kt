package org.jellyfin.androidtv.util

import android.view.MotionEvent
import android.view.View

/**
 * Views that are focusable in touch mode consume the first tap to gain focus and only click on the second
 * tap. In leanback rows the focus change also scrolls the row, moving the item away from the pointer, so
 * pointer input (touchscreens, mice, Meta Quest controllers) can never select it. Requesting focus just
 * before the framework handles the release makes a single tap both focus and click the view.
 */
object FocusOnTapTouchListener : View.OnTouchListener {
	override fun onTouch(view: View, event: MotionEvent): Boolean {
		if (
			event.actionMasked == MotionEvent.ACTION_UP &&
			view.isPressed &&
			view.isFocusable &&
			view.isFocusableInTouchMode &&
			!view.isFocused
		) view.requestFocus()

		return false
	}
}

fun View.setFocusOnTap() = setOnTouchListener(FocusOnTapTouchListener)
