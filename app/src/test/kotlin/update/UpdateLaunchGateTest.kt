package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * Regression coverage for the on-device bug where the "Update available" dialog flashed briefly
 * then was immediately replaced by the app: StartupActivity finished itself (navigating to
 * MainActivity) independently of whether the update dialog was showing, tearing the dialog's
 * window down along with it.
 *
 * [UpdateLaunchGate.awaitBeforeNavigating] is the fix - the piece of coordination logic a caller
 * (StartupActivity) must suspend on before finishing/navigating. These tests verify that
 * coordination contract in isolation, independent of Android; the AlertDialog/Activity-lifecycle
 * wiring itself is not covered here since this repo has no Robolectric/instrumented test setup -
 * that part remains verified manually on-device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateLaunchGateTest : FunSpec({
	val update = UpdateCheckResult.UpdateAvailable(
		version = AppVersion(0, 19, 0, 960),
		tagName = "v0.19.0-dev.960",
		releaseUrl = "https://github.com/xanderphillips/jellyfin-androidtv/releases/tag/v0.19.0-dev.960",
		apkDownloadUrl = "https://example.com/app-release.apk",
		apkAssetName = "jellyfin-androidtv-v0.19.0-dev.960-release.apk",
	)

	test("resolves immediately without invoking showDialog when there is no update") {
		val checker = mockk<UpdateChecker>()
		coEvery { checker.checkForUpdate(any()) } returns UpdateCheckResult.UpToDate
		val gate = UpdateLaunchGate(checker)
		var dialogShown = false

		runTest {
			gate.awaitBeforeNavigating("0.19.0-dev.959") { dialogShown = true }
		}

		dialogShown shouldBe false
	}

	test("resolves immediately without invoking showDialog when the check result is unknown") {
		val checker = mockk<UpdateChecker>()
		coEvery { checker.checkForUpdate(any()) } returns UpdateCheckResult.Unknown
		val gate = UpdateLaunchGate(checker)
		var dialogShown = false

		runTest {
			gate.awaitBeforeNavigating("0.19.0-dev.959") { dialogShown = true }
		}

		dialogShown shouldBe false
	}

	test("passes the UpdateAvailable result through to showDialog") {
		val checker = mockk<UpdateChecker>()
		coEvery { checker.checkForUpdate(any()) } returns update
		val gate = UpdateLaunchGate(checker)
		var received: UpdateCheckResult.UpdateAvailable? = null

		runTest {
			gate.awaitBeforeNavigating("0.19.0-dev.959") { received = it }
		}

		received shouldBe update
	}

	test("does not complete until showDialog resolves - this is the race-prevention contract") {
		val checker = mockk<UpdateChecker>()
		coEvery { checker.checkForUpdate(any()) } returns update
		val gate = UpdateLaunchGate(checker)
		val dialogDismissed = CompletableDeferred<Unit>()

		runTest {
			val job = launch {
				gate.awaitBeforeNavigating("0.19.0-dev.959") { dialogDismissed.await() }
			}

			advanceUntilIdle()
			// The gate must still be suspended - a caller awaiting it (e.g. before finishing an
			// activity) must not proceed while the dialog is still up.
			job.isCompleted shouldBe false

			dialogDismissed.complete(Unit)
			advanceUntilIdle()
			job.isCompleted shouldBe true
		}
	}
})
