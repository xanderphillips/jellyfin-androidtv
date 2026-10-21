package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.BufferDefaults
import org.jellyfin.androidtv.preference.constant.BufferLength

private const val MIB = 1024L * 1024L

class BufferDefaultsTest : FunSpec({
	test("Meta headsets default to Large whatever the heap") {
		BufferDefaults.defaultBufferLength(isMetaHeadset = true, maxHeapBytes = 64 * MIB) shouldBe BufferLength.LARGE
	}

	test("a 256 MB heap such as the Onn 4K Pro defaults to Large") {
		BufferDefaults.defaultBufferLength(false, 256 * MIB) shouldBe BufferLength.LARGE
		BufferDefaults.defaultBufferLength(false, 192 * MIB) shouldBe BufferLength.LARGE
	}

	test("small heaps keep Auto") {
		BufferDefaults.defaultBufferLength(false, 128 * MIB) shouldBe BufferLength.AUTO
		BufferDefaults.defaultBufferLength(false, 191 * MIB) shouldBe BufferLength.AUTO
	}

	test("the byte cap is a third of the heap between 32 and 128 MB") {
		BufferDefaults.byteCap(256 * MIB) shouldBe (256 * MIB / 3).toInt()
		BufferDefaults.byteCap(64 * MIB) shouldBe (32 * MIB).toInt()
		BufferDefaults.byteCap(1024 * MIB) shouldBe (128 * MIB).toInt()
	}
})
