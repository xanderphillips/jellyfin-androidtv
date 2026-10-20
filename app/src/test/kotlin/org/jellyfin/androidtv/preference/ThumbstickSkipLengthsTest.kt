package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ThumbstickSkipLengthsTest : FunSpec({
	test("allowed values are 5s, 15s, 30s, 1min, 5min") {
		ThumbstickSkipLengths.ALLOWED_MS shouldBe listOf(5_000, 15_000, 30_000, 60_000, 300_000)
	}

	test("defaults are allowed values") {
		ThumbstickSkipLengths.ALLOWED_MS.contains(ThumbstickSkipLengths.DEFAULT_BACK_MS) shouldBe true
		ThumbstickSkipLengths.ALLOWED_MS.contains(ThumbstickSkipLengths.DEFAULT_FORWARD_MS) shouldBe true
	}

	test("coerce keeps allowed values and snaps others to the closest") {
		ThumbstickSkipLengths.coerce(15_000) shouldBe 15_000
		ThumbstickSkipLengths.coerce(10_000) shouldBe 5_000
		ThumbstickSkipLengths.coerce(20_000) shouldBe 15_000
		ThumbstickSkipLengths.coerce(0) shouldBe 5_000
		ThumbstickSkipLengths.coerce(10_000_000) shouldBe 300_000
		ThumbstickSkipLengths.coerce(-1) shouldBe 5_000
	}

	test("next cycles through the values and wraps") {
		ThumbstickSkipLengths.next(5_000) shouldBe 15_000
		ThumbstickSkipLengths.next(60_000) shouldBe 300_000
		ThumbstickSkipLengths.next(300_000) shouldBe 5_000
		ThumbstickSkipLengths.next(12_345) shouldBe 30_000
	}
})
