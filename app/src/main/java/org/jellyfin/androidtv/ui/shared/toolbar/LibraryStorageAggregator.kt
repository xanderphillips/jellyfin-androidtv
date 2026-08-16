package org.jellyfin.androidtv.ui.shared.toolbar

import org.jellyfin.sdk.model.api.LibraryStorageDto

data class AggregateStorage(
	val freeSpace: Long,
	val usedSpace: Long,
	val totalSpace: Long,
	val usedPercentage: Double,
	val hasData: Boolean,
)

private val EMPTY_STORAGE = AggregateStorage(
	freeSpace = 0,
	usedSpace = 0,
	totalSpace = 0,
	usedPercentage = 0.0,
	hasData = false,
)

/**
 * Aggregates free/used space across all folders backing the given libraries,
 * deduplicating folders that share an underlying physical storage device
 * (keyed on the free/used space pair, not deviceId or path — see
 * jellyfin-web's aggregateLibraryStorage.ts for why deviceId is unreliable
 * across Docker bind-mounts of one host disk into several container paths).
 */
fun aggregateLibraryStorage(libraries: List<LibraryStorageDto>?): AggregateStorage {
	if (libraries.isNullOrEmpty()) return EMPTY_STORAGE

	val seenKeys = mutableSetOf<String>()
	var freeSpace = 0L
	var usedSpace = 0L

	for (library in libraries) {
		for (folder in library.folders) {
			val folderUsedSpace = folder.usedSpace
			if (folderUsedSpace < 0) continue

			val folderFreeSpace = folder.freeSpace.coerceAtLeast(0)
			val key = "$folderFreeSpace:$folderUsedSpace"
			if (!seenKeys.add(key)) continue

			freeSpace += folderFreeSpace
			usedSpace += folderUsedSpace
		}
	}

	val totalSpace = freeSpace + usedSpace

	return AggregateStorage(
		freeSpace = freeSpace,
		usedSpace = usedSpace,
		totalSpace = totalSpace,
		usedPercentage = if (totalSpace > 0) (usedSpace.toDouble() / totalSpace * 100).coerceAtMost(100.0) else 0.0,
		hasData = totalSpace > 0,
	)
}
