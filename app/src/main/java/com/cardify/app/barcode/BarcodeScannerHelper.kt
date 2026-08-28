package com.cardify.app.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

data class ScannedBarcodeResult(
    val rawValue: String,
    val format: BarcodeFormatEnum
)

object BarcodeScannerHelper {

    private val supportedFormats = listOf(
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODABAR,
        BarcodeFormat.ITF,
        BarcodeFormat.QR_CODE,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.AZTEC,
        BarcodeFormat.PDF_417
    )

    private fun createReader(): MultiFormatReader {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.POSSIBLE_FORMATS, supportedFormats)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }
        return MultiFormatReader().apply { setHints(hints) }
    }

    fun processImageProxyAsync(
        imageProxy: ImageProxy,
        onResult: (ScannedBarcodeResult?) -> Unit
    ) {
        try {
            val result = decodeImageProxy(imageProxy)
            onResult(result)
        } catch (e: Exception) {
            onResult(null)
        } finally {
            imageProxy.close()
        }
    }

    suspend fun processImageProxy(imageProxy: ImageProxy): ScannedBarcodeResult? = withContext(Dispatchers.Default) {
        try {
            decodeImageProxy(imageProxy)
        } catch (e: Exception) {
            null
        } finally {
            imageProxy.close()
        }
    }

    private fun decodeImageProxy(imageProxy: ImageProxy): ScannedBarcodeResult? {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()
        val rotatedBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        } else {
            bitmap
        }
        return decodeBitmapInternal(rotatedBitmap)
    }

    suspend fun processUri(context: Context, uri: Uri): ScannedBarcodeResult? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                processBitmap(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun processBitmap(bitmap: Bitmap): ScannedBarcodeResult? = withContext(Dispatchers.Default) {
        decodeBitmapInternal(bitmap)
    }

    private fun decodeBitmapInternal(bitmap: Bitmap): ScannedBarcodeResult? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = createReader()

        // 1. Try HybridBinarizer (Standard)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val zxingResult = reader.decodeWithState(binaryBitmap)
            val format = BarcodeGenerator.mapFromZXingFormat(zxingResult.barcodeFormat)
            return ScannedBarcodeResult(zxingResult.text, format)
        } catch (e: Exception) {
            reader.reset()
        }

        // 2. Try GlobalHistogramBinarizer (Enhanced for 1D barcodes / low contrast)
        try {
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val zxingResult = reader.decodeWithState(binaryBitmap)
            val format = BarcodeGenerator.mapFromZXingFormat(zxingResult.barcodeFormat)
            return ScannedBarcodeResult(zxingResult.text, format)
        } catch (e: Exception) {
            reader.reset()
        }

        // 3. Try Inverted (for white-on-black barcodes)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
            val zxingResult = reader.decodeWithState(binaryBitmap)
            val format = BarcodeGenerator.mapFromZXingFormat(zxingResult.barcodeFormat)
            return ScannedBarcodeResult(zxingResult.text, format)
        } catch (e: Exception) {
            reader.reset()
        }

        return null
    }
}
