package com.cardify.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.cardify.app.ui.theme.GoogleSansFlexSlantedBadge
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.cardify.app.ui.components.M3ExpressiveCollapsingHeader
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.data.local.ThemeMode
import com.cardify.app.domain.model.CardCategory
import com.cardify.app.ui.components.*
import com.cardify.app.ui.components.M3SettingsSegmentedSwitcher
import com.cardify.app.ui.components.SegmentItem
import com.cardify.app.ui.components.getCategoryIcon
import com.cardify.app.ui.components.getLocalizedCategoryRes
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Helper to compute M3 Expressive Connected Grouped Item Shapes (6.dp inner seams)
 */
private fun getM3GroupedItemShape(index: Int, totalCount: Int, cornerRadius: Dp = 24.dp, seamRadius: Dp = 6.dp): Shape {
    if (totalCount <= 1) return RoundedCornerShape(cornerRadius)
    return when (index) {
        0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius, bottomStart = seamRadius, bottomEnd = seamRadius)
        totalCount - 1 -> RoundedCornerShape(topStart = seamRadius, topEnd = seamRadius, bottomStart = cornerRadius, bottomEnd = cornerRadius)
        else -> RoundedCornerShape(seamRadius)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val hapticHelper = rememberHapticHelper()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CardCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<CardCategory?>(null) }
    var revealedCategoryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(categoryToDelete) {
        if (categoryToDelete != null) {
            hapticHelper.performDestructiveWarning()
        }
    }

    // JSON Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    // JSON Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val windowSizeInfo = MaterialThemeAdaptive

    val lazyListState = rememberLazyListState()

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && revealedCategoryId != null) {
            revealedCategoryId = null
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val maxCollapsePx = with(density) { 88.dp.toPx() }
    val collapseFraction by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                (lazyListState.firstVisibleItemScrollOffset / maxCollapsePx).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = windowSizeInfo.horizontalPadding)
                .pointerInput(revealedCategoryId) {
                    if (revealedCategoryId != null) {
                        detectTapGestures {
                            revealedCategoryId = null
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = statusBarHeight + 56.dp, bottom = 48.dp)
        ) {
            // Header Expanded Space Item
            item(key = "header_spacer", contentType = "header_spacer") {
                Spacer(modifier = Modifier.height(88.dp))
            }

            // 1. Theme Mode Selector Section
            item(key = "theme_section", contentType = "switcher_section") {
                Column {
                    Text(
                        text = stringResource(R.string.theme_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val themeItems = remember {
                        listOf(
                            SegmentItem(ThemeMode.AUTO, "Auto", Icons.Rounded.BrightnessAuto),
                            SegmentItem(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
                            SegmentItem(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
                            SegmentItem(ThemeMode.OLED, "OLED", Icons.Rounded.Contrast)
                        )
                    }

                    M3SettingsSegmentedSwitcher(
                        items = themeItems,
                        selectedValue = uiState.currentThemeMode,
                        showLabels = false,
                        onSelect = { viewModel.setThemeMode(context, it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Color Setting Row (Material You Monet Android 12+)
                    val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    Surface(
                        onClick = {
                            if (isSupported) {
                                hapticHelper.performClick()
                                viewModel.setDynamicColorEnabled(context, !uiState.isDynamicColorEnabled)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.dynamic_color_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSupported)
                                        stringResource(R.string.dynamic_color_subtitle)
                                    else
                                        "Доступно на Android 12+",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = uiState.isDynamicColorEnabled && isSupported,
                                onCheckedChange = {
                                    if (isSupported) {
                                        hapticHelper.performClick()
                                        viewModel.setDynamicColorEnabled(context, it)
                                    }
                                },
                                enabled = isSupported
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vibration / Haptic Feedback Setting Row
                    Surface(
                        onClick = {
                            if (!uiState.isHapticEnabled) {
                                hapticHelper.performClick()
                            }
                            viewModel.setHapticEnabled(context, !uiState.isHapticEnabled)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Vibration, contentDescription = null, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.haptic_feedback_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.haptic_feedback_subtitle),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = uiState.isHapticEnabled,
                                onCheckedChange = {
                                    if (it) {
                                        hapticHelper.performClick()
                                    }
                                    viewModel.setHapticEnabled(context, it)
                                }
                            )
                        }
                    }
                }
            }

            // 2. Language Selector Section
            item(key = "language_section", contentType = "switcher_section") {
                Column {
                    Text(
                        text = stringResource(R.string.language_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val langItems = remember {
                        listOf(
                            SegmentItem("system", "AUTO", Icons.Rounded.Language),
                            SegmentItem("ru", "RU"),
                            SegmentItem("en", "EN")
                        )
                    }

                    M3SettingsSegmentedSwitcher(
                        items = langItems,
                        selectedValue = uiState.currentLanguage,
                        showLabels = true,
                        onSelect = { viewModel.setLanguage(context, it) }
                    )
                }
            }

            // 3. Security & Privacy Section
            item(key = "security_section", contentType = "surface_blocks") {
                Column {
                    Text(
                        text = stringResource(R.string.security_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        val appSecurityBottomCorner by animateDpAsState(
                            targetValue = if (uiState.isBiometricEnabled) 24.dp else 6.dp,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "appSecurityBottomCorner"
                        )
                        val recentAppsTopCorner by animateDpAsState(
                            targetValue = if (uiState.isBiometricEnabled) 24.dp else 6.dp,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "recentAppsTopCorner"
                        )

                        // Biometric App Lock Block (Top Item)
                        val appLockShape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = appSecurityBottomCorner,
                            bottomEnd = appSecurityBottomCorner
                        )

                        Surface(
                            onClick = {
                                hapticHelper.performClick()
                                viewModel.setBiometricEnabled(context, !uiState.isBiometricEnabled)
                            },
                            shape = appLockShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_lock_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.app_lock_subtitle),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = uiState.isBiometricEnabled,
                                    onCheckedChange = {
                                        hapticHelper.performClick()
                                        viewModel.setBiometricEnabled(context, it)
                                    }
                                )
                            }
                        }

                        // Auto-lock Timeout Styled in Theme Mode / Language Switcher Style
                        AnimatedVisibility(
                            visible = uiState.isBiometricEnabled,
                            enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(280)),
                            exit = shrinkVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(280))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.lock_timeout_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
                                )

                                val timeoutItems = listOf(
                                    SegmentItem(0, stringResource(R.string.lock_timeout_immediately), Icons.Rounded.Bolt),
                                    SegmentItem(60, stringResource(R.string.lock_timeout_1min), Icons.Rounded.Timer),
                                    SegmentItem(300, stringResource(R.string.lock_timeout_5min), Icons.Rounded.HourglassBottom)
                                )

                                M3SettingsSegmentedSwitcher(
                                    items = timeoutItems,
                                    selectedValue = uiState.lockTimeoutSeconds,
                                    showLabels = true,
                                    onSelect = { viewModel.setLockTimeoutSeconds(context, it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Flag Secure Protection Block (Bottom Item)
                        val recentAppsShape = RoundedCornerShape(
                            topStart = recentAppsTopCorner,
                            topEnd = recentAppsTopCorner,
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )

                        Surface(
                            onClick = {
                                hapticHelper.performClick()
                                viewModel.setFlagSecureEnabled(context, !uiState.isFlagSecureEnabled)
                            },
                            shape = recentAppsShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.recent_apps_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.recent_apps_subtitle),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = uiState.isFlagSecureEnabled,
                                    onCheckedChange = {
                                        hapticHelper.performClick()
                                        viewModel.setFlagSecureEnabled(context, it)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Backup and Restore Section
            item(key = "backup_section", contentType = "surface_blocks") {
                Column {
                    Text(
                        text = stringResource(R.string.backup_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Export JSON Block
                        Surface(
                            onClick = {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("cardify_backup_$timestamp.json")
                            },
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.export_json_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.export_json_subtitle),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Import JSON Block
                        Surface(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.import_json_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.import_json_subtitle),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Categories Section (M3 Expressive Connected Group with 2.dp Seams)
            item(key = "categories_section", contentType = "categories_list") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.categories_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp)
                        )

                        FilledTonalButton(
                            onClick = { showAddCategoryDialog = true },
                            shape = PillShape,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.add_category_action),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Connected Category Rows with 2.dp inner seams and Drag & Drop Reordering
                    val density = LocalDensity.current
                    val itemHeightPx = with(density) { 72.dp.toPx() }
                    var localCategories by remember(uiState.categories) { mutableStateOf(uiState.categories) }
                    var draggingIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(uiState.categories) {
                        if (draggingIndex == null) {
                            localCategories = uiState.categories
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val categoriesCount = localCategories.size
                        localCategories.forEachIndexed { index, category ->
                            val itemShape = remember(index, categoriesCount) {
                                getM3GroupedItemShape(index = index, totalCount = categoriesCount)
                            }
                            val isDragging = (draggingIndex == index)

                            key(category.id) {
                                M3SwipeableCategoryRow(
                                    category = category,
                                    index = index,
                                    totalCount = categoriesCount,
                                    isRevealed = (revealedCategoryId == category.id),
                                    isDragging = isDragging,
                                    dragOffsetY = if (isDragging) dragOffsetY else 0f,
                                    onStartDrag = {
                                        revealedCategoryId = null
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                        hapticHelper.performHeavyClick()
                                    },
                                    onDrag = { deltaY ->
                                        dragOffsetY += deltaY
                                        val cur = draggingIndex ?: return@M3SwipeableCategoryRow
                                        val steps = (dragOffsetY / itemHeightPx).roundToInt()
                                        val target = (cur + steps).coerceIn(0, localCategories.size - 1)
                                        if (target != cur) {
                                            val mutable = localCategories.toMutableList()
                                            val item = mutable.removeAt(cur)
                                            mutable.add(target, item)
                                            localCategories = mutable
                                            draggingIndex = target
                                            dragOffsetY -= (target - cur) * itemHeightPx
                                            hapticHelper.performTick()
                                        }
                                    },
                                    onEndDrag = {
                                        val from = uiState.categories
                                        val to = localCategories
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        hapticHelper.performClick()
                                        if (from != to) {
                                            viewModel.reorderCategories(to)
                                        }
                                    },
                                    onCancelDrag = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        localCategories = uiState.categories
                                    },
                                    onToggleReveal = {
                                        revealedCategoryId = if (revealedCategoryId == category.id) null else category.id
                                    },
                                    onEdit = {
                                        categoryToEdit = category
                                        revealedCategoryId = null
                                    },
                                    onDelete = {
                                        categoryToDelete = category
                                        revealedCategoryId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // App About Section
            item(key = "about_section", contentType = "about_card") {
                Column {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Cardify",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "v${com.cardify.app.BuildConfig.VERSION_NAME.removePrefix("v")}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = GoogleSansFlexSlantedBadge,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Ultra-Expressive M3 • Offline-First",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // GitHub Repository Link Button
                            Surface(
                                onClick = {
                                    hapticHelper.performClick()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ilya33gh/cardify"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "github.com/ilya33gh/cardify",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = GoogleSansFlexDisplay,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Pinned Collapsing Header on top
        M3ExpressiveCollapsingHeader(
            title = stringResource(R.string.settings_title),
            onNavigateBack = onNavigateBack,
            collapseFraction = collapseFraction,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showAddCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#1E88E5") }
        val iconsList = listOf(
            "shopping_cart", "checkroom", "local_pharmacy", "local_gas_station",
            "restaurant", "devices", "sports_esports", "fitness_center", "local_cafe", "card_giftcard"
        )
        var selectedIcon by remember { mutableStateOf(iconsList.first()) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.new_category_title),
                    fontWeight = FontWeight.Black,
                    fontFamily = OnestFamily,
                    maxLines = 1,
                    softWrap = false
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.category_name_label)) },
                        singleLine = true,
                        shape = ExpressiveButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.category_icon_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconsList.take(5).forEach { iconName ->
                            val isSelected = selectedIcon == iconName
                            Surface(
                                onClick = { selectedIcon = iconName },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(getCategoryIcon(iconName), contentDescription = null, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCategory(categoryName, selectedColorHex, selectedIcon)
                        showAddCategoryDialog = false
                    },
                    shape = PillShape
                ) {
                    Text(
                        text = stringResource(R.string.create_action),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel_action),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }

    categoryToEdit?.let { category ->
        var categoryName by remember { mutableStateOf(category.name) }
        var selectedColorHex by remember { mutableStateOf(category.colorHex) }
        val iconsList = listOf(
            "shopping_cart", "checkroom", "local_pharmacy", "local_gas_station",
            "restaurant", "devices", "sports_esports", "fitness_center", "local_cafe", "card_giftcard"
        )
        var selectedIcon by remember { mutableStateOf(category.iconName) }

        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Редактировать категорию",
                    fontWeight = FontWeight.Black,
                    fontFamily = OnestFamily,
                    maxLines = 1,
                    softWrap = false
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.category_name_label)) },
                        singleLine = true,
                        shape = ExpressiveButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.category_icon_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconsList.take(5).forEach { iconName ->
                            val isSelected = selectedIcon == iconName
                            Surface(
                                onClick = { selectedIcon = iconName },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(getCategoryIcon(iconName), contentDescription = null, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCategory(category.copy(name = categoryName, colorHex = selectedColorHex, iconName = selectedIcon))
                        categoryToEdit = null
                    },
                    shape = PillShape
                ) {
                    Text(
                        text = stringResource(R.string.save_action),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text(
                        text = stringResource(R.string.cancel_action),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Удалить категорию?",
                    fontWeight = FontWeight.Black,
                    fontFamily = OnestFamily,
                    maxLines = 1,
                    softWrap = false
                )
            },
            text = {
                Text(
                    text = "Вы действительно хотите удалить категорию «${category.name}»? Все карты этой категории сохранятся в кошельке.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(category.id)
                        categoryToDelete = null
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(R.string.delete_action),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onError,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(
                        text = stringResource(R.string.cancel_action),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }
}

/**
 * Material 3 Expressive Category Row with Slope-Checked Gesture Detection for 120 FPS Fluid Scrolling
 */
@Composable
private fun M3SwipeableCategoryRow(
    category: CardCategory,
    index: Int,
    totalCount: Int,
    isRevealed: Boolean,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onStartDrag: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onEndDrag: () -> Unit = {},
    onCancelDrag: () -> Unit = {},
    onToggleReveal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dynamicCornerRadius by animateDpAsState(
        targetValue = if (isDragging || isRevealed) 24.dp else 6.dp,
        animationSpec = spring<Dp>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "categoryRowCornerAnim"
    )

    val itemShape = remember(index, totalCount, dynamicCornerRadius) {
        if (totalCount <= 1) {
            RoundedCornerShape(24.dp)
        } else when (index) {
            0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = dynamicCornerRadius, bottomEnd = dynamicCornerRadius)
            totalCount - 1 -> RoundedCornerShape(topStart = dynamicCornerRadius, topEnd = dynamicCornerRadius, bottomStart = 24.dp, bottomEnd = 24.dp)
            else -> RoundedCornerShape(dynamicCornerRadius)
        }
    }
    val density = LocalDensity.current
    val maxRevealPx = remember(density) { with(density) { 106.dp.toPx() } }
    val revealOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val fastTweenSpec = remember { tween<Float>(durationMillis = 210, easing = FastOutSlowInEasing) }

    val horizontalEdgeSpring = remember {
        spring<Float>(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    val buttonScaleX by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0.82f,
        animationSpec = horizontalEdgeSpring,
        label = "buttonScaleX"
    )

    LaunchedEffect(isRevealed, maxRevealPx) {
        val targetPx = if (isRevealed) -maxRevealPx else 0f
        if (revealOffset.targetValue != targetPx) {
            revealOffset.animateTo(targetPx, fastTweenSpec)
        }
    }

    val hapticHelper = rememberHapticHelper()
    var lastSwipeHapticPx by remember { mutableFloatStateOf(0f) }

    val revealFraction = (-revealOffset.value / maxRevealPx).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (isDragging) 1.03f else 1.0f
                scaleY = if (isDragging) 1.03f else 1.0f
                shadowElevation = if (isDragging) 24f else 0f
            }
            .pointerInput(category.id, isRevealed) {
                var isHorizontalDrag = false
                detectHorizontalDragGestures(
                    onDragStart = { isHorizontalDrag = false },
                    onDragEnd = {
                        if (isHorizontalDrag) {
                            coroutineScope.launch {
                                val currentPx = revealOffset.value
                                val shouldBeRevealed = currentPx < -maxRevealPx * 0.35f
                                if (shouldBeRevealed != isRevealed) {
                                    hapticHelper.performClick()
                                    onToggleReveal()
                                } else {
                                    val targetPx = if (isRevealed) -maxRevealPx else 0f
                                    revealOffset.animateTo(targetPx, fastTweenSpec)
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            val targetPx = if (isRevealed) -maxRevealPx else 0f
                            revealOffset.animateTo(targetPx, fastTweenSpec)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val dx = kotlin.math.abs(change.positionChange().x)
                        val dy = kotlin.math.abs(change.positionChange().y)
                        if (dx > dy * 1.2f || isHorizontalDrag) {
                            isHorizontalDrag = true
                            change.consume()
                            coroutineScope.launch {
                                val newPx = (revealOffset.value + dragAmount).coerceIn(-maxRevealPx, 0f)
                                if (kotlin.math.abs(newPx - lastSwipeHapticPx) >= 24f) {
                                    hapticHelper.performTick()
                                    lastSwipeHapticPx = newPx
                                }
                                revealOffset.snapTo(newPx)
                            }
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Category Surface
        Surface(
            shape = itemShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = null,
            shadowElevation = if (isDragging) 8.dp else 0.dp,
            modifier = Modifier.weight(1f),
            onClick = {
                if (isRevealed) {
                    onToggleReveal()
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    val localizedRes = getLocalizedCategoryRes(category.name)
                    val displayName = if (localizedRes != null) stringResource(localizedRes) else category.name

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }

                // Solid background zone next to drag handle (=) that covers the text cleanly on swipe
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .pointerInput(category.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                                onDragEnd = { onEndDrag() },
                                onDragCancel = { onCancelDrag() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onToggleReveal,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Изменить порядок или открыть действия",
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Outside Action Buttons Container
        if (revealFraction > 0f) {
            val totalWidth = 106.dp * revealFraction
            val gapWidth = 6.dp * revealFraction

            Spacer(modifier = Modifier.width(gapWidth))

            Box(
                modifier = Modifier
                    .width(totalWidth)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        alpha = revealFraction.coerceIn(0f, 1f)
                        scaleX = buttonScaleX
                        scaleY = 1.0f
                    },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Action Button (Vertical Stadium Capsule)
                    Surface(
                        onClick = onEdit,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .width(50.dp)
                            .height(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Изменить",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Delete Action Button (Vertical Stadium Capsule)
                    Surface(
                        onClick = onDelete,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .width(50.dp)
                            .height(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Удалить",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
