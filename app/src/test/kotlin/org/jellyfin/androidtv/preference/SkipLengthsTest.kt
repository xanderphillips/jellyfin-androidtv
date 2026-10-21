package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SkipLengthsTest : FunSpec({
	test("offered values are 5s, 15s, 30s, 1min, 5min") {
		SkipLengths.ALLOWED_MS shouldBe listOf(5_000, 15_000, 30_000, 60_000, 300_000)
	}

	test("next cycles through the values and wraps") {
		SkipLengths.next(5_000) shouldBe 15_000
		SkipLengths.next(15_000) shouldBe 30_000
		SkipLengths.next(60_000) shouldBe 300_000
		SkipLengths.next(300_000) shouldBe 5_000
	}

	test("legacy stored values off the list move to the next step up") {
		SkipLengths.next(10_000) shouldBe 15_000
		SkipLengths.next(20_000) shouldBe 30_000
		SkipLengths.next(45_000) shouldBe 60_000
		SkipLengths.next(0) shouldBe 5_000
		SkipLengths.next(1_000_000) shouldBe 5_000
	}
})
