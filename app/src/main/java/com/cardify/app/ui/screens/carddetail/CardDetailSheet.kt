package com.cardify.app.ui.screens.carddetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import com.cardify.app.R
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.components.AnimatedFavoriteIconButton
import com.cardify.app.ui.components.BarcodeDisplay
import com.cardify.app.ui.components.ExpressiveBrightnessSlider
import com.cardify.app.ui.components.FullScreenBarcodeDialog
import com.cardify.app.ui.components.getLocalizedCategoryRes
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: LoyaltyCard,
    onDismiss: () -> Unit,
    onEditCard: (Long) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val hapticHelper = rememberHapticHelper()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullScreenBarcode by remember { mutableStateOf(false) }

    LaunchedEffect(showDeleteConfirm) {
        if (showDeleteConfirm) {
            hapticHelper.performDestructiveWarning()
        }
    }

    val baseCardColor = CardColorPalette.getHarmonizedColor(card.colorHex)

    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val isOled = MaterialTheme.colorScheme.surface == Color.Black

    // Dynamic Muted Bottom Sheet Background matching Card Color & System Theme
    val sheetContainerColor = remember(baseCardColor, surfaceContainerColor, isDark, isOled) {
        if (isOled) {
            Color(ColorUtils.blendARGB(baseCardColor.toArgb(), android.graphics.Color.BLACK, 0.94f))
        } else {
            val targetArgb = surfaceContainerColor.toArgb()
            val blendFraction = if (isDark) 0.88f else 0.92f
            Color(ColorUtils.blendARGB(baseCardColor.toArgb(), targetArgb, blendFraction))
        }
    }

    val windowSizeInfo = MaterialThemeAdaptive

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = BottomSheetTopShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 620.dp)
                .padding(horizontal = windowSizeInfo.horizontalPadding)
                .padding(top = 10.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Capsule Header (if present)
            if (!card.categoryName.isNullOrBlank()) {
                val localizedRes = getLocalizedCategoryRes(card.categoryName)
                val displayCatName = if (localizedRes != null) stringResource(localizedRes) else card.categoryName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(0f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = PillShape,
                        color = baseCardColor,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = displayCatName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = OnestFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Merchant / Card Title (Left) + Favorite Squircle Button (Right) in One Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(14.dp))

                AnimatedFavoriteIconButton(
                    isFavorite = card.isFavorite,
                    onToggle = onToggleFavorite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Barcode Display Surface (Holds barcode image & formatted number inside) - Tapping opens Fullscreen POS Mode
            BarcodeDisplay(
                value = card.barcodeValue,
                format = card.barcodeFormat,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    hapticHelper.performClick()
                    showFullScreenBarcode = true
                }
            )

            // Optional Notes Section
            if (card.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = null
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.note_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = card.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brightness Control Slider (Photo 6 Capsule Design)
            ExpressiveBrightnessSlider(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            val copiedToastText = stringResource(R.string.copied_toast)
            val shareTitleText = stringResource(R.string.share_title)

            // Centered Floating Bottom Action Bar (Photo 5: Enlarged, Centered)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill Container: Action icons (Copy, Share, Edit) with fixed compact spacing
                Surface(
                    modifier = Modifier.height(68.dp),
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy Action
                        IconButton(
                            onClick = {
                                hapticHelper.performClick()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Cardify Code", card.barcodeValue)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_action),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Share Action
                        IconButton(
                            onClick = {
                                hapticHelper.performClick()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${card.title}: ${card.barcodeValue}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, shareTitleText))
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.share_action),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Edit Action (Keeps Detail Sheet open so it persists when returning from editing)
                        IconButton(
                            onClick = {
                                hapticHelper.performClick()
                                onEditCard(card.id)
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.edit_action),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Dedicated Delete Button (Photo 5: Monet Error Red Container)
                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        showDeleteConfirm = true
                    },
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_action),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.delete_card_dialog_title),
                    fontWeight = FontWeight.Black,
                    fontFamily = OnestFamily,
                    maxLines = 1,
                    softWrap = false
                )
            },
            text = {
                Text(stringResource(R.string.delete_card_dialog_text, card.title))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCard(card.id)
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.delete_action),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = OnestFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = stringResource(R.string.cancel_action),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = OnestFamily),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }

    // Fullscreen Barcode POS Overlay Dialog
    if (showFullScreenBarcode) {
        FullScreenBarcodeDialog(
            card = card,
            onDismiss = { showFullScreenBarcode = false }
        )
    }
}
