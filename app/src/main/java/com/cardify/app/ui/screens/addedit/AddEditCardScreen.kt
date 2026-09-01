package com.cardify.app.ui.screens.addedit

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cardify.app.R
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.ui.components.AnimatedFavoriteIconButton
import com.cardify.app.ui.components.BarcodeDisplay
import com.cardify.app.ui.components.ColorPickerRow
import com.cardify.app.ui.components.GooglePillChip
import com.cardify.app.ui.components.M3ExpressiveCollapsingHeader
import com.cardify.app.ui.components.getCategoryIcon
import com.cardify.app.ui.components.getLocalizedCategoryRes
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.ExpressiveButtonShape
import com.cardify.app.ui.theme.GoogleSansFlexCardTitle
import com.cardify.app.ui.theme.GoogleSansFlexSlantedHint
import com.cardify.app.ui.theme.MaterialThemeAdaptive
import com.cardify.app.ui.theme.ManropeFamily
import com.cardify.app.ui.theme.OnestFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    viewModel: AddEditCardViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val windowSizeInfo = MaterialThemeAdaptive

    val dynamicCardColor = CardColorPalette.getAdaptiveColor(uiState.selectedColorHex)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    val hapticHelper = rememberHapticHelper()

    LaunchedEffect(uiState.errorTimestamp) {
        if (uiState.errorTimestamp > 0L) {
            uiState.errorMessage?.let { msg ->
                hapticHelper.performDestructiveWarning()
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val maxCollapsePx = with(density) { 88.dp.toPx() }
    val collapseFraction by remember {
        derivedStateOf {
            (scrollState.value.toFloat() / maxCollapsePx).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        val adaptiveHorizontalPadding = windowSizeInfo.horizontalPadding

        if (windowSizeInfo.isWideScreen) {
            // Two-Pane Split Layout for Foldables, Tablets, and Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarHeight + 56.dp, bottom = 12.dp)
                    .padding(horizontal = adaptiveHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Sticky Pane: Card Preview
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(88.dp * (1f - collapseFraction)))
                        CardPreviewCard(
                            title = uiState.title,
                            barcodeValue = uiState.barcodeValue,
                            barcodeFormat = uiState.barcodeFormat,
                            dynamicCardColor = dynamicCardColor,
                            selectedColorHex = uiState.selectedColorHex,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Right Scrollable Pane: Form Fields
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(88.dp))
                    CardFormInputs(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        } else {
            // Single-Column Layout for standard Phones
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(horizontal = adaptiveHorizontalPadding)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Expanded Space
                    Spacer(modifier = Modifier.height(statusBarHeight + 56.dp + 88.dp))

                    // Preview Card
                    CardPreviewCard(
                        title = uiState.title,
                        barcodeValue = uiState.barcodeValue,
                        barcodeFormat = uiState.barcodeFormat,
                        dynamicCardColor = dynamicCardColor,
                        selectedColorHex = uiState.selectedColorHex,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Form inputs
                    CardFormInputs(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }

        // Pinned Collapsing Header on top
        M3ExpressiveCollapsingHeader(
            title = if (uiState.cardId > 0)
                stringResource(R.string.edit_card_title)
            else
                stringResource(R.string.new_card_title),
            onNavigateBack = onNavigateBack,
            collapseFraction = collapseFraction,
            modifier = Modifier.align(Alignment.TopCenter),
            actions = {
                AnimatedFavoriteIconButton(
                    isFavorite = uiState.isFavorite,
                    onToggle = { viewModel.onFavoriteToggle() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        )

        // Floating Action Button
        LargeFloatingActionButton(
            onClick = {
                hapticHelper.performHeavyClick()
                viewModel.saveCard()
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = windowSizeInfo.horizontalPadding, bottom = 24.dp)
                .size(68.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = if (uiState.cardId > 0)
                    stringResource(R.string.save_action)
                else
                    stringResource(R.string.add_to_wallet_action),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CardPreviewCard(
    title: String,
    barcodeValue: String,
    barcodeFormat: BarcodeFormatEnum,
    dynamicCardColor: Color,
    selectedColorHex: String,
    modifier: Modifier = Modifier
) {
    val cardContentColor = CardColorPalette.getCardContentColor(selectedColorHex)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = dynamicCardColor,
        contentColor = cardContentColor,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.ifBlank { stringResource(R.string.card_preview_title) },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlexCardTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = cardContentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            val displayBarcodeValue = barcodeValue.ifBlank { "123456789012" }
            BarcodeDisplay(
                value = displayBarcodeValue,
                format = barcodeFormat,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CardFormInputs(
    uiState: AddEditCardUiState,
    viewModel: AddEditCardViewModel
) {
    var isFormatDropdownOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val titleRequester = remember { BringIntoViewRequester() }
    val barcodeRequester = remember { BringIntoViewRequester() }
    val notesRequester = remember { BringIntoViewRequester() }

    // Card Title Field
    OutlinedTextField(
        value = uiState.title,
        onValueChange = { viewModel.onTitleChanged(it) },
        label = { Text(stringResource(R.string.card_title_label)) },
        placeholder = {
            Text(
                stringResource(R.string.card_title_placeholder),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlexSlantedHint)
            )
        },
        singleLine = true,
        shape = ExpressiveButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(titleRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        delay(120)
                        titleRequester.bringIntoView()
                    }
                }
            }
    )

    // Barcode Number / Value Field
    OutlinedTextField(
        value = uiState.barcodeValue,
        onValueChange = { viewModel.onBarcodeValueChanged(it) },
        label = { Text(stringResource(R.string.card_code_label)) },
        placeholder = {
            Text(
                stringResource(R.string.card_code_placeholder),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlexSlantedHint)
            )
        },
        singleLine = true,
        shape = ExpressiveButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(barcodeRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        delay(120)
                        barcodeRequester.bringIntoView()
                    }
                }
            }
    )

    // Barcode Format Dropdown
    ExposedDropdownMenuBox(
        expanded = isFormatDropdownOpen,
        onExpandedChange = { isFormatDropdownOpen = it }
    ) {
        OutlinedTextField(
            value = uiState.barcodeFormat.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.code_type_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFormatDropdownOpen) },
            shape = ExpressiveButtonShape,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        )

        ExposedDropdownMenu(
            expanded = isFormatDropdownOpen,
            onDismissRequest = { isFormatDropdownOpen = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            BarcodeFormatEnum.entries.forEach { format ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = format.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = OnestFamily,
                                fontWeight = if (uiState.barcodeFormat == format) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 17.sp
                            )
                        )
                    },
                    onClick = {
                        viewModel.onBarcodeFormatChanged(format)
                        isFormatDropdownOpen = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }

    // Category Selection using GooglePillChip
    Text(
        text = stringResource(R.string.category_label),
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        uiState.categories.forEach { category ->
            val isSelected = uiState.selectedCategoryId == category.id
            val localizedRes = getLocalizedCategoryRes(category.name)
            val displayName = if (localizedRes != null) stringResource(localizedRes) else category.name

            GooglePillChip(
                isSelected = isSelected,
                onClick = { viewModel.onCategorySelected(if (isSelected) null else category.id) },
                label = displayName,
                icon = getCategoryIcon(category.iconName)
            )
        }
    }

    // Card Color Palette Picker
    Text(
        text = stringResource(R.string.card_color_label),
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false
    )

    ColorPickerRow(
        selectedHex = uiState.selectedColorHex,
        onSelectHex = { viewModel.onColorSelected(it) }
    )

    // Optional Notes Field
    OutlinedTextField(
        value = uiState.notes,
        onValueChange = { viewModel.onNotesChanged(it) },
        label = { Text(stringResource(R.string.note_optional_label)) },
        placeholder = {
            Text(
                stringResource(R.string.note_placeholder),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlexSlantedHint)
            )
        },
        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null) },
        minLines = 2,
        maxLines = 4,
        shape = ExpressiveButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(notesRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        delay(120)
                        notesRequester.bringIntoView()
                    }
                }
            }
    )
}
