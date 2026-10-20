package org.jellyfin.androidtv.ui.playback

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GamepadPlaybackInputTest : FunSpec({
	test("button mapping") {
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_X, false) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_X, true) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_Y, false) shouldBe PlaybackInputAction.TOGGLE_CONTROLS
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_THUMBL, false) shouldBe PlaybackInputAction.PREVIOUS_ITEM
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_THUMBR, false) shouldBe PlaybackInputAction.NEXT_ITEM
	}

	test("A toggles playback only while the controls are hidden") {
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_A, false) shouldBe PlaybackInputAction.PLAY_PAUSE
		GamepadPlaybackInput.actionForKey(KeyEvent.KEYCODE_BUTTON_A, true) shouldBe null
	}

	test("keys handled elsewhere or unrelated are not mapped") {
		for (key in listOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
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
})
