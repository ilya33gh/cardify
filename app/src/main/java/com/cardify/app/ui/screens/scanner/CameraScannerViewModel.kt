package com.cardify.app.ui.screens.scanner

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.barcode.BarcodeScannerHelper
import com.cardify.app.barcode.ScannedBarcodeResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ScannerNavigationEvent {
    data class OnBarcodeScanned(val value: String, val formatName: String) : ScannerNavigationEvent
    data class ShowToast(val message: String) : ScannerNavigationEvent
}

class CameraScannerViewModel : ViewModel() {

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _navigationEvents = MutableSharedFlow<ScannerNavigationEvent>()
    val navigationEvents: SharedFlow<ScannerNavigationEvent> = _navigationEvents

    private var hasScanned = false

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun onBarcodeDetected(result: ScannedBarcodeResult, context: Context) {
        if (hasScanned) return
        hasScanned = true
        triggerHapticFeedback(context)
        viewModelScope.launch {
            _navigationEvents.emit(
                ScannerNavigationEvent.OnBarcodeScanned(
                    value = result.rawValue,
                    formatName = result.format.name
                )
            )
        }
    }

    fun processImageUri(context: Context, uri: Uri) {
        if (_isProcessing.value) return
        _isProcessing.value = true

        viewModelScope.launch {
            val result = BarcodeScannerHelper.processUri(context, uri)
            _isProcessing.value = false

            if (result != null) {
                triggerHapticFeedback(context)
                _navigationEvents.emit(
                    ScannerNavigationEvent.OnBarcodeScanned(
                        value = result.rawValue,
                        formatName = result.format.name
                    )
                )
            } else {
                _navigationEvents.emit(
                    ScannerNavigationEvent.ShowToast("Штрихкод на изображении не обнаружен")
                )
            }
        }
    }

    fun resetScanner() {
        hasScanned = false
        _isProcessing.value = false
    }

    private fun triggerHapticFeedback(context: Context) {
        if (!com.cardify.app.ui.components.HapticPreference.isHapticEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }
}
