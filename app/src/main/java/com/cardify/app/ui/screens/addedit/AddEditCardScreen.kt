package com.cardify.app.ui.screens.addedit

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.ui.components.AnimatedFavoriteIconButton
import com.cardify.app.ui.components.BarcodeDisplay
import com.cardify.app.ui.components.ColorPickerRow
import com.cardify.app.ui.components.GooglePillChip
import com.cardify.app.ui.components.getCategoryIcon
import com.cardify.app.ui.components.getLocalizedCategoryRes
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.ExpressiveButtonShape
import com.cardify.app.ui.theme.SpaceGroteskFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    viewModel: AddEditCardViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isFormatDropdownOpen by remember { mutableStateOf(false) }

    val dynamicCardColor = remember(uiState.selectedColorHex) {
        CardColorPalette.getColor(uiState.selectedColorHex)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.cardId > 0)
                            stringResource(R.string.edit_card_title)
                        else
                            stringResource(R.string.new_card_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    AnimatedFavoriteIconButton(
                        isFavorite = uiState.isFavorite,
                        onToggle = { viewModel.onFavoriteToggle() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            val hapticHelper = rememberHapticHelper()
            LargeFloatingActionButton(
                onClick = {
                    hapticHelper.performHeavyClick()
                    viewModel.saveCard()
                },
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (uiState.cardId > 0)
                        stringResource(R.string.save_action)
                    else
                        stringResource(R.string.add_to_wallet_action),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permanent Dynamic Barcode Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = dynamicCardColor,
                contentColor = Color.White,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.title.ifBlank { stringResource(R.string.card_preview_title) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val displayBarcodeValue = uiState.barcodeValue.ifBlank { "123456789012" }
                    BarcodeDisplay(
                        value = displayBarcodeValue,
                        format = uiState.barcodeFormat,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Card Title Field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(stringResource(R.string.card_title_label)) },
                placeholder = { Text(stringResource(R.string.card_title_placeholder)) },
                singleLine = true,
                shape = ExpressiveButtonShape,
                modifier = Modifier.fillMaxWidth()
            )

            // Barcode Number / Value Field
            OutlinedTextField(
                value = uiState.barcodeValue,
                onValueChange = { viewModel.onBarcodeValueChanged(it) },
                label = { Text(stringResource(R.string.card_code_label)) },
                placeholder = { Text(stringResource(R.string.card_code_placeholder)) },
                singleLine = true,
                shape = ExpressiveButtonShape,
                modifier = Modifier.fillMaxWidth()
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
                                    fontWeight = if (uiState.barcodeFormat == format) FontWeight.Bold else FontWeight.Normal
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
                    fontFamily = SpaceGroteskFamily,
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
                    fontFamily = SpaceGroteskFamily,
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
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                minLines = 2,
                maxLines = 4,
                shape = ExpressiveButtonShape,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
