package org.jellyfin.androidtv.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal subset of the GitHub "get the latest release" API response.
 * https://docs.github.com/en/rest/releases/releases#get-the-latest-release
 */
@Serializable
data class GitHubRelease(
	@SerialName("tag_name") val tagName: String,
	@SerialName("html_url") val htmlUrl: String,
	val assets: List<GitHubReleaseAsset> = emptyList(),
) {
	/**
	 * Pick the APK asset to offer for install. Prefers an asset explicitly named for the
	 * release-signed build; falls back to the first ".apk" asset found.
	 */
	fun findApkAsset(): GitHubReleaseAsset? =
		assets.firstOrNull { it.name.endsWith("-release.apk") }
			?: assets.firstOrNull { it.name.endsWith(".apk") }
}

@Serializable
data class GitHubReleaseAsset(
	val name: String,
	@SerialName("browser_download_url") val browserDownloadUrl: String,
)
