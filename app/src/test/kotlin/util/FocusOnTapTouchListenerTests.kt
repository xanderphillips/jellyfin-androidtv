package org.jellyfin.androidtv.util

import android.view.MotionEvent
import android.view.View
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FocusOnTapTouchListenerTests : FunSpec({
	fun view(
		pressed: Boolean = true,
		focusable: Boolean = true,
		focusableInTouchMode: Boolean = true,
		focused: Boolean = false,
	) = mockk<View> {
		every { isPressed } returns pressed
		every { isFocusable } returns focusable
		every { isFocusableInTouchMode } returns focusableInTouchMode
		every { isFocused } returns focused
		every { requestFocus() } returns true
	}

	fun event(action: Int) = mockk<MotionEvent> {
		every { actionMasked } returns action
	}

	test("Release on unfocused pressed view requests focus and does not consume the event") {
		val view = view()
		FocusOnTapTouchListener.onTouch(view, event(MotionEvent.ACTION_UP)) shouldBe false
		verify(exactly = 1) { view.requestFocus() }
	}

	test("Press does not request focus") {
		val view = view()
		FocusOnTapTouchListener.onTouch(view, event(MotionEvent.ACTION_DOWN)) shouldBe false
		verify(exactly = 0) { view.requestFocus() }
	}

	test("Release after pointer left the view does not request focus") {
		val view = view(pressed = false)
		FocusOnTapTouchListener.onTouch(view, event(MotionEvent.ACTION_UP)) shouldBe false
		verify(exactly = 0) { view.requestFocus() }
	}

	test("Release on already focused view does not request focus") {
		val view = view(focused = true)
		FocusOnTapTouchListener.onTouch(view, event(MotionEvent.ACTION_UP)) shouldBe false
		verify(exactly = 0) { view.requestFocus() }
	}

	test("Release on view not focusable in touch mode does not request focus") {
		val view = view(focusableInTouchMode = false)
		FocusOnTapTouchListener.onTouch(view, event(MotionEvent.ACTION_UP)) shouldBe false
		verify(exactly = 0) { view.requestFocus() }
	}
})
