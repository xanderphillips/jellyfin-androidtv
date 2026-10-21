package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LatestSeekQueueTest : FunSpec({
	test("a seek requested while idle is taken at once") {
		val q = LatestSeekQueue()
		q.set(5_000)
		q.take(busy = false) shouldBe 5_000
		q.hasPending shouldBe false
	}

	test("nothing is taken while busy and the latest target wins") {
		val q = LatestSeekQueue()
		q.set(5_000)
		q.set(9_000)
		q.take(busy = true) shouldBe null
		q.hasPending shouldBe true
		q.take(busy = false) shouldBe 9_000
	}

	test("an empty queue gives nothing") {
		LatestSeekQueue().take(busy = false) shouldBe null
	}

	test("clear drops the waiting target") {
		val q = LatestSeekQueue()
		q.set(5_000)
		q.clear()
		q.take(false) shouldBe null
	}
})
