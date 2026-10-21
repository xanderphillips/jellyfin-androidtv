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

		test("a held stick gives one step, then repeats after the hold delay at the repeat interval") {
			val m = ScrollSeekMapper(holdDelayMs = 600, repeatMs = 450)
			m.onScroll(-0.25f, 0) shouldBe PlaybackInputAction.SEEK_BACKWARD
			// events arrive about every 16 ms while held
			for (t in 16L..592L step 16) m.onScroll(-0.25f, t) shouldBe null
			m.onScroll(-0.25f, 608) shouldBe PlaybackInputAction.SEEK_BACKWARD
			// the repeat at 608 schedules the next one for 608 + 450 = 1058, so 1056 is still too early
			for (t in 624L..1056L step 16) m.onScroll(-0.25f, t) shouldBe null
			m.onScroll(-0.25f, 1_072) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("a pause between pushes starts a new push with an immediate step") {
			val m = ScrollSeekMapper(pushGapMs = 300)
			m.onScroll(0.25f, 0) shouldBe PlaybackInputAction.SEEK_FORWARD
			m.onScroll(0.25f, 100) shouldBe null
			m.onScroll(0.25f, 500) shouldBe PlaybackInputAction.SEEK_FORWARD
		}

		test("stray opposite events inside a push are ignored") {
			val m = ScrollSeekMapper(flipConfirmCount = 3)
			m.onScroll(0.25f, 0) shouldBe PlaybackInputAction.SEEK_FORWARD
			m.onScroll(-0.2f, 16) shouldBe null
			m.onScroll(-0.2f, 32) shouldBe null
			m.onScroll(0.25f, 48) shouldBe null
			m.onScroll(-0.2f, 64) shouldBe null
			m.onScroll(-0.2f, 80) shouldBe null
		}

		test("enough opposite events in a row reverse direction immediately") {
			val m = ScrollSeekMapper(flipConfirmCount = 3)
			m.onScroll(0.25f, 0)
			m.onScroll(-0.2f, 16) shouldBe null
			m.onScroll(-0.2f, 32) shouldBe null
			m.onScroll(-0.2f, 48) shouldBe PlaybackInputAction.SEEK_BACKWARD
		}

		test("dead zone values do not break up a push") {
			val m = ScrollSeekMapper(pushGapMs = 300)
			m.onScroll(0.25f, 0) shouldBe PlaybackInputAction.SEEK_FORWARD
			m.onScroll(0.02f, 100) shouldBe null
			m.onScroll(0.25f, 200) shouldBe null
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

	context("container versus control size") {
		test("screen-sized views are containers and small views are controls") {
			PointerPlayback.isContainerSized(1920, 1080, 1920, 1080) shouldBe true
			PointerPlayback.isContainerSized(1600, 900, 1920, 1080) shouldBe true
			PointerPlayback.isContainerSized(96, 96, 1920, 1080) shouldBe false
			PointerPlayback.isContainerSized(1920, 200, 1920, 1080) shouldBe false
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

	context("skip accumulator") {
		test("nothing is due before any click or while clicks are still arriving") {
			val a = SkipAccumulator(quietMs = 400)
			a.takeIfQuiet(10_000) shouldBe null
			a.add(30_000, 1_000)
			a.takeIfQuiet(1_200) shouldBe null
			a.hasPending shouldBe true
		}

		test("rapid clicks accumulate into one skip after the quiet period") {
			val a = SkipAccumulator(quietMs = 400)
			a.add(30_000, 1_000)
			a.add(30_000, 1_300)
			a.add(30_000, 1_600)
			a.takeIfQuiet(1_900) shouldBe null
			a.takeIfQuiet(2_000) shouldBe 90_000
			a.hasPending shouldBe false
			a.takeIfQuiet(5_000) shouldBe null
		}

		test("forward and back clicks cancel out") {
			val a = SkipAccumulator(quietMs = 400)
			a.add(30_000, 0)
			a.add(-10_000, 100)
			a.takeIfQuiet(600) shouldBe 20_000
		}

		test("a refused skip can be put back and is due again") {
			val a = SkipAccumulator(quietMs = 400)
			a.add(30_000, 0)
			a.takeIfQuiet(500) shouldBe 30_000
			a.restore(30_000)
			a.hasPending shouldBe true
			a.takeIfQuiet(600) shouldBe 30_000
		}
	}
})
