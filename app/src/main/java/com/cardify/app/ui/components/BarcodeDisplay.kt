package com.cardify.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.barcode.BarcodeGenerator
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import kotlinx.coroutines.delay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import com.cardify.app.ui.theme.CardNumberFontFamily

import androidx.compose.foundation.BorderStroke

@Composable
fun BarcodeDisplay(
    value: String,
    format: BarcodeFormatEnum,
    modifier: Modifier = Modifier,
    showValueText: Boolean = true,
    containerColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null
) {
    var bitmap by remember(value, format) { mutableStateOf(BarcodeGenerator.getCachedBitmap(value, format)) }
    var errorMessage by remember(value, format) { mutableStateOf<String?>(null) }
    var isLoading by remember(value, format) { mutableStateOf(bitmap == null) }

    LaunchedEffect(value, format) {
        val cached = BarcodeGenerator.getCachedBitmap(value, format)
        if (cached != null) {
            bitmap = cached
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        val result = BarcodeGenerator.generateBarcodeBitmap(value, format)
        result.onSuccess {
            bitmap = it
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Ошибка генерации кода"
            isLoading = false
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        color = containerColor,
        shape = shape,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .height(if (format.is2D) 200.dp else 105.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveMorphLoadingIndicator(
                            size = 44.dp
                        )
                    }
                } else if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Barcode $value",
                        modifier = Modifier
                            .fillMaxWidth(if (format.is2D) 0.78f else 1f)
                            .height(if (format.is2D) 200.dp else 105.dp),
                        contentScale = if (format.is2D) ContentScale.Fit else ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(if (format.is2D) 200.dp else 105.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Не удалось сгенерировать штрихкод",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (showValueText && value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatCardNumber(value, format),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CardNumberFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

fun formatCardNumber(value: String, format: BarcodeFormatEnum): String {
    if (format.is2D || value.length > 24) return value
    val trimmed = value.trim()
    if (trimmed.contains(" ") || trimmed.contains("-")) {
        return trimmed.replace(Regex("\\s+"), " ")
    }
    if (trimmed.all { it.isDigit() }) {
        return trimmed.chunked(4).joinToString(" ")
    }
    return trimmed
}
