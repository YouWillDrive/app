package ru.gd_alt.youwilldrive.data.cbor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// --- Custom Exception ---
class CborException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// --- Represents CBOR 'undefined' ---
object CborUndefined {
    override fun toString(): String = "CborUndefined"
}

/**
 * CBOR Interactor for encoding and decoding CBOR data.
 * This class provides methods to encode Kotlin objects into CBOR format
 * and decode CBOR data back into Kotlin objects.
 **/
class CborInteractor(
    /**
     * Hook function for custom encoding.
     * Takes a Kotlin object.
     * Returns null if the hook doesn't handle this object type.
     * Returns Pair(tag: Long, dataToEncode: Any) if it handles the type.
     * The 'dataToEncode' will then be recursively passed to the standard encoder.
     */
    val enHook: (Any) -> Pair<Long, Any>? = { null }, // Default: no-op hook

    /**
     * Hook function for custom decoding.
     * Takes the tag number (Long) and the decoded data item (Any?).
     * Returns the final processed Kotlin object.
     * If the hook doesn't handle this tag, it should return the original 'decodedItem'.
     */
    val deHook: (Long, Any?) -> Any? = { _, decodedItem -> decodedItem } // Default: pass-through hook
) {

    // --- Constants ---
    private companion object {
        const val MAJOR_TYPE_UNSIGNED_INT = 0
        const val MAJOR_TYPE_NEGATIVE_INT = 1
        const val MAJOR_TYPE_BYTE_STRING = 2
        const val MAJOR_TYPE_TEXT_STRING = 3
        const val MAJOR_TYPE_ARRAY = 4
        const val MAJOR_TYPE_MAP = 5
        const val MAJOR_TYPE_TAG = 6
        const val MAJOR_TYPE_SIMPLE_FLOAT = 7

        const val ADDITIONAL_INFO_MAX_SIMPLE = 23
        const val ADDITIONAL_INFO_UINT8 = 24
        const val ADDITIONAL_INFO_UINT16 = 25
        const val ADDITIONAL_INFO_UINT32 = 26
        const val ADDITIONAL_INFO_UINT64 = 27
        const val ADDITIONAL_INFO_INDEFINITE = 31

        const val SIMPLE_FALSE = 20
        const val SIMPLE_TRUE = 21
        const val SIMPLE_NULL = 22
        const val SIMPLE_UNDEFINED = 23

        const val BREAK_STOP_CODE = 0xFF

        // Standard Tags
        const val TAG_DATETIME_STRING = 0L
        const val TAG_EPOCH_DATETIME = 1L
        const val TAG_POSITIVE_BIGNUM = 2L
        const val TAG_NEGATIVE_BIGNUM = 3L
        // Add other standard tags here if needed (e.g., 4, 5 for decimal fractions/bigfloats)
    }

    // --- Encoding ---

    fun encode(item: Any?): ByteArray {
        val outputStream = ByteArrayOutputStream()
        try {
            encodeItem(item, outputStream)
        } catch (e: CborException) {
            throw e // Re-throw specific exceptions
        } catch (e: Exception) {
            throw CborException("Encoding failed for item: $item", e)
        }
        return outputStream.toByteArray()
    }

    private fun encodeItem(item: Any?, stream: ByteArrayOutputStream) {
        // 1. Check Custom Encoding Hook
        if (item != null) {
            val hookResult = enHook(item)
            if (hookResult != null) {
                val (tag, dataToEncode) = hookResult
                encodeTag(tag, stream)
                encodeItem(dataToEncode, stream) // Recursively encode the data provided by the hook
                return // Handled by hook
            }
        }

        // 2. Standard Encoding based on Kotlin type
        when (item) {
            null -> encodeSimpleValue(SIMPLE_NULL, stream)
            is Boolean -> encodeSimpleValue(if (item) SIMPLE_TRUE else SIMPLE_FALSE, stream)
            is Byte -> encodeLong(item.toLong(), stream)
            is Short -> encodeLong(item.toLong(), stream)
            is Int -> encodeLong(item.toLong(), stream)
            is Long -> encodeLong(item, stream)
            is BigInteger -> encodeBigInteger(item, stream) // Handles standard tags 2 & 3
            is Float -> encodeFloat(item, stream)
            is Double -> encodeDouble(item, stream)
            is String -> encodeString(item, stream)
            is ByteArray -> encodeByteArray(item, stream)
            is List<*> -> encodeList(item, stream)
            is Array<*> -> encodeList(item.asList(), stream) // Treat Array same as List
            is Map<*, *> -> encodeMap(item, stream)
            is Instant -> encodeInstant(item, stream) // Handles standard tag 1
            CborUndefined -> encodeSimpleValue(SIMPLE_UNDEFINED, stream) // Handle our Undefined object
            // Add other specific types before the fallback if needed
            else -> throw CborException("Unsupported type for CBOR encoding: ${item::class.qualifiedName}")
        }
    }

    private fun encodeMajorTypeAndValue(majorType: Int, value: Long, stream: ByteArrayOutputStream) {
        val mt = (majorType and 0x07) shl 5
        when {
            value < 0 -> throw CborException("Value cannot be negative for this encoding: $value")
            value <= ADDITIONAL_INFO_MAX_SIMPLE -> { // 0-23
                stream.write(mt or (value.toInt() and 0x1F))
            }
            value <= 0xFF -> { // 24: 1 byte
                stream.write(mt or ADDITIONAL_INFO_UINT8)
                stream.write(value.toInt() and 0xFF)
            }
            value <= 0xFFFF -> { // 25: 2 bytes
                stream.write(mt or ADDITIONAL_INFO_UINT16)
                stream.writeShort(value.toInt())
            }
            value <= 0xFFFFFFFFL -> { // 26: 4 bytes
                stream.write(mt or ADDITIONAL_INFO_UINT32)
                stream.writeInt(value.toInt())
            }
            value <= Long.MAX_VALUE -> { // 27: 8 bytes
                stream.write(mt or ADDITIONAL_INFO_UINT64)
                stream.writeLong(value)
            }
            else -> {
                // This case should theoretically not happen for standard types if inputs are Long.
                // BigInteger is handled separately. Lengths exceeding Long.MAX_VALUE are problematic.
                throw CborException("Value too large for CBOR encoding (exceeds 64-bit unsigned): $value")
            }
        }
    }

    private fun encodeLong(value: Long, stream: ByteArrayOutputStream) {
        if (value >= 0) {
            encodeMajorTypeAndValue(MAJOR_TYPE_UNSIGNED_INT, value, stream)
        } else {
            // CBOR Negative Int: -1 - value (encoded as unsigned)
            val encodedValue = -1L - value
            if (encodedValue < 0) { // Check for potential overflow (Long.MIN_VALUE)
                // Special case: Long.MIN_VALUE needs BigInteger encoding conceptually
                // encodeMajorTypeAndValue(MAJOR_TYPE_NEGATIVE_INT, Long.MAX_VALUE + 1) -> doesn't work
                // Fallback to BigInteger encoding for this edge case
                encodeBigInteger(BigInteger.valueOf(value), stream)
            } else {
                encodeMajorTypeAndValue(MAJOR_TYPE_NEGATIVE_INT, encodedValue, stream)
            }
        }
    }

    private fun encodeBigInteger(value: BigInteger, stream: ByteArrayOutputStream) {
        when {
            value >= BigInteger.ZERO -> {
                encodeTag(TAG_POSITIVE_BIGNUM, stream)
                encodeByteArray(value.toByteArray(), stream) // BigInteger.toByteArray() is big-endian MSB
            }
            else -> {
                // For negative bignums, tag 3 is followed by byte string of (-1 - value)
                val encodedValue = BigInteger.valueOf(-1).subtract(value)
                encodeTag(TAG_NEGATIVE_BIGNUM, stream)
                encodeByteArray(encodedValue.toByteArray(), stream)
            }
        }
    }

    private fun encodeFloat(value: Float, stream: ByteArrayOutputStream) {
        // Prefer single-precision (AI 26) for Float
        // Could add half-precision (AI 25) encoding here if needed, but it's more complex
        stream.write((MAJOR_TYPE_SIMPLE_FLOAT shl 5) or ADDITIONAL_INFO_UINT32)
        stream.writeInt(value.toRawBits())
    }

    private fun encodeDouble(value: Double, stream: ByteArrayOutputStream) {
        // Use double-precision (AI 27) for Double
        stream.write((MAJOR_TYPE_SIMPLE_FLOAT shl 5) or ADDITIONAL_INFO_UINT64)
        stream.writeLong(value.toRawBits())
    }

    private fun encodeString(value: String, stream: ByteArrayOutputStream) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        encodeMajorTypeAndValue(MAJOR_TYPE_TEXT_STRING, bytes.size.toLong(), stream)
        stream.write(bytes)
    }

    private fun encodeByteArray(value: ByteArray, stream: ByteArrayOutputStream) {
        encodeMajorTypeAndValue(MAJOR_TYPE_BYTE_STRING, value.size.toLong(), stream)
        stream.write(value)
    }

    private fun encodeList(value: List<*>, stream: ByteArrayOutputStream) {
        encodeMajorTypeAndValue(MAJOR_TYPE_ARRAY, value.size.toLong(), stream)
        value.forEach { encodeItem(it, stream) }
    }

    private fun encodeMap(value: Map<*, *>, stream: ByteArrayOutputStream) {
        // Note: CBOR spec recommends sorting keys for canonical representation,
        // but it's not strictly required for basic interoperability. We skip sorting here.
        encodeMajorTypeAndValue(MAJOR_TYPE_MAP, value.size.toLong(), stream)
        value.forEach { (key, mapValue) ->
            encodeItem(key, stream)
            encodeItem(mapValue, stream)
        }
    }

    private fun encodeTag(tag: Long, stream: ByteArrayOutputStream) {
        if (tag < 0) throw CborException("CBOR tags must be non-negative: $tag")
        encodeMajorTypeAndValue(MAJOR_TYPE_TAG, tag, stream)
    }

    private fun encodeInstant(instant: Instant, stream: ByteArrayOutputStream) {
        // Standard Tag 1: Epoch-based date/time
        // We can encode as integer seconds if fractional seconds are zero, or float otherwise.
        // Encoding as Double is simpler and always valid according to RFC 8949 Appendix A examples.
        encodeTag(TAG_EPOCH_DATETIME, stream)
        val seconds = instant.epochSecond
        val nanos = instant.nano
        if (nanos == 0) {
            encodeLong(seconds, stream) // Encode as integer if possible
        } else {
            val epochDouble = seconds.toDouble() + nanos.toDouble() / 1_000_000_000.0
            encodeDouble(epochDouble, stream)
        }
    }

    private fun encodeSimpleValue(value: Int, stream: ByteArrayOutputStream) {
        if (value < 0 || value > 23 && value != SIMPLE_NULL && value != SIMPLE_UNDEFINED && value != SIMPLE_TRUE && value != SIMPLE_FALSE ) {
            throw CborException("Invalid simple value: $value")
        }
        stream.write((MAJOR_TYPE_SIMPLE_FLOAT shl 5) or value)
    }

    // --- Decoding ---

    fun decode(data: ByteArray): Any? {
        val inputStream = CborInputStream(data)
        return try {
            decodeItem(inputStream)
        } catch (e: CborException) {
            throw e
        } catch (e: EOFException) {
            throw CborException("Unexpected end of CBOR data", e)
        } catch (e: Exception) {
            throw CborException("Decoding failed", e)
        } finally {

        }
    }

    private fun decodeItem(stream: CborInputStream): Any? {
        val initialByte = stream.read()
        if (initialByte == -1) throw EOFException("Unexpected end of stream while reading initial byte")

        val majorType = (initialByte ushr 5) and 0x07
        val additionalInfo = initialByte and 0x1F

        val value: Long? = readValue(additionalInfo, stream) // Can be null for indefinite length marker

        return when (majorType) {
            MAJOR_TYPE_UNSIGNED_INT -> {
                if (value == null) throw CborException("Invalid indefinite length for unsigned integer")
                // Potentially return BigInteger if value > Long.MAX_VALUE, but requires value read logic update
                value // Return as Long
            }
            MAJOR_TYPE_NEGATIVE_INT -> {
                if (value == null) throw CborException("Invalid indefinite length for negative integer")
                // val bigVal = BigInteger.valueOf(value).add(BigInteger.ONE).negate() -> safer for overflow
                // if (bigVal > BigInteger.valueOf(Long.MAX_VALUE) || bigVal < BigInteger.valueOf(Long.MIN_VALUE)) bigVal
                // else bigVal.toLong()
                try {
                    Math.subtractExact(-1L, value)
                } catch (e: ArithmeticException) {
                    // Handle Long.MIN_VALUE case or other large negatives by returning BigInteger
                    BigInteger.valueOf(-1).subtract(BigInteger.valueOf(value))
                }
            }
            MAJOR_TYPE_BYTE_STRING -> decodeByteString(additionalInfo, value, stream)
            MAJOR_TYPE_TEXT_STRING -> decodeTextString(additionalInfo, value, stream)
            MAJOR_TYPE_ARRAY -> decodeArray(additionalInfo, value, stream)
            MAJOR_TYPE_MAP -> decodeMap(additionalInfo, value, stream)
            MAJOR_TYPE_TAG -> decodeTag(additionalInfo, value, stream)
            MAJOR_TYPE_SIMPLE_FLOAT -> decodeSimpleFloat(additionalInfo, value, stream)
            else -> throw CborException("Reserved or unknown major type: $majorType")
        }
    }

    // Reads the length/value based on additional info (0-27)
    private fun readValue(addInfo: Int, stream: CborInputStream): Long? {
        return when (addInfo) {
            in 0..ADDITIONAL_INFO_MAX_SIMPLE -> addInfo.toLong() // Value is directly encoded
            ADDITIONAL_INFO_UINT8 -> stream.readUnsignedByte().toLong()
            ADDITIONAL_INFO_UINT16 -> stream.readUnsignedShort().toLong()
            ADDITIONAL_INFO_UINT32 -> stream.readUnsignedInt().toLong()
            ADDITIONAL_INFO_UINT64 -> stream.readLong() // Can be negative if MSB is 1, but represents large uint
            ADDITIONAL_INFO_INDEFINITE -> null // Signal for indefinite length
            else -> throw CborException("Reserved or invalid additional information: $addInfo")
        }
    }

    private fun decodeByteString(addInfo: Int, lengthVal: Long?, stream: CborInputStream): ByteArray {
        return if (addInfo == ADDITIONAL_INFO_INDEFINITE) { // Indefinite length
            val buffer = ByteArrayOutputStream()
            while (true) {
                val chunkItem = decodeItem(stream) // Decode next item, expecting byte string chunks
                if (chunkItem == CborBreakStop) break // Found the break marker
                if (chunkItem !is ByteArray) throw CborException("Expected byte string chunk or break stop for indefinite byte string, got ${chunkItem?.javaClass?.simpleName}")
                buffer.write(chunkItem)
            }
            buffer.toByteArray()
        } else { // Definite length
            val length = lengthVal?.toIntChecked("Byte string length") ?: throw CborException("Invalid length for byte string")
            stream.readBytes(length)
        }
    }

    private fun decodeTextString(addInfo: Int, lengthVal: Long?, stream: CborInputStream): String {
        return if (addInfo == ADDITIONAL_INFO_INDEFINITE) { // Indefinite length
            val buffer = StringBuilder()
            while (true) {
                val chunkItem = decodeItem(stream) // Decode next item, expecting text string chunks
                if (chunkItem == CborBreakStop) break // Found the break marker
                if (chunkItem !is String) throw CborException("Expected text string chunk or break stop for indefinite text string, got ${chunkItem?.javaClass?.simpleName}")
                buffer.append(chunkItem)
            }
            buffer.toString()
        } else { // Definite length
            val length = lengthVal?.toIntChecked("Text string length") ?: throw CborException("Invalid length for text string")
            val bytes = stream.readBytes(length)
            try {
                String(bytes, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                throw CborException("Failed to decode UTF-8 text string", e)
            }
        }
    }

    private fun decodeArray(addInfo: Int, countVal: Long?, stream: CborInputStream): List<Any?> {
        return if (addInfo == ADDITIONAL_INFO_INDEFINITE) { // Indefinite length
            val list = mutableListOf<Any?>()
            while (true) {
                // Peek to check for break stop before attempting to decode item
                if (stream.peek() == BREAK_STOP_CODE) {
                    stream.read() // Consume the break stop byte
                    break
                }
                list.add(decodeItem(stream))
            }
            list
        } else { // Definite length
            val count = countVal?.toIntChecked("Array count") ?: throw CborException("Invalid count for array")
            List(count) { decodeItem(stream) }
        }
    }

    private fun decodeMap(addInfo: Int, countVal: Long?, stream: CborInputStream): Map<Any?, Any?> {
        return if (addInfo == ADDITIONAL_INFO_INDEFINITE) { // Indefinite length
            val map = mutableMapOf<Any?, Any?>()
            while (true) {
                // Peek to check for break stop before attempting to decode key
                if (stream.peek() == BREAK_STOP_CODE) {
                    stream.read() // Consume the break stop byte
                    break
                }
                val key = decodeItem(stream)
                // Check again for break stop *after* decoding key, in case of odd number of items before break
                if (stream.peek() == BREAK_STOP_CODE) {
                    throw CborException("Unexpected break stop after map key (odd number of items in indefinite map)")
                }
                val value = decodeItem(stream)
                if (map.containsKey(key)) {
                    // RFC 8949: "If a receiving application encounters duplicate keys, it needs to decide how to handle them."
                    // We'll overwrite here, but could throw or preserve first.
                    println("Warning: Duplicate key encountered in CBOR map: $key")
                }
                map[key] = value
            }
            map
        } else { // Definite length
            val count = countVal?.toIntChecked("Map entry count") ?: throw CborException("Invalid count for map")
            val map = mutableMapOf<Any?, Any?>()
            repeat(count) {
                val key = decodeItem(stream)
                val value = decodeItem(stream)
                if (map.containsKey(key)) {
                    println("Warning: Duplicate key encountered in CBOR map: $key")
                }
                map[key] = value
            }
            map
        }
    }

    private fun decodeTag(addInfo: Int, tagVal: Long?, stream: CborInputStream): Any? {
        val tag = tagVal ?: throw CborException("Indefinite length tag is invalid")
        if (tag < 0) throw CborException("Invalid negative tag value encountered during decode")

        // Decode the data item associated with the tag
        val decodedItem = decodeItem(stream)

        // 1. Handle Standard Tags directly
        val standardResult = when (tag) {
            TAG_DATETIME_STRING -> decodeDateTimeString(decodedItem)
            TAG_EPOCH_DATETIME -> decodeEpochDateTime(decodedItem)
            TAG_POSITIVE_BIGNUM -> decodePositiveBignum(decodedItem)
            TAG_NEGATIVE_BIGNUM -> decodeNegativeBignum(decodedItem)
            // Add other standard tag handlers here
            else -> null // Not a standard tag we handle directly
        }

        // 2. If not handled by standard processing, pass to the custom deHook
        return standardResult ?: deHook(tag, decodedItem)
    }

    private fun decodeDateTimeString(item: Any?): Instant? {
        if (item !is String) throw CborException("Expected text string for Tag 0 (DateTime String), got ${item?.javaClass?.simpleName}")
        return try {
            // Try ISO-8601 OffsetDateTime format first
            OffsetDateTime.parse(item, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (e: DateTimeParseException) {
            try {
                // Fallback or alternative formats if needed, or just rethrow/return null
                // Example: Maybe Instant.parse if it's a Z-ulu format
                Instant.parse(item)
            } catch (e2: DateTimeParseException) {
                throw CborException("Failed to parse DateTime string (Tag 0): '$item'", e)
            }
        }
    }

    private fun decodeEpochDateTime(item: Any?): Instant {
        return when (item) {
            is Long -> Instant.ofEpochSecond(item, 0) // Assume integer means seconds
            is Double -> {
                val seconds = item.toLong()
                val nanos = ((item - seconds) * 1_000_000_000.0).toLong().coerceIn(0, 999_999_999)
                Instant.ofEpochSecond(seconds, nanos)
            }
            is Float -> { // Handle float epoch too
                val seconds = item.toLong()
                val nanos = ((item - seconds) * 1_000_000_000.0f).toLong().coerceIn(0, 999_999_999)
                Instant.ofEpochSecond(seconds, nanos)
            }
            else -> throw CborException("Expected Long, Float, or Double for Tag 1 (Epoch DateTime), got ${item?.javaClass?.simpleName}")
        }
    }

    private fun decodePositiveBignum(item: Any?): BigInteger {
        if (item !is ByteArray) throw CborException("Expected byte string for Tag 2 (Positive Bignum), got ${item?.javaClass?.simpleName}")
        return BigInteger(1, item) // 1 indicates positive signum
    }

    private fun decodeNegativeBignum(item: Any?): BigInteger {
        if (item !is ByteArray) throw CborException("Expected byte string for Tag 3 (Negative Bignum), got ${item?.javaClass?.simpleName}")
        // Tag 3 payload is (-1 - N). So N = -1 - payload
        val payloadBigInt = BigInteger(1, item) // Payload is always positive bytes
        return BigInteger.valueOf(-1).subtract(payloadBigInt)
    }


    private fun decodeSimpleFloat(addInfo: Int, value: Long?, stream: CborInputStream): Any? {
        return when (addInfo) {
            in 0..19 -> addInfo // Simple values 0-19 (unassigned) - return the value itself
            SIMPLE_FALSE -> false
            SIMPLE_TRUE -> true
            SIMPLE_NULL -> null
            SIMPLE_UNDEFINED -> CborUndefined
            ADDITIONAL_INFO_UINT8 -> { // Could be simple value 24-31 if assigned, or 1-byte float (not standard CBOR)
                // Per RFC 8949, AI=24 with Major Type 7 is "Simple value (one-byte uint8_t follows)"
                val simpleVal = value?.toIntChecked("Simple value 1-byte") ?: throw CborException("Invalid length for 1-byte simple value")
                if (simpleVal <= 31) throw CborException("Simple values 24-31 are unassigned") // According to registry Feb 2024
                // You might treat 32-255 as custom simple values if needed, otherwise it's an error
                throw CborException("Invalid simple value encoded with 1-byte length: $simpleVal")
            }
            ADDITIONAL_INFO_UINT16 -> { // Half-Precision Float (16 bit)
                val bits = value?.toIntChecked("Half-float bits") ?: throw CborException("Invalid length for half-float")
                decodeHalfFloat(bits) // Decode to Kotlin Float
            }
            ADDITIONAL_INFO_UINT32 -> { // Single-Precision Float (32 bit)
                val bits = value?.toIntChecked("Single-float bits") ?: throw CborException("Invalid length for single-float")
                Float.fromBits(bits)
            }
            ADDITIONAL_INFO_UINT64 -> { // Double-Precision Float (64 bit)
                val bits = value ?: throw CborException("Invalid length for double-float")
                Double.fromBits(bits)
            }
            ADDITIONAL_INFO_INDEFINITE -> CborBreakStop // Special marker for indefinite length termination
            else -> throw CborException("Invalid additional info for Major Type 7: $addInfo")
        }
    }

    // Basic half-float decoding (simplified, might lose precision/handle edge cases improperly)
    // Based on https://stackoverflow.com/a/6162687/1167781
    private fun decodeHalfFloat(half: Int): Float {
        val mant = half and 0x03ff
        var exp = half and 0x7c00
        val sign = half and 0x8000
        if (exp == 0x7c00) exp = 0x3fc00 // NaN/Inf
        else if (exp != 0) { // Normalized
            exp += 0x1c000
            if (mant == 0 && exp > 0x1c400) return Float.fromBits((sign shl 16) or (exp shl 13) or 0x3ff) // Fix -0 maybe?
        } else if (mant != 0) { // Denormalized
            exp = 0x1c400
            do {
                exp -= 0x400
            } while ((mant and 0x400) == 0) // Corrected loop condition
            //mant = mant shl 1 and 0x7ff // incorrect shift
        }
        val intBits = (sign shl 16) or ((exp or mant) shl 13)
        return Float.fromBits(intBits)
    }


    // --- Helper Extensions / Classes ---

    private fun ByteArrayOutputStream.writeShort(v: Int) {
        write((v ushr 8) and 0xFF)
        write((v ushr 0) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt(v: Int) {
        write((v ushr 24) and 0xFF)
        write((v ushr 16) and 0xFF)
        write((v ushr 8) and 0xFF)
        write((v ushr 0) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeLong(v: Long) {
        write(((v ushr 56) and 0xFF).toInt())
        write(((v ushr 48) and 0xFF).toInt())
        write(((v ushr 40) and 0xFF).toInt())
        write(((v ushr 32) and 0xFF).toInt())
        write(((v ushr 24) and 0xFF).toInt())
        write(((v ushr 16) and 0xFF).toInt())
        write(((v ushr 8) and 0xFF).toInt())
        write(((v ushr 0) and 0xFF).toInt())
    }

    // Internal marker object for break stop code (0xFF)
    private object CborBreakStop

    private class CborInputStream(data: ByteArray) {
        private val stream = ByteArrayInputStream(data)
        private var peekedByte: Int? = null

        fun read(): Int {
            return if (peekedByte != null) {
                val b = peekedByte!!
                peekedByte = null
                b
            } else {
                stream.read()
            }
        }

        fun peek(): Int {
            if (peekedByte == null) {
                peekedByte = stream.read()
            }
            return peekedByte!!
        }

        fun readBytes(count: Int): ByteArray {
            if (count < 0) throw CborException("Cannot read negative number of bytes: $count")
            if (count == 0) return ByteArray(0)

            // Handle peeked byte if applicable
            val buffer = ByteArrayOutputStream(count)
            var bytesRead = 0
            if (peekedByte != null) {
                buffer.write(peekedByte!!)
                peekedByte = null
                bytesRead = 1
                if (bytesRead == count) return buffer.toByteArray()
            }

            val remainingBytes = ByteArray(count - bytesRead)
            val actualRead = stream.read(remainingBytes)

            if (actualRead != remainingBytes.size) {
                throw EOFException("Expected $count bytes, but only ${bytesRead + actualRead} available.")
            }
            buffer.write(remainingBytes)
            return buffer.toByteArray()
        }

        fun readUnsignedByte(): Int {
            val b = read()
            if (b == -1) throw EOFException("Expected 1 byte for unsigned byte, but got EOF")
            return b and 0xFF
        }

        fun readUnsignedShort(): Int {
            val b1 = readUnsignedByte()
            val b2 = readUnsignedByte()
            return (b1 shl 8) or b2
        }

        fun readUnsignedInt(): Long {
            val b1 = readUnsignedByte().toLong()
            val b2 = readUnsignedByte().toLong()
            val b3 = readUnsignedByte().toLong()
            val b4 = readUnsignedByte().toLong()
            return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        fun readLong(): Long {
            val bytes = readBytes(8)
            // Use ByteBuffer for reliable big-endian conversion
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).long
        }
    }

    private fun Long.toIntChecked(context: String): Int {
        if (this < Int.MIN_VALUE || this > Int.MAX_VALUE) {
            throw CborException("$context value ($this) exceeds integer limits.")
        }
        return this.toInt()
    }
}


/*
// --- Example Usage ---

// 1. Define a custom type and its hooks
data class Point(val x: Int, val y: Int)
const val POINT_TAG = 1001L // Choose a custom tag number (>= 24 recommended for non-standard)

val customEnHook: (Any) -> Pair<Long, Any>? = { item ->
    when (item) {
        is Point -> POINT_TAG to listOf(item.x, item.y) // Encode Point as Tag(1001) followed by a 2-element array [x, y]
        else -> null // Let standard encoder handle other types
    }
}

val customDeHook: (Long, Any?) -> Any? = { tag, decodedItem ->
    when (tag) {
        POINT_TAG -> {
            if (decodedItem is List<*> && decodedItem.size == 2 && decodedItem[0] is Long && decodedItem[1] is Long) {
                // Convert Long back to Int safely if needed, assuming they fit
                val x = (decodedItem[0] as Long).toInt()
                val y = (decodedItem[1] as Long).toInt()
                Point(x, y) // Reconstruct the Point object
            } else {
                throw CborException("Invalid data for Point tag ($POINT_TAG): expected List<Long>(size=2), got $decodedItem")
            }
        }
        else -> decodedItem // Pass through other tags or untagged data
    }
}

fun main() {
    // --- Interactor without hooks ---
    val standardInteractor = CborInteractor()

    val dataSimple: Map<Any?, Any?> = mapOf(
        "a" to 1,
        "b" to true,
        "c" to null,
        "d" to listOf(10L, 20.5, "hello".toByteArray()),
        "e" to -100,
        "f" to 123.45f,
        "g" to Instant.now(),
        "h" to BigInteger("18446744073709551616"), // 2^64
        "i" to CborUndefined,
        "j" to mutableMapOf(
            "key1" to "value1",
            "key2" to listOf(1, 2, 3),
            "key3" to mapOf("nestedKey" to "nestedValue")
        )
    )

    println("Original Simple Data: $dataSimple")
    val encodedSimple = standardInteractor.encode(dataSimple)
    println("Encoded Simple CBOR (hex): ${encodedSimple.joinToString("") { "%02x".format(it) }}")
    val decodedSimple = standardInteractor.decode(encodedSimple)
    println("Decoded Simple Data: $decodedSimple")
    println("-".repeat(30))


    // --- Interactor with custom hooks for Point ---
    val customInteractor = CborInteractor(enHook = customEnHook, deHook = customDeHook)

    val dataCustom = mapOf(
        "p1" to Point(10, 20),
        "label" to "A custom point",
        "other_data" to listOf(1, 2, 3)
    )

    println("Original Custom Data: $dataCustom")
    val encodedCustom = customInteractor.encode(dataCustom)
    println("Encoded Custom CBOR (hex): ${encodedCustom.joinToString("") { "%02x".format(it) }}")
    val decodedCustom = customInteractor.decode(encodedCustom)
    println("Decoded Custom Data: $decodedCustom")
    println("-".repeat(30))

    // --- Example: Decoding Indefinite Length Array ---
    // Manual construction of indefinite array [1, 2, 3]
    // 0x9f (array indefinite) 0x01 (1) 0x02 (2) 0x03 (3) 0xff (break)
    val indefiniteArrayBytes = byteArrayOf(0x9f.toByte(), 0x01, 0x02, 0x03, 0xff.toByte())
    println("Decoding Indefinite Array (hex): ${indefiniteArrayBytes.joinToString("") { "%02x".format(it) }}")
    val decodedIndefiniteArray = standardInteractor.decode(indefiniteArrayBytes)
    println("Decoded Indefinite Array: $decodedIndefiniteArray")
    println("-".repeat(30))

    // --- Example: Decoding Indefinite Length Text String ---
    // Manual construction of indefinite text string "Hello" "World"
    // 0x7f (string indefinite) 0x65 (5 bytes) H e l l o  0x65 (5 bytes) W o r l d 0xff (break)
    val indefiniteStringBytes = byteArrayOf(
        0x7f.toByte(), // Indefinite Text String
        0x65.toByte(), // Text chunk length 5
        'H'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte(),
        0x65.toByte(), // Text chunk length 5
        'W'.code.toByte(), 'o'.code.toByte(), 'r'.code.toByte(), 'l'.code.toByte(), 'd'.code.toByte(),
        0xff.toByte() // Break
    )
    println("Decoding Indefinite String (hex): ${indefiniteStringBytes.joinToString("") { "%02x".format(it) }}")
    val decodedIndefiniteString = standardInteractor.decode(indefiniteStringBytes)
    println("Decoded Indefinite String: $decodedIndefiniteString")
}
*/