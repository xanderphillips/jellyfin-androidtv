package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SeekPlayIntentTest : FunSpec({
	test("a seek while playing resumes playing") {
		SeekPlayIntent().begin(playingNow = true) shouldBe true
	}

	test("a seek while paused stays paused") {
		SeekPlayIntent().begin(playingNow = false) shouldBe false
	}

	test("a second seek before the first finished keeps the original intent") {
		val playing = SeekPlayIntent()
		playing.begin(true) shouldBe true
		// the first seek left the player buffering, so it no longer looks like it is playing
		playing.begin(false) shouldBe true

		val paused = SeekPlayIntent()
		paused.begin(false) shouldBe false
		paused.begin(true) shouldBe false
	}

	test("settling starts a fresh series") {
		val i = SeekPlayIntent()
		i.begin(true)
		i.inFlight shouldBe true
		i.settle()
		i.inFlight shouldBe false
		i.begin(false) shouldBe false
	}

	test("a retried seek after a refused one keeps the intent") {
		val i = SeekPlayIntent()
		i.begin(true) shouldBe true
		i.begin(true) shouldBe true
	}

	test("only an explicit pause, idle or error means playback is not wanted") {
		SeekPlayIntent.wantsPlayback(isPaused = false, isIdleOrError = false) shouldBe true
		SeekPlayIntent.wantsPlayback(isPaused = true, isIdleOrError = false) shouldBe false
		SeekPlayIntent.wantsPlayback(isPaused = false, isIdleOrError = true) shouldBe false
	}
})
