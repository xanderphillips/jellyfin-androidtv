package org.jellyfin.androidtv.ui.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import org.jellyfin.androidtv.util.setFocusOnTap

/**
 * Focusable and clickable container for Compose card content in leanback rows.
 *
 * Compose reports every touch that lands on an embedded Android view (like the AsyncImageView used for card images) as
 * handled, so a ComposeView never gets to perform its own click. Intercepting all touch events here makes taps from
 * pointer devices (touchscreens, mice, Meta Quest controllers) click the card the same way a D-pad center press does.
 */
@SuppressLint("ViewConstructor")
class ComposeCardContainer(context: Context) : FrameLayout(context) {
	init {
		isFocusable = true
		isFocusableInTouchMode = true
		setFocusOnTap()
		descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
	}

	override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true

	// Hack to prevent Compose crash with leanback presenters
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (isAttachedToWindow) super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		else setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
	}
}
