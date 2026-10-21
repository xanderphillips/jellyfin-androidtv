package util.profile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.profile.hlsTranscodeVideoCodecs

class HlsTranscodeCodecsTests : FunSpec({
	test("devices without HEVC decoding only get H.264") {
		hlsTranscodeVideoCodecs(supportsHevc = false, preferH264 = false) shouldBe listOf(Codec.Video.H264)
		hlsTranscodeVideoCodecs(supportsHevc = false, preferH264 = true) shouldBe listOf(Codec.Video.H264)
	}

	test("HEVC stays first by default so other devices are unchanged") {
		hlsTranscodeVideoCodecs(supportsHevc = true, preferH264 = false) shouldBe listOf(Codec.Video.HEVC, Codec.Video.H264)
	}

	test("preferring H.264 lists it first and keeps HEVC as an option") {
		hlsTranscodeVideoCodecs(supportsHevc = true, preferH264 = true) shouldBe listOf(Codec.Video.H264, Codec.Video.HEVC)
	}
})
