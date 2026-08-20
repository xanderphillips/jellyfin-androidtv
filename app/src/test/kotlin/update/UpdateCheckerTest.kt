package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

class UpdateCheckerTest : FunSpec({
	val currentVersion = AppVersion(0, 19, 0, 959)

	fun release(tagName: String, assets: List<GitHubReleaseAsset> = emptyList()) = GitHubRelease(
		tagName = tagName,
		htmlUrl = "https://github.com/xanderphillips/jellyfin-androidtv/releases/tag/$tagName",
		assets = assets,
	)

	val releaseApkAsset = GitHubReleaseAsset(
		name = "jellyfin-androidtv-v0.20.0-release.apk",
		browserDownloadUrl = "https://example.com/jellyfin-androidtv-v0.20.0-release.apk",
	)

	test("reports UpdateAvailable when the release is newer and has an APK asset") {
		val checker = UpdateChecker(mockk())

		val result = checker.evaluate(currentVersion, release("v0.20.0", listOf(releaseApkAsset)))

		result.shouldBeInstanceOf<UpdateCheckResult.UpdateAvailable>()
		result as UpdateCheckResult.UpdateAvailable
		result.version shouldBe AppVersion(0, 20, 0, null)
		result.apkDownloadUrl shouldBe releaseApkAsset.browserDownloadUrl
		result.apkAssetName shouldBe releaseApkAsset.name
	}

	test("prefers a *-release.apk asset over other apk assets") {
		val checker = UpdateChecker(mockk())
		val debugAsset = GitHubReleaseAsset("jellyfin-androidtv-v0.20.0-debug.apk", "https://example.com/debug.apk")

		val result = checker.evaluate(currentVersion, release("v0.20.0", listOf(debugAsset, releaseApkAsset)))

		result.shouldBeInstanceOf<UpdateCheckResult.UpdateAvailable>()
		(result as UpdateCheckResult.UpdateAvailable).apkAssetName shouldBe releaseApkAsset.name
	}

	test("reports UpToDate when the release is the same version") {
		val checker = UpdateChecker(mockk())

		checker.evaluate(currentVersion, release("v0.19.0-dev.959")) shouldBe UpdateCheckResult.UpToDate
	}

	test("reports UpToDate when the release is older") {
		val checker = UpdateChecker(mockk())

		checker.evaluate(currentVersion, release("v0.10.0")) shouldBe UpdateCheckResult.UpToDate
	}

	test("reports Unknown when the release tag cannot be parsed") {
		val checker = UpdateChecker(mockk())

		checker.evaluate(currentVersion, release("not-a-version")) shouldBe UpdateCheckResult.Unknown
	}

	test("reports Unknown when a newer release has no APK asset attached") {
		val checker = UpdateChecker(mockk())

		checker.evaluate(currentVersion, release("v0.20.0", emptyList())) shouldBe UpdateCheckResult.Unknown
	}

	test("checkForUpdate reports Unknown when the API call fails") {
		val api = mockk<GitHubReleaseApi>()
		coEvery { api.getLatestRelease() } returns null
		val checker = UpdateChecker(api)

		checker.checkForUpdate("0.19.0-dev.959") shouldBe UpdateCheckResult.Unknown
	}

	test("checkForUpdate reports Unknown when the current versionName is malformed") {
		val checker = UpdateChecker(mockk())

		checker.checkForUpdate("not-a-version") shouldBe UpdateCheckResult.Unknown
	}

	test("checkForUpdate delegates to evaluate on a successful API response") {
		val api = mockk<GitHubReleaseApi>()
		coEvery { api.getLatestRelease() } returns release("v0.20.0", listOf(releaseApkAsset))
		val checker = UpdateChecker(api)

		val result = checker.checkForUpdate("0.19.0-dev.959")

		result.shouldBeInstanceOf<UpdateCheckResult.UpdateAvailable>()
	}
})
