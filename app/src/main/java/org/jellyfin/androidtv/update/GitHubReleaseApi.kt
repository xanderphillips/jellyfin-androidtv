package org.jellyfin.androidtv.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

interface GitHubReleaseApi {
	/**
	 * Fetch the latest published (non-draft, non-prerelease) release, or null if it could
	 * not be retrieved (network error, non-200 response, or unparseable body).
	 */
	suspend fun getLatestRelease(): GitHubRelease?
}

/**
 * Talks to the public, unauthenticated GitHub REST API. No token is used since this only
 * reads public release metadata for a public repository, well within the unauthenticated
 * rate limit (60 requests/hour) for a check-on-launch usage pattern.
 */
class HttpGitHubReleaseApi(
	private val repositoryOwner: String = "xanderphillips",
	private val repositoryName: String = "jellyfin-androidtv",
) : GitHubReleaseApi {
	private val json = Json { ignoreUnknownKeys = true }

	override suspend fun getLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
		runCatching {
			val url = URL("https://api.github.com/repos/$repositoryOwner/$repositoryName/releases/latest")
			val connection = url.openConnection() as HttpURLConnection

			try {
				connection.requestMethod = "GET"
				connection.setRequestProperty("Accept", "application/vnd.github+json")
				connection.connectTimeout = CONNECT_TIMEOUT_MS
				connection.readTimeout = READ_TIMEOUT_MS

				if (connection.responseCode != HttpURLConnection.HTTP_OK) {
					Timber.w("Update check failed with HTTP ${connection.responseCode}")
					return@runCatching null
				}

				val body = connection.inputStream.bufferedReader().use { it.readText() }
				json.decodeFromString(GitHubRelease.serializer(), body)
			} finally {
				connection.disconnect()
			}
		}.onFailure { error ->
			Timber.w(error, "Failed to check for updates")
		}.getOrNull()
	}

	companion object {
		private const val CONNECT_TIMEOUT_MS = 10_000
		private const val READ_TIMEOUT_MS = 10_000
	}
}
