package org.jellyfin.androidtv.ui.playback

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GamepadPlaybackInputTest : FunSpec({
	test("button mapping") {
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_X, false) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_X, true) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_Y, false) shouldBe PlaybackInputAction.TOGGLE_CONTROLS
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_SELECT, false) shouldBe PlaybackInputAction.PREVIOUS_ITEM
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_START, false) shouldBe PlaybackInputAction.NEXT_ITEM
	}

	test("A toggles playback only while the controls are hidden") {
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_A, false) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_A, true) shouldBe null
	}

	test("keys handled elsewhere or unrelated are not mapped") {
		val unmapped = listOf(
			KeyEvent.KEYCODE_BUTTON_B,
			KeyEvent.KEYCODE_BUTTON_L1,
			KeyEvent.KEYCODE_BUTTON_R1,
			KeyEvent.KEYCODE_DPAD_LEFT,
			KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
			KeyEvent.KEYCODE_MENU,
			KeyEvent.KEYCODE_BUTTON_THUMBL,
			KeyEvent.KEYCODE_BUTTON_THUMBR,
		)
		for (key in unmapped) {
			GamepadPlaybackInput.actionForKey(key, false) shouldBe null
		}
	}

	context("stick seek repeater") {
		test("inside the dead zone nothing happens") {
			val r = StickSeekRepeater()
			r.onAxes(0.5f, 0f, 0f, 0) shouldBe StickSeekRepeater.Result(false, null)
			r.onAxes(-0.59f, 0f, 0f, 10) shouldBe StickSeekRepeater.Result(false, null)
			r.isEngaged shouldBe false
		}

		test("pushing right steps forward once and consumes the event") {
			val r = StickSeekRepeater()
			r.onAxes(0.9f, 0f, 0f, 0) shouldBe StickSeekRepeater.Result(true, PlaybackInputAction.SEEK_FORWARD)
			r.isEngaged shouldBe true
			r.onAxes(0.95f, 0f, 0f, 20) shouldBe StickSeekRepeater.Result(true, null)
		}

		test("pushing left steps backward") {
			StickSeekRepeater().onAxes(-1f, 0f, 0f, 0).action shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("hat axis works like the stick") {
			StickSeekRepeater().onAxes(0f, 1f, 0f, 0).action shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("mostly vertical pushes are ignored") {
			StickSeekRepeater().onAxes(0.7f, 0f, -0.9f, 0) shouldBe StickSeekRepeater.Result(false, null)
		}

		test("holding repeats after the initial delay at the repeat interval") {
			val r = StickSeekRepeater(initialDelayMs = 450, repeatMs = 300)
			r.onAxes(1f, 0f, 0f, 0)
			r.tick(100) shouldBe null
			r.tick(449) shouldBe null
			r.tick(450) shouldBe PlaybackInputAction.SEEK_FORWARD
			r.tick(600) shouldBe null
			r.tick(749) shouldBe null
			r.tick(750) shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("jitter between release and engage thresholds does not re-trigger or release") {
			val r = StickSeekRepeater()
			r.onAxes(1f, 0f, 0f, 0)
			r.onAxes(0.4f, 0f, 0f, 10) shouldBe StickSeekRepeater.Result(true, null)
			r.isEngaged shouldBe true
			r.onAxes(0.7f, 0f, 0f, 20) shouldBe StickSeekRepeater.Result(true, null)
		}

		test("releasing the stick stops repeats and re-arms") {
			val r = StickSeekRepeater()
			r.onAxes(1f, 0f, 0f, 0)
			r.onAxes(0f, 0f, 0f, 100) shouldBe StickSeekRepeater.Result(false, null)
			r.isEngaged shouldBe false
			r.tick(10_000) shouldBe null
			r.onAxes(1f, 0f, 0f, 10_100).action shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("reversing direction steps immediately") {
			val r = StickSeekRepeater()
			r.onAxes(1f, 0f, 0f, 0)
			r.onAxes(-1f, 0f, 0f, 50) shouldBe StickSeekRepeater.Result(true, PlaybackInputAction.SEEK_BACKWARD)
			r.tick(500) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("reset disengages") {
			val r = StickSeekRepeater()
			r.onAxes(1f, 0f, 0f, 0)
			r.reset()
			r.isEngaged shouldBe false
			r.tick(10_000) shouldBe null
		}
	}

	context("seek bar d-pad skip") {
		test("left and right on the focused seek bar skip") {
			SeekBarDpadSkip.actionFor(KeyEvent.KEYCODE_DPAD_RIGHT, seekBarFocused = true) shouldBe PlaybackInputAction.SEEK_FORWARD
			SeekBarDpadSkip.actionFor(KeyEvent.KEYCODE_DPAD_LEFT, seekBarFocused = true) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("other focus or other keys are left alone") {
			SeekBarDpadSkip.actionFor(KeyEvent.KEYCODE_DPAD_RIGHT, seekBarFocused = false) shouldBe null
			SeekBarDpadSkip.actionFor(KeyEvent.KEYCODE_DPAD_CENTER, seekBarFocused = true) shouldBe null
			SeekBarDpadSkip.actionFor(KeyEvent.KEYCODE_DPAD_UP, seekBarFocused = true) shouldBe null
		}
	}

	context("d-pad with the controls hidden") {
		fun action(key: Int, hidden: Boolean = true, playing: Boolean = true, live: Boolean = false, other: Boolean = false) =
			HiddenOverlayDpad.actionFor(key, hidden, playing, live, other)

		test("left and right skip while playing") {
			action(KeyEvent.KEYCODE_DPAD_LEFT) shouldBe HiddenDpadAction.SKIP_BACKWARD
			action(KeyEvent.KEYCODE_DPAD_RIGHT) shouldBe HiddenDpadAction.SKIP_FORWARD
		}

		test("up and down are swallowed so they do not show the controls") {
			action(KeyEvent.KEYCODE_DPAD_UP) shouldBe HiddenDpadAction.SWALLOW
			action(KeyEvent.KEYCODE_DPAD_DOWN) shouldBe HiddenDpadAction.SWALLOW
		}

		test("the centre key only shows the controls") {
			action(KeyEvent.KEYCODE_DPAD_CENTER) shouldBe HiddenDpadAction.SHOW_CONTROLS
			action(KeyEvent.KEYCODE_ENTER) shouldBe HiddenDpadAction.SHOW_CONTROLS
		}

		test("the centre key on a paused video keeps its normal behaviour") {
			action(KeyEvent.KEYCODE_DPAD_CENTER, playing = false) shouldBe HiddenDpadAction.NONE
		}

		test("paused video skips and swallows the same way but centre keeps resuming") {
			action(KeyEvent.KEYCODE_DPAD_RIGHT, playing = false) shouldBe HiddenDpadAction.SKIP_FORWARD
			action(KeyEvent.KEYCODE_DPAD_LEFT, playing = false) shouldBe HiddenDpadAction.SKIP_BACKWARD
			action(KeyEvent.KEYCODE_DPAD_DOWN, playing = false) shouldBe HiddenDpadAction.SWALLOW
		}

		test("visible controls, live TV and other UI keep the normal behaviour") {
			action(KeyEvent.KEYCODE_DPAD_RIGHT, hidden = false) shouldBe HiddenDpadAction.NONE
			action(KeyEvent.KEYCODE_DPAD_RIGHT, live = true) shouldBe HiddenDpadAction.NONE
			action(KeyEvent.KEYCODE_DPAD_UP, other = true) shouldBe HiddenDpadAction.NONE
		}
	}
})
