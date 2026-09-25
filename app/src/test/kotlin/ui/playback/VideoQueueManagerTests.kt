package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class VideoQueueManagerTests : FunSpec({
	test("subtitle selection only applies to the selected item") {
		val manager = VideoQueueManager()
		val item = UUID.randomUUID()
		manager.setSubtitleSelection(item, -1)
		manager.getSubtitleSelection(item) shouldBe -1
		manager.getSubtitleSelection(UUID.randomUUID()) shouldBe null
	}

	test("clearing the queue clears the subtitle selection") {
		val manager = VideoQueueManager()
		val item = UUID.randomUUID()
		manager.setSubtitleSelection(item, 3)
		manager.clearVideoQueue()
		manager.getSubtitleSelection(item) shouldBe null
	}
})
