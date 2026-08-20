package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class AppVersionTest : FunSpec({
	test("parses a plain core version") {
		AppVersion.parseOrNull("2.0.0") shouldBe AppVersion(2, 0, 0, null)
	}

	test("parses a pre-release version") {
		AppVersion.parseOrNull("2.0.0-rc.3") shouldBe AppVersion(2, 0, 0, 3)
	}

	test("parses this project's dev-snapshot scheme") {
		AppVersion.parseOrNull("0.19.0-dev.959") shouldBe AppVersion(0, 19, 0, 959)
	}

	test("strips a leading v as used in GitHub release tags") {
		AppVersion.parseOrNull("v0.19.10") shouldBe AppVersion(0, 19, 10, null)
	}

	test("returns null for malformed input") {
		AppVersion.parseOrNull("not-a-version") should beNull()
		AppVersion.parseOrNull("1.2") should beNull()
		AppVersion.parseOrNull("") should beNull()
		AppVersion.parseOrNull("1.2.x") should beNull()
	}

	test("returns null for an unparseable pre-release number") {
		AppVersion.parseOrNull("1.2.3-rc.x") shouldBe AppVersion(1, 2, 3, null)
	}

	test("compares core versions numerically, not lexicographically") {
		(AppVersion.parseOrNull("0.9.0")!! < AppVersion.parseOrNull("0.10.0")!!) shouldBe true
	}

	test("a release without a pre-release part is newer than one with, at the same core version") {
		(AppVersion.parseOrNull("2.0.0-rc.1")!! < AppVersion.parseOrNull("2.0.0")!!) shouldBe true
	}

	test("higher pre-release numbers are newer at the same core version") {
		(AppVersion.parseOrNull("2.0.0-dev.1")!! < AppVersion.parseOrNull("2.0.0-dev.2")!!) shouldBe true
	}

	test("equal versions compare as equal") {
		AppVersion.parseOrNull("0.19.0-dev.959")!!.compareTo(AppVersion.parseOrNull("0.19.0-dev.959")!!) shouldBe 0
	}
})
