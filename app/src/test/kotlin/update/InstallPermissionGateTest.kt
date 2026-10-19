package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InstallPermissionGateTest : FunSpec({
	test("allows immediately without opening settings when permission is already granted") {
		var settingsOpened = false
		var awaited = false
		val gate = InstallPermissionGate(
			canInstall = { true },
			openSettings = { settingsOpened = true; true },
			awaitReturn = { awaited = true },
		)

		gate.ensurePermission() shouldBe true
		settingsOpened shouldBe false
		awaited shouldBe false
	}

	test("allows after the user grants the permission in settings and returns") {
		var granted = false
		var awaited = false
		val gate = InstallPermissionGate(
			canInstall = { granted },
			openSettings = { true },
			awaitReturn = { awaited = true; granted = true },
		)

		gate.ensurePermission() shouldBe true
		awaited shouldBe true
	}

	test("denies when the user returns from settings without granting the permission") {
		var awaited = false
		val gate = InstallPermissionGate(
			canInstall = { false },
			openSettings = { true },
			awaitReturn = { awaited = true },
		)

		gate.ensurePermission() shouldBe false
		awaited shouldBe true
	}

	test("denies without waiting when the settings screen cannot be opened") {
		var awaited = false
		val gate = InstallPermissionGate(
			canInstall = { false },
			openSettings = { false },
			awaitReturn = { awaited = true },
		)

		gate.ensurePermission() shouldBe false
		awaited shouldBe false
	}
})
