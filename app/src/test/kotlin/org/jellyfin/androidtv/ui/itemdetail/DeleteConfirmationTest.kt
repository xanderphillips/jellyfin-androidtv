package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

private fun item(type: BaseItemKind, name: String? = "Name", block: (BaseItemDto) -> BaseItemDto = { it }) =
	block(BaseItemDto(id = UUID.randomUUID(), type = type, name = name))

class DeleteConfirmationTest : FunSpec({
	test("series names the series and uses the episode count when known") {
		val c = item(BaseItemKind.SERIES, "Show") { it.copy(recursiveItemCount = 42) }.toDeleteConfirmation()
		c shouldBe DeleteConfirmation(DeleteTargetKind.SERIES, "Show", "Series", 42)
	}

	test("series without a count omits it") {
		item(BaseItemKind.SERIES, "Show").toDeleteConfirmation().count shouldBe null
		item(BaseItemKind.SERIES, "Show") { it.copy(recursiveItemCount = 0) }.toDeleteConfirmation().count shouldBe null
	}

	test("season includes series name and falls back to child count") {
		val c = item(BaseItemKind.SEASON, "Season 2") { it.copy(seriesName = "Show", childCount = 8) }.toDeleteConfirmation()
		c shouldBe DeleteConfirmation(DeleteTargetKind.SEASON, "Show - Season 2", "Season", 8)
	}

	test("season prefers recursive item count") {
		item(BaseItemKind.SEASON, "Season 2") { it.copy(recursiveItemCount = 10, childCount = 8) }.toDeleteConfirmation().count shouldBe 10
	}

	test("episode label has season and episode code") {
		val c = item(BaseItemKind.EPISODE, "Title") { it.copy(parentIndexNumber = 2, indexNumber = 8) }.toDeleteConfirmation()
		c shouldBe DeleteConfirmation(DeleteTargetKind.EPISODE, "S02E08 - Title", "Episode", null)
	}

	test("episode without numbers is just the title") {
		item(BaseItemKind.EPISODE, "Title").toDeleteConfirmation().label shouldBe "Title"
	}

	test("movie names the movie") {
		item(BaseItemKind.MOVIE, "Film").toDeleteConfirmation() shouldBe DeleteConfirmation(DeleteTargetKind.MOVIE, "Film", "Movie", null)
	}

	test("other types carry their type name") {
		val c = item(BaseItemKind.BOX_SET, "Collection").toDeleteConfirmation()
		c.kind shouldBe DeleteTargetKind.OTHER
		c.typeName shouldBe BaseItemKind.BOX_SET.serialName
	}

	test("missing name does not produce a blank label") {
		item(BaseItemKind.MOVIE, null).toDeleteConfirmation().label shouldBe "?"
	}
})
