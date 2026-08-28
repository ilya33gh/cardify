package com.cardify.app.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import com.cardify.app.ui.theme.CardNumberFontFamily
import com.cardify.app.ui.theme.GoogleSansFlexCardTitle
import com.cardify.app.ui.theme.OnestFamily
import com.cardify.app.ui.theme.PillShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.cardify.app.R
import com.cardify.app.barcode.BarcodeGenerator
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.theme.ManropeFamily
import com.cardify.app.ui.theme.OnestFamily
import com.cardify.app.ui.theme.PillShape

/**
 * Fullscreen Barcode Dialog (POS Mode)
 * Expands barcode across full screen height with maximum brightness and high-contrast pure white backdrop.
 * For 1D barcodes, rotates 90 degrees to span the full vertical screen height.
 * The formatted number is displayed clearly at the bottom with one-tap copy.
 */
@Composable
fun FullScreenBarcodeDialog(
    card: LoyaltyCard,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticHelper = rememberHapticHelper()
    val act = context as? Activity
    val actWindow = act?.window

    // Override screen brightness to 100% while dialog is open and restore accurately on dismiss
    DisposableEffect(actWindow) {
        val origActBrightness = actWindow?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        actWindow?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            win.attributes = lp
        }

        onDispose {
            actWindow?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = origActBrightness
                win.attributes = lp
            }
        }
    }

    val fullWidth = if (card.barcodeFormat.is2D) 1600 else 2000
    val fullHeight = if (card.barcodeFormat.is2D) 1600 else 900
    var bitmap by remember(card.barcodeValue, card.barcodeFormat) {
        mutableStateOf(BarcodeGenerator.getCachedBitmap(card.barcodeValue, card.barcodeFormat, fullWidth, fullHeight))
    }
    var isLoading by remember(card.barcodeValue, card.barcodeFormat) { mutableStateOf(bitmap == null) }
    var errorMessage by remember(card.barcodeValue, card.barcodeFormat) { mutableStateOf<String?>(null) }
    val copiedToastText = stringResource(R.string.copied_toast)

    LaunchedEffect(card.barcodeValue, card.barcodeFormat) {
        val cached = BarcodeGenerator.getCachedBitmap(card.barcodeValue, card.barcodeFormat, fullWidth, fullHeight)
        if (cached != null) {
            bitmap = cached
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        val res = BarcodeGenerator.generateBarcodeBitmap(
            content = card.barcodeValue,
            format = card.barcodeFormat,
            width = fullWidth,
            height = fullHeight
        )
        res.onSuccess {
            bitmap = it
            isLoading = false
        }.onFailure {
            errorMessage = it.localizedMessage ?: "Ошибка генерации"
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            var parent = dialogView.parent
            var dialogWindow: android.view.Window? = null
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    dialogWindow = parent.window
                    break
                }
                parent = parent.parent
            }
            dialogWindow?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                win.attributes = lp
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
            }
            onDispose { }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                },
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Header Row: Close Button (Left), Card Title (Center), 100% Brightness Badge (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            hapticHelper.performClick()
                            onDismiss()
                        },
                        shape = CircleShape,
                        color = Color(0xFFF0F0F0),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF1E1E1E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansFlexCardTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!card.categoryName.isNullOrBlank()) {
                            val localizedRes = getLocalizedCategoryRes(card.categoryName)
                            val displayCat = if (localizedRes != null) stringResource(localizedRes) else card.categoryName
                            Text(
                                text = displayCat,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF666666),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = PillShape,
                        color = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BrightnessHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // 2. Center Barcode Canvas: Stretched across full screen height
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val availWidth = maxWidth
                    val availHeight = maxHeight

                    if (isLoading) {
                        ExpressiveMorphLoadingIndicator(
                            size = 52.dp
                        )
                    } else if (bitmap != null) {
                        if (card.barcodeFormat.is2D) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = card.barcodeValue,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            val shouldRotate90 = availHeight > (availWidth * 1.15f)

                            if (shouldRotate90) {
                                // 1D Linear Barcode on Portrait Phones: Rotated 90 degrees to stretch across full vertical screen height
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = card.barcodeValue,
                                    modifier = Modifier
                                        .size(width = (availHeight - 20.dp).coerceAtLeast(100.dp), height = (availWidth - 20.dp).coerceAtLeast(60.dp))
                                        .graphicsLayer {
                                            rotationZ = 90f
                                        },
                                    contentScale = ContentScale.FillBounds
                                )
                            } else {
                                // 1D Linear Barcode on Tablets, Foldables, and Landscape: Display horizontally across full width
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = card.barcodeValue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 340.dp)
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        }
                    } else {
                        Text(
                            text = errorMessage ?: "Ошибка генерации",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 3. Bottom Number & Quick Action Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        onClick = {
                            hapticHelper.performClick()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cardify Code", card.barcodeValue)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF7F7F7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatCardNumber(card.barcodeValue, card.barcodeFormat),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = CardNumberFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (card.barcodeValue.length > 20) 18.sp else 22.sp,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
