package org.jellyfin.androidtv.update

sealed interface UpdateCheckResult {
	data class UpdateAvailable(
		val version: AppVersion,
		val tagName: String,
		val releaseUrl: String,
		val apkDownloadUrl: String,
		val apkAssetName: String,
	) : UpdateCheckResult

	data object UpToDate : UpdateCheckResult

	/** Network failure, unparseable response, or no APK asset attached to the release. */
	data object Unknown : UpdateCheckResult
}

class UpdateChecker(
	private val api: GitHubReleaseApi,
) {
	suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
		val currentVersion = AppVersion.parseOrNull(currentVersionName) ?: return UpdateCheckResult.Unknown
		val release = api.getLatestRelease() ?: return UpdateCheckResult.Unknown

		return evaluate(currentVersion, release)
	}

	/**
	 * Pure decision function, split out from [checkForUpdate] so the comparison/asset-picking
	 * logic can be unit tested without mocking network calls.
	 */
	fun evaluate(currentVersion: AppVersion, release: GitHubRelease): UpdateCheckResult {
		val releaseVersion = AppVersion.parseOrNull(release.tagName) ?: return UpdateCheckResult.Unknown

		if (releaseVersion <= currentVersion) return UpdateCheckResult.UpToDate

		val apkAsset = release.findApkAsset() ?: return UpdateCheckResult.Unknown

		return UpdateCheckResult.UpdateAvailable(
			version = releaseVersion,
			tagName = release.tagName,
			releaseUrl = release.htmlUrl,
			apkDownloadUrl = apkAsset.browserDownloadUrl,
			apkAssetName = apkAsset.name,
		)
	}
}
