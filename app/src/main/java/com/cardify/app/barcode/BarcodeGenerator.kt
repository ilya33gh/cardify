package com.cardify.app.barcode

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap

object BarcodeGenerator {

    // High-performance LRU memory cache for instant barcode rendering
    private val memoryCache = LruCache<String, Bitmap>(100)
    private val preloadScope = CoroutineScope(Dispatchers.Default)

    private fun cacheKey(content: String, format: BarcodeFormatEnum, width: Int, height: Int): String {
        return "${format.name}_${content}_${width}x${height}"
    }

    fun getCachedBitmap(
        content: String,
        format: BarcodeFormatEnum,
        width: Int = 1000,
        height: Int = if (format.is2D) 1000 else 380
    ): Bitmap? {
        if (content.isBlank()) return null
        return memoryCache.get(cacheKey(content, format, width, height))
    }

    fun preloadBarcode(content: String, format: BarcodeFormatEnum) {
        if (content.isBlank()) return
        preloadScope.launch {
            // Preload standard size
            generateBarcodeBitmap(content, format, 1000, if (format.is2D) 1000 else 380)
            // Preload fullscreen size
            generateBarcodeBitmap(content, format, 1200, if (format.is2D) 1200 else 420)
        }
    }

    suspend fun generateBarcodeBitmap(
        content: String,
        format: BarcodeFormatEnum,
        width: Int = 1000,
        height: Int = if (format.is2D) 1000 else 380
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        if (content.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Содержимое кода не может быть пустым"))
        }

        val key = cacheKey(content, format, width, height)
        memoryCache.get(key)?.let { cached ->
            return@withContext Result.success(cached)
        }

        val zxingFormat = mapToZXingFormat(format)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, if (format.is2D) 1 else 2)
            if (format == BarcodeFormatEnum.QR_CODE) {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            }
        }

        try {
            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(content, zxingFormat, width, height, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            memoryCache.put(key, bitmap)
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun mapToZXingFormat(format: BarcodeFormatEnum): BarcodeFormat {
        return when (format) {
            BarcodeFormatEnum.EAN_13 -> BarcodeFormat.EAN_13
            BarcodeFormatEnum.EAN_8 -> BarcodeFormat.EAN_8
            BarcodeFormatEnum.CODE_128 -> BarcodeFormat.CODE_128
            BarcodeFormatEnum.CODE_39 -> BarcodeFormat.CODE_39
            BarcodeFormatEnum.CODE_93 -> BarcodeFormat.CODE_93
            BarcodeFormatEnum.UPC_A -> BarcodeFormat.UPC_A
            BarcodeFormatEnum.UPC_E -> BarcodeFormat.UPC_E
            BarcodeFormatEnum.CODABAR -> BarcodeFormat.CODABAR
            BarcodeFormatEnum.ITF -> BarcodeFormat.ITF
            BarcodeFormatEnum.QR_CODE -> BarcodeFormat.QR_CODE
            BarcodeFormatEnum.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
            BarcodeFormatEnum.AZTEC -> BarcodeFormat.AZTEC
            BarcodeFormatEnum.PDF_417 -> BarcodeFormat.PDF_417
        }
    }

    fun mapFromMLKitFormat(mlKitFormat: Int): BarcodeFormatEnum {
        return when (mlKitFormat) {
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> BarcodeFormatEnum.EAN_13
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> BarcodeFormatEnum.EAN_8
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> BarcodeFormatEnum.CODE_128
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> BarcodeFormatEnum.CODE_39
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93 -> BarcodeFormatEnum.CODE_93
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> BarcodeFormatEnum.UPC_A
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E -> BarcodeFormatEnum.UPC_E
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR -> BarcodeFormatEnum.CODABAR
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF -> BarcodeFormatEnum.ITF
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> BarcodeFormatEnum.QR_CODE
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX -> BarcodeFormatEnum.DATA_MATRIX
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC -> BarcodeFormatEnum.AZTEC
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417 -> BarcodeFormatEnum.PDF_417
            else -> BarcodeFormatEnum.CODE_128
        }
    }

    /**
     * Validates if the content string is valid for the specified barcode format.
     * Returns null if valid, or a human-readable localized error message if invalid.
     */
    fun validateBarcode(content: String, format: BarcodeFormatEnum): String? {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            return "Введите номер карты или штрихкод"
        }

        // Format-specific length/content pre-validation for friendly user feedback
        when (format) {
            BarcodeFormatEnum.EAN_13 -> {
                if (!trimmed.all { it.isDigit() }) return "Для формата EAN-13 разрешены только цифры"
                if (trimmed.length !in 12..13) return "Для формата EAN-13 нужно ввести 12 или 13 цифр (введено: ${trimmed.length})"
            }
            BarcodeFormatEnum.EAN_8 -> {
                if (!trimmed.all { it.isDigit() }) return "Для формата EAN-8 разрешены только цифры"
                if (trimmed.length !in 7..8) return "Для формата EAN-8 нужно ввести 7 или 8 цифр (введено: ${trimmed.length})"
            }
            BarcodeFormatEnum.UPC_A -> {
                if (!trimmed.all { it.isDigit() }) return "Для формата UPC-A разрешены только цифры"
                if (trimmed.length !in 11..12) return "Для формата UPC-A нужно ввести 11 или 12 цифр (введено: ${trimmed.length})"
            }
            BarcodeFormatEnum.UPC_E -> {
                if (!trimmed.all { it.isDigit() }) return "Для формата UPC-E разрешены только цифры"
                if (trimmed.length !in 7..8) return "Для формата UPC-E нужно ввести 7 или 8 цифр (введено: ${trimmed.length})"
            }
            BarcodeFormatEnum.ITF -> {
                if (!trimmed.all { it.isDigit() }) return "Для формата ITF разрешены только цифры"
                if (trimmed.length % 2 != 0) return "Для формата ITF нужно чётное количество цифр (введено: ${trimmed.length})"
            }
            else -> {}
        }

        val zxingFormat = mapToZXingFormat(format)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, if (format.is2D) 1 else 2)
            if (format == BarcodeFormatEnum.QR_CODE) {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            }
        }

        return try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(trimmed, zxingFormat, 100, if (format.is2D) 100 else 40, hints)
            if (matrix.width <= 0 || matrix.height <= 0) {
                "Не удалось сгенерировать штрихкод. Проверьте введенные данные."
            } else {
                null // Valid!
            }
        } catch (e: Exception) {
            when (format) {
                BarcodeFormatEnum.EAN_13 -> "Неверный номер карты: проверьте правильность введенных цифр"
                BarcodeFormatEnum.EAN_8 -> "Неверный номер карты: проверьте правильность введенных цифр"
                BarcodeFormatEnum.UPC_A -> "Неверный номер карты: проверьте правильность введенных цифр"
                BarcodeFormatEnum.UPC_E -> "Неверный номер карты: проверьте правильность введенных цифр"
                BarcodeFormatEnum.ITF -> "Неверный номер карты: проверьте чётность количества цифр"
                BarcodeFormatEnum.CODABAR -> "Недопустимые символы для формата Codabar"
                BarcodeFormatEnum.CODE_39 -> "Недопустимые символы для формата Code 39"
                BarcodeFormatEnum.CODE_93 -> "Недопустимые символы для формата Code 93"
                else -> "Не удалось создать штрихкод. Проверьте правильность номера карты"
            }
        }
    }
}
