package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PointerPlaybackInputTest : FunSpec({
	context("scroll seek mapper") {
		test("positive scroll skips forward and negative skips back") {
			ScrollSeekMapper().onScroll(0.25f, 0) shouldBe PlaybackInputAction.SEEK_FORWARD
			ScrollSeekMapper().onScroll(-0.25f, 0) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("observed Quest magnitudes are accepted and tiny values are ignored") {
			ScrollSeekMapper().onScroll(0.22f, 0) shouldBe PlaybackInputAction.SEEK_FORWARD
			ScrollSeekMapper().onScroll(-0.27f, 0) shouldBe PlaybackInputAction.SEEK_BACKWARD
			ScrollSeekMapper().onScroll(0.04f, 0) shouldBe null
			ScrollSeekMapper().onScroll(-0.09f, 0) shouldBe null
			ScrollSeekMapper().onScroll(0f, 0) shouldBe null
		}

		test("steps are rate limited") {
			val m = ScrollSeekMapper(minIntervalMs = 275)
			m.onScroll(0.25f, 1_000) shouldBe PlaybackInputAction.SEEK_FORWARD
			m.onScroll(0.25f, 1_100) shouldBe null
			m.onScroll(0.25f, 1_274) shouldBe null
			m.onScroll(0.25f, 1_275) shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("ignored events do not consume the rate limit window") {
			val m = ScrollSeekMapper(minIntervalMs = 275)
			m.onScroll(0.25f, 1_000)
			m.onScroll(0.01f, 1_100) shouldBe null
			m.onScroll(-0.25f, 1_300) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("reset allows an immediate step") {
			val m = ScrollSeekMapper()
			m.onScroll(0.25f, 0)
			m.reset()
			m.onScroll(0.25f, 10) shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("only mostly horizontal pushes count") {
			val m = ScrollSeekMapper()
			m.isHorizontalPush(0.25f, 0f) shouldBe true
			m.isHorizontalPush(-0.25f, 0.1f) shouldBe true
			m.isHorizontalPush(0.25f, -0.3f) shouldBe false
			m.isHorizontalPush(0.05f, 0f) shouldBe false
		}
	}

	context("tap to toggle controls") {
		test("a tap on the video toggles") {
			PointerPlayback.shouldToggleControlsOnTap(isTap = true, hitControl = false, popupOrGuideVisible = false) shouldBe true
		}

		test("a tap on a control, a drag, or an open popup does not toggle") {
			PointerPlayback.shouldToggleControlsOnTap(true, true, false) shouldBe false
			PointerPlayback.shouldToggleControlsOnTap(false, false, false) shouldBe false
			PointerPlayback.shouldToggleControlsOnTap(true, false, true) shouldBe false
		}

		test("tap detection uses slop and duration") {
			PointerPlayback.isTap(3f, -4f, 120, slopPx = 8f) shouldBe true
			PointerPlayback.isTap(30f, 0f, 120, slopPx = 8f) shouldBe false
			PointerPlayback.isTap(0f, 0f, 900, slopPx = 8f) shouldBe false
		}
	}

	context("seek bar position") {
		test("maps x to a position and clamps to the bar") {
			PointerPlayback.seekPositionMs(300f, 100f, 400f, 10_000) shouldBe 5_000
			PointerPlayback.seekPositionMs(100f, 100f, 400f, 10_000) shouldBe 0
			PointerPlayback.seekPositionMs(50f, 100f, 400f, 10_000) shouldBe 0
			PointerPlayback.seekPositionMs(900f, 100f, 400f, 10_000) shouldBe 10_000
		}

		test("invalid bar or duration gives 0") {
			PointerPlayback.seekPositionMs(300f, 100f, 0f, 10_000) shouldBe 0
			PointerPlayback.seekPositionMs(300f, 100f, 400f, 0) shouldBe 0
		}
	}
})
