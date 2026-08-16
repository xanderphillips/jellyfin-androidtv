package org.jellyfin.androidtv.ui.itemdetail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import timber.log.Timber
import java.util.UUID

/**
 * Deletes a library item by id. Shared by the full item-detail view and the
 * episode-list quick-delete action so both go through one code path.
 */
suspend fun deleteLibraryItem(api: ApiClient, itemId: UUID, itemName: String?): Boolean {
	Timber.i("Deleting item $itemName (id=$itemId)")

	return try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(itemId)
		}
		true
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item $itemName (id=$itemId)")
		false
	}
}
