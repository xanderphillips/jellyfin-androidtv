package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RemoteSkipPlannerTest : FunSpec({
	test("the first press goes out immediately") {
		val p = RemoteSkipPlanner(minSeekGapMs = 500)
		p.onPress(30_000, isRepeat = false, nowMs = 1_000) shouldBe true
		p.next(1_000, busy = false) shouldBe 30_000
		p.hasPending shouldBe false
	}

	test("presses inside the minimum gap add up into the next seek") {
		val p = RemoteSkipPlanner(minSeekGapMs = 500)
		p.onPress(30_000, false, 0)
		p.next(0, false) shouldBe 30_000
		p.onPress(30_000, false, 100)
		p.next(100, false) shouldBe null
		p.onPress(30_000, false, 300)
		p.next(400, false) shouldBe null
		p.next(500, false) shouldBe 60_000
	}

	test("nothing goes out while a seek is running, then everything does") {
		val p = RemoteSkipPlanner()
		p.onPress(-10_000, false, 0)
		p.onPress(-10_000, false, 50)
		p.next(2_000, busy = true) shouldBe null
		p.hasPending shouldBe true
		p.next(2_100, busy = false) shouldBe -20_000
	}

	test("forward and back presses cancel out") {
		val p = RemoteSkipPlanner()
		p.onPress(30_000, false, 0)
		p.onPress(-10_000, false, 10)
		p.next(10, false) shouldBe 20_000
	}

	test("held key repeats are thinned to the repeat interval") {
		val p = RemoteSkipPlanner(repeatIntervalMs = 300)
		p.onPress(30_000, false, 0) shouldBe true
		p.onPress(30_000, true, 50) shouldBe false
		p.onPress(30_000, true, 250) shouldBe false
		p.onPress(30_000, true, 300) shouldBe true
	}

	test("a fresh press is never thinned") {
		val p = RemoteSkipPlanner(repeatIntervalMs = 300)
		p.onPress(30_000, false, 0)
		p.onPress(30_000, false, 10) shouldBe true
	}

	test("a refused skip is restored and can be sent again at once") {
		val p = RemoteSkipPlanner(minSeekGapMs = 500)
		p.onPress(30_000, false, 0)
		p.next(0, false) shouldBe 30_000
		p.restore(30_000)
		p.hasPending shouldBe true
		p.next(10, false) shouldBe 30_000
	}

	test("clear drops anything waiting") {
		val p = RemoteSkipPlanner()
		p.onPress(30_000, false, 0)
		p.clear()
		p.next(5_000, false) shouldBe null
	}
})
