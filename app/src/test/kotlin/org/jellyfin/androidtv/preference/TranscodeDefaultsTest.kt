package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TranscodeDefaultsTest : FunSpec({
	test("H.264 is preferred for transcodes by default while the server encodes in software") {
		TranscodeDefaults.PREFER_H264 shouldBe true
	}
})
