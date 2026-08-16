package org.jellyfin.androidtv.ui.shared.toolbar

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.FolderStorageDto
import org.jellyfin.sdk.model.api.LibraryStorageDto
import java.util.UUID

// FolderStorageDto's freeSpace/usedSpace are non-nullable Long in the SDK (org.jellyfin.sdk:jellyfin-model-jvm:1.8.12),
// not Long? as originally assumed; deviceId and storageType are the only nullable fields, both defaulting to null.
private fun folder(
	path: String = "/media",
	deviceId: String? = null,
	freeSpace: Long = 0,
	usedSpace: Long = 0,
) = FolderStorageDto(path = path, freeSpace = freeSpace, usedSpace = usedSpace, deviceId = deviceId)

// LibraryStorageDto.id is a non-nullable UUID in the SDK, not String as originally assumed.
private fun library(folders: List<FolderStorageDto>) = LibraryStorageDto(id = UUID.randomUUID(), name = "Library", folders = folders)

class LibraryStorageAggregatorTest : FunSpec({
	test("returns empty result for null, or empty libraries") {
		val empty = AggregateStorage(freeSpace = 0, usedSpace = 0, totalSpace = 0, usedPercentage = 0.0, hasData = false)
		aggregateLibraryStorage(null) shouldBe empty
		aggregateLibraryStorage(emptyList()) shouldBe empty
	}

	test("aggregates a single folder directly") {
		val libraries = listOf(library(listOf(folder(path = "/movies", deviceId = "dev1", freeSpace = 100, usedSpace = 300))))
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 100
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 400
		result.usedPercentage shouldBe 75.0
		result.hasData shouldBe true
	}

	test("deduplicates folders with identical free/used space across libraries, even with matching deviceIds") {
		val libraries = listOf(
			library(listOf(folder(path = "/movies", deviceId = "dev1", freeSpace = 100, usedSpace = 300))),
			library(listOf(folder(path = "/tv", deviceId = "dev1", freeSpace = 100, usedSpace = 300))),
		)
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 100
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 400
	}

	test("deduplicates folders with identical free/used space even when deviceId differs between them") {
		val libraries = listOf(
			library(listOf(folder(path = "/movies", deviceId = "mount-a", freeSpace = 100, usedSpace = 300))),
			library(listOf(folder(path = "/tv", deviceId = "mount-b", freeSpace = 100, usedSpace = 300))),
		)
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 100
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 400
	}

	test("sums folders whose free/used space genuinely differs") {
		val libraries = listOf(
			library(listOf(folder(path = "/movies", freeSpace = 100, usedSpace = 300))),
			library(listOf(folder(path = "/tv", freeSpace = 50, usedSpace = 150))),
		)
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 150
		result.usedSpace shouldBe 450
		result.totalSpace shouldBe 600
	}

	test("deduplicates via the free/used space fallback when deviceId is missing on both") {
		val libraries = listOf(
			library(listOf(folder(path = "/media", freeSpace = 100, usedSpace = 300))),
			library(listOf(folder(path = "/media", freeSpace = 100, usedSpace = 300))),
		)
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 100
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 400
	}

	test("does not double-count Docker bind-mount libraries sharing one disk under distinct paths/deviceIds") {
		fun sameDiskFolder(path: String, deviceId: String) =
			folder(path = path, deviceId = deviceId, freeSpace = 2_300_000_000_000, usedSpace = 1_400_000_000_000)

		val libraries = listOf(
			library(listOf(sameDiskFolder("/data/movies", "mount-1"))),
			library(listOf(sameDiskFolder("/data/tvshows", "mount-2"))),
			library(listOf(sameDiskFolder("/data/music", "mount-3"))),
		)
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 2_300_000_000_000
		result.usedSpace shouldBe 1_400_000_000_000
		result.totalSpace shouldBe 3_700_000_000_000
	}

	// The original TS test also covered a "missing" (undefined) usedSpace case, but FolderStorageDto.usedSpace
	// is a non-nullable Long in the SDK, so only the negative-usedSpace exclusion is representable here.
	test("excludes folders with negative usedSpace without throwing") {
		val libraries = listOf(library(listOf(
			folder(path = "/movies", deviceId = "dev1", freeSpace = 100, usedSpace = 300),
			folder(path = "/negative", deviceId = "dev3", freeSpace = 10, usedSpace = -1),
		)))
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 100
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 400
	}

	test("clamps negative freeSpace to 0 on an otherwise valid folder") {
		val libraries = listOf(library(listOf(folder(path = "/movies", deviceId = "dev1", freeSpace = -5, usedSpace = 300))))
		val result = aggregateLibraryStorage(libraries)
		result.freeSpace shouldBe 0
		result.usedSpace shouldBe 300
		result.totalSpace shouldBe 300
		result.usedPercentage shouldBe 100.0
	}

	test("does not throw for a library with an empty folders list") {
		val result = aggregateLibraryStorage(listOf(library(emptyList())))
		result shouldBe AggregateStorage(freeSpace = 0, usedSpace = 0, totalSpace = 0, usedPercentage = 0.0, hasData = false)
	}

	test("clamps usedPercentage to 100") {
		val libraries = listOf(library(listOf(folder(path = "/movies", deviceId = "dev1", freeSpace = 0, usedSpace = 300))))
		aggregateLibraryStorage(libraries).usedPercentage shouldBe 100.0
	}
})
