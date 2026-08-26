package com.cardify.app.barcode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class ScannedBarcodeResult(
    val rawValue: String,
    val format: BarcodeFormatEnum
)

object BarcodeScannerHelper {

    private val allFormatsOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    fun getScanner(): BarcodeScanner {
        return BarcodeScanning.getClient(allFormatsOptions)
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxyAsync(
        imageProxy: ImageProxy,
        onResult: (ScannedBarcodeResult?) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(null)
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        getScanner().process(inputImage)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (first?.rawValue != null) {
                    val format = BarcodeGenerator.mapFromMLKitFormat(first.format)
                    onResult(ScannedBarcodeResult(first.rawValue!!, format))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun processImageProxy(imageProxy: ImageProxy): ScannedBarcodeResult? {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return null
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = getScanner()

        return suspendCancellableCoroutine { continuation ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first?.rawValue != null) {
                        val format = BarcodeGenerator.mapFromMLKitFormat(first.format)
                        continuation.resume(ScannedBarcodeResult(first.rawValue!!, format))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    suspend fun processUri(context: Context, uri: Uri): ScannedBarcodeResult? {
        val inputImage = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return null
        }
        val scanner = getScanner()

        return suspendCancellableCoroutine { continuation ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first?.rawValue != null) {
                        val format = BarcodeGenerator.mapFromMLKitFormat(first.format)
                        continuation.resume(ScannedBarcodeResult(first.rawValue!!, format))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    suspend fun processBitmap(bitmap: Bitmap): ScannedBarcodeResult? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val scanner = getScanner()

        return suspendCancellableCoroutine { continuation ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first?.rawValue != null) {
                        val format = BarcodeGenerator.mapFromMLKitFormat(first.format)
                        continuation.resume(ScannedBarcodeResult(first.rawValue!!, format))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
