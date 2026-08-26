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
import androidx.core.graphics.ColorUtils
import com.cardify.app.R
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.components.BarcodeDisplay
import com.cardify.app.ui.components.ExpressiveBrightnessSlider
import com.cardify.app.ui.components.SmartUsagePulseBadge
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: LoyaltyCard,
    onDismiss: () -> Unit,
    onEditCard: (Long) -> Unit,
    onDeleteCard: (Long) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val hapticHelper = rememberHapticHelper()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(showDeleteConfirm) {
        if (showDeleteConfirm) {
            hapticHelper.performDestructiveWarning()
        }
    }

    val baseCardColor = remember(card.colorHex) {
        CardColorPalette.getColor(card.colorHex)
    }

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

    // High Contrast, Distinct Card-Dependent Action Buttons (Never blending with sheet background)
    val copyButtonBg = remember(baseCardColor, isDark) {
        val targetRole = if (isDark) android.graphics.Color.parseColor("#172E54") else android.graphics.Color.parseColor("#D3E3FD")
        Color(ColorUtils.blendARGB(baseCardColor.toArgb(), targetRole, 0.65f))
    }
    val copyButtonContent = remember(baseCardColor, isDark) {
        if (isDark) Color(0xFFDBE8FE) else Color(0xFF041E49)
    }

    val shareButtonBg = remember(baseCardColor, isDark) {
        val targetRole = if (isDark) android.graphics.Color.parseColor("#0E3B36") else android.graphics.Color.parseColor("#CCF8F0")
        Color(ColorUtils.blendARGB(baseCardColor.toArgb(), targetRole, 0.65f))
    }
    val shareButtonContent = remember(baseCardColor, isDark) {
        if (isDark) Color(0xFFCCFBF1) else Color(0xFF043832)
    }

    val editButtonBg = remember(baseCardColor, isDark) {
        val targetRole = if (isDark) android.graphics.Color.parseColor("#3C1852") else android.graphics.Color.parseColor("#F3E0FD")
        Color(ColorUtils.blendARGB(baseCardColor.toArgb(), targetRole, 0.65f))
    }
    val editButtonContent = remember(baseCardColor, isDark) {
        if (isDark) Color(0xFFF3E8FF) else Color(0xFF3B0764)
    }

    val deleteButtonBg = remember(baseCardColor, isDark) {
        val targetRole = if (isDark) android.graphics.Color.parseColor("#501923") else android.graphics.Color.parseColor("#FCE0E3")
        Color(ColorUtils.blendARGB(baseCardColor.toArgb(), targetRole, 0.65f))
    }
    val deleteButtonContent = remember(baseCardColor, isDark) {
        if (isDark) Color(0xFFFFE4E6) else Color(0xFF881337)
    }

    // Split Button Geometries: 0dp inner corner radius on joining seam!
    val leftSegmentShape = remember {
        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    }
    val rightSegmentShape = remember {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Capsule Header & Smart Usage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!card.categoryName.isNullOrBlank()) {
                    Surface(
                        shape = PillShape,
                        color = baseCardColor,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = card.categoryName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                SmartUsagePulseBadge(
                    isFavorite = card.isFavorite,
                    useCount = card.useCount
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Merchant / Card Title (Display Large in Space Grotesk Black)
            Text(
                text = card.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Barcode Display Surface (Holds barcode image & formatted number inside)
            BarcodeDisplay(
                value = card.barcodeValue,
                format = card.barcodeFormat,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            val copiedToastText = stringResource(R.string.copied_toast)
            val shareTitleText = stringResource(R.string.share_title)

            // Row 1: Copy & Share Split Button Pair (0dp seam corner radius)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy Segment (Left)
                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Cardify Code", card.barcodeValue)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                    },
                    shape = leftSegmentShape,
                    color = copyButtonBg,
                    contentColor = copyButtonContent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = copyButtonContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.copy_action),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = copyButtonContent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Share Segment (Right)
                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${card.title}: ${card.barcodeValue}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareTitleText))
                    },
                    shape = rightSegmentShape,
                    color = shareButtonBg,
                    contentColor = shareButtonContent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = shareButtonContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share_action),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = shareButtonContent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Edit & Delete Split Button Pair (0dp seam corner radius)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Segment (Left)
                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        onDismiss()
                        onEditCard(card.id)
                    },
                    shape = leftSegmentShape,
                    color = editButtonBg,
                    contentColor = editButtonContent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = editButtonContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.edit_action),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = editButtonContent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Delete Segment (Right)
                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        showDeleteConfirm = true
                    },
                    shape = rightSegmentShape,
                    color = deleteButtonBg,
                    contentColor = deleteButtonContent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = deleteButtonContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.delete_action),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = deleteButtonContent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Brightness Control Slider (Expressive capsule matching photo reference)
            ExpressiveBrightnessSlider(modifier = Modifier.fillMaxWidth())
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
                    fontFamily = InterFamily,
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
                            fontFamily = InterFamily,
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
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = InterFamily),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }
}
