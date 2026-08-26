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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
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
import com.cardify.app.ui.theme.InterFamily
import com.cardify.app.ui.theme.PillShape
import com.cardify.app.ui.theme.SpaceGroteskFamily

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
    val copiedToastText = stringResource(R.string.copied_toast)

    // Override screen brightness to 100% while dialog is open
    DisposableEffect(Unit) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val act = (context as? Activity)
        val actWindow = act?.window

        val origActBrightness = actWindow?.attributes?.screenBrightness
        val origDialogBrightness = dialogWindow?.attributes?.screenBrightness

        dialogWindow?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            win.attributes = lp
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
        }
        actWindow?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            win.attributes = lp
        }

        onDispose {
            origDialogBrightness?.let { b ->
                dialogWindow?.let { win ->
                    val lp = win.attributes
                    lp.screenBrightness = b
                    win.attributes = lp
                }
            }
            origActBrightness?.let { b ->
                actWindow?.let { win ->
                    val lp = win.attributes
                    lp.screenBrightness = b
                    win.attributes = lp
                }
            }
        }
    }

    var bitmap by remember(card.barcodeValue, card.barcodeFormat) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(card.barcodeValue, card.barcodeFormat) { mutableStateOf(true) }
    var errorMessage by remember(card.barcodeValue, card.barcodeFormat) { mutableStateOf<String?>(null) }

    LaunchedEffect(card.barcodeValue, card.barcodeFormat) {
        isLoading = true
        errorMessage = null
        val res = BarcodeGenerator.generateBarcodeBitmap(
            content = card.barcodeValue,
            format = card.barcodeFormat,
            width = if (card.barcodeFormat.is2D) 1600 else 2000,
            height = if (card.barcodeFormat.is2D) 1600 else 900
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
                    .windowInsetsPadding(WindowInsets.safeDrawing),
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
                                imageVector = Icons.Default.Close,
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
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Black,
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
                                    fontFamily = InterFamily,
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
                                imageVector = Icons.Outlined.BrightnessHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFamily,
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
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(48.dp)
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
                            // 1D Linear Barcode: Rotated 90 degrees to stretch across full screen height
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = card.barcodeValue,
                                modifier = Modifier
                                    .size(width = availHeight - 16.dp, height = availWidth - 16.dp)
                                    .graphicsLayer {
                                        rotationZ = 90f
                                    },
                                contentScale = ContentScale.FillBounds
                            )
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
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 28.dp),
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
                        modifier = Modifier.fillMaxWidth()
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
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (card.barcodeValue.length > 20) 18.sp else 22.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.tap_to_close_hint),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFamily,
                            color = Color(0xFF888888)
                        )
                    )
                }
            }
        }
    }
}
