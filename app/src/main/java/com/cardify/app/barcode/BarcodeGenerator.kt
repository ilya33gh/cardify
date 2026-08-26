package com.cardify.app.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

object BarcodeGenerator {

    suspend fun generateBarcodeBitmap(
        content: String,
        format: BarcodeFormatEnum,
        width: Int = 1000,
        height: Int = if (format.is2D) 1000 else 380
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        if (content.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Содержимое кода не может быть пустым"))
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
}
