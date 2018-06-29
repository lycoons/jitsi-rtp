package org.jitsi.rtp

import io.kotlintest.matchers.collections.shouldContainInOrder
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import java.nio.ByteBuffer

internal class BitBufferRtpHeaderTest : ShouldSpec() {
    // v=2, p=1, x=0, cc=3 = 0xA3
    // m=1, pt=96 = 0xE0
    // seqnum 4224 = 0x10 0x80
    // timestamp 98765 = 0x00 0x01 0x81 0xCD
    // ssrc 1234567 = 0x00 0x12 0xD6 0x87
    // csrc 1 = 0x00 0x00 0x00 0x01
    // csrc 2 = 0x00 0x00 0x00 0x02
    // csrc 3 = 0x00 0x00 0x00 0x03
    private val headerNoExtensions = ByteBuffer.wrap(byteArrayOf(
        0xA3.toByte(),  0xE0.toByte(),  0x10,           0x80.toByte(),
        0x00,           0x01,           0x81.toByte(),  0xCD.toByte(),
        0x00,           0x12,           0xD6.toByte(),  0x87.toByte(),
        0x00,           0x00,           0x00,           0x01,
        0x00,           0x00,           0x00,           0x02,
        0x00,           0x00,           0x00,           0x03
    ))
    private fun idLengthByte(id: Int, length: Int): Byte {
        return ((id shl 4) or length).toByte()
    }
    // v=2, p=1, x=1, cc=3 = 0xB3
    // m=1, pt=96 = 0xE0
    // seqnum 4224 = 0x10 0x80
    // timestamp 98765 = 0x00 0x01 0x81 0xCD
    // ssrc 1234567 = 0x00 0x12 0xD6 0x87
    // csrc 1 = 0x00 0x00 0x00 0x01
    // csrc 2 = 0x00 0x00 0x00 0x02
    // csrc 3 = 0x00 0x00 0x00 0x03
    private val headerWithExtensionBit = byteArrayOf(
        0xB3.toByte(),  0xE0.toByte(),  0x10,           0x80.toByte(),
        0x00,           0x01,           0x81.toByte(),  0xCD.toByte(),
        0x00,           0x12,           0xD6.toByte(),  0x87.toByte(),
        0x00,           0x00,           0x00,           0x01,
        0x00,           0x00,           0x00,           0x02,
        0x00,           0x00,           0x00,           0x03
    )
    private val headerWithOneByteExtensions = ByteBuffer.wrap(headerWithExtensionBit.plus(byteArrayOf(
        // Extensions
        0xBE.toByte(),                   0xDE.toByte(),  0x00,                          0x03,
        idLengthByte(1, 0),   0x42,           idLengthByte(2, 1), 0x42,
        0x42,                            0x00,           0x00,                          idLengthByte(3, 3),
        0x42,                            0x42,           0x42,                          0x42
    )))
    private val headerWithTwoByteExtensions = ByteBuffer.wrap(headerWithExtensionBit.plus(byteArrayOf(
        0x10,           0x00,           0x00,           0x03,
        0x01,           0x00,           0x02,           0x01,
        0x42.toByte(),  0x00,           0x03,           0x04,
        0x42.toByte(),  0x42.toByte(),  0x42.toByte(),  0x42.toByte()
    )))

    init {
        "parsing" {
            "a header without extensions" {
                val header = BitBufferRtpHeader.fromBuffer(headerNoExtensions.asReadOnlyBuffer())
                header.version shouldBe 2
                header.hasPadding shouldBe true
                header.hasExtension shouldBe false
                header.csrcCount shouldBe 3
                header.marker shouldBe true
                header.payloadType shouldBe 96
                header.sequenceNumber shouldBe 4224
                header.timestamp shouldBe 98765
                header.ssrc shouldBe 1234567
                header.csrcs should haveSize(3)
                header.csrcs.shouldContainInOrder(listOf<Long>(1, 2, 3))
                header.extensions.size shouldBe 0
            }
            "a header with one byte extensions" {
                val header = BitBufferRtpHeader.fromBuffer(headerWithOneByteExtensions.asReadOnlyBuffer())
                header.extensions.size shouldBe 3
                header.extensions.values.forEach {
                    it.shouldBeTypeOf<RtpOneByteHeaderExtension>()
                }
            }
            "a header with two byte extensions" {
                val header = BitBufferRtpHeader.fromBuffer(headerWithTwoByteExtensions.asReadOnlyBuffer())
                header.extensions.size shouldBe 3
                header.extensions.values.forEach {
                    it.shouldBeTypeOf<RtpTwoByteHeaderExtension>()
                }
            }
        }
        "writing" {
            "should update the object's value without touching the buffer" {
                val header = BitBufferRtpHeader.fromBuffer(headerNoExtensions.asReadOnlyBuffer())
                header.version = 10
                header.version shouldBe 10
                // We passed the buffer as readonly, so we know it hasn't been changed
            }
        }
        "serializing" {
            val header = BitBufferRtpHeader.fromBuffer(headerNoExtensions.asReadOnlyBuffer())
            val newBuf = ByteBuffer.allocate(headerNoExtensions.limit())
            header.serializeToBuffer(newBuf)
            newBuf.rewind()
            headerNoExtensions.rewind()
            should("match the original buffer") {
                newBuf.compareTo(headerNoExtensions) shouldBe 0
            }
        }
    }
}
