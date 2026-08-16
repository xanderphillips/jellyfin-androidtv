package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.operations.LibraryApi
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.UUID

class LibraryItemDeletionTest : FunSpec({
	val itemId = UUID.randomUUID()

	beforeEach {
		mockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	afterEach {
		unmockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	test("returns true when the API call succeeds") {
		val api = mockk<ApiClient>()
		val libraryApi = mockk<LibraryApi>()
		every { api.libraryApi } returns libraryApi
		coEvery { libraryApi.deleteItem(itemId) } returns mockk()

		runTest {
			val result = deleteLibraryItem(api, itemId, "Test Episode")
			result shouldBe true
		}
	}

	test("returns false when the API call throws ApiClientException") {
		val api = mockk<ApiClient>()
		val libraryApi = mockk<LibraryApi>()
		every { api.libraryApi } returns libraryApi
		coEvery { libraryApi.deleteItem(itemId) } throws ApiClientException("failed", null)

		runTest {
			val result = deleteLibraryItem(api, itemId, "Test Episode")
			result shouldBe false
		}
	}
})
