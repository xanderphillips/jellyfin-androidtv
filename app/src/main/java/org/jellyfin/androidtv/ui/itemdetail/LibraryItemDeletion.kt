package org.jellyfin.androidtv.ui.itemdetail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import timber.log.Timber
import java.util.UUID

/**
 * Deletes a library item by id and records it as the last deleted item so surfaces
 * like the home rows can prune it. Shared by the full item-detail view and the
 * quick-delete actions so all of them go through one code path.
 */
suspend fun deleteLibraryItem(
	api: ApiClient,
	dataRefreshService: DataRefreshService,
	itemId: UUID,
	itemName: String?,
): Boolean {
	Timber.i("Deleting item $itemName (id=$itemId)")

	return try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(itemId)
		}
		dataRefreshService.lastDeletedItemId = itemId
		true
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item $itemName (id=$itemId)")
		false
	}
}
