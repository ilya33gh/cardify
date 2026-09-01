package com.cardify.app.ui.screens.wallet

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import kotlin.math.roundToInt
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.cardify.app.ui.theme.GoogleSansFlexSlantedCount
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import com.cardify.app.ui.theme.GoogleSansFlexBrand
import com.cardify.app.ui.theme.GoogleSansFlexFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.cardify.app.R
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.components.*
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.screens.carddetail.CardDetailSheet
import com.cardify.app.ui.screens.search.SearchScreen
import com.cardify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    isWalletTop: Boolean = true,
    backupRepository: com.cardify.app.data.repository.BackupRepository? = null,
    onNavigateToScanner: () -> Unit,
    onNavigateToAddCard: () -> Unit,
    onNavigateToEditCard: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentNavTab by rememberSaveable { mutableIntStateOf(0) }

    var showSortBottomSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<LoyaltyCard?>(null) }
    val selectedCardIds = remember { mutableStateListOf<Long>() }
    var isSelectionActive by remember { mutableStateOf(false) }
    val isSelectionMode = isSelectionActive
    var showShareBundleBottomSheet by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    val hapticHelper = rememberHapticHelper()

    LaunchedEffect(cardToDelete) {
        if (cardToDelete != null) {
            hapticHelper.performDestructiveWarning()
        }
    }

    val context = LocalContext.current
    val triggerHaptic = remember(hapticHelper) {
        {
            hapticHelper.performClick()
        }
    }

    // System Back Gesture: In selection mode, clear selection and exit mode
    BackHandler(enabled = isSelectionMode) {
        selectedCardIds.clear()
        isSelectionActive = false
    }

    // System Back Gesture: When in Search tab, return to Cards tab
    BackHandler(enabled = !isSelectionMode && currentNavTab == 1) {
        currentNavTab = 0
    }

    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    // Export Selected Cards Launcher
    val exportSelectedJsonLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = backupRepository?.exportSelectedToJson(uri, selectedCardIds.toList())
                if (res?.isSuccess == true) {
                    android.widget.Toast.makeText(context, "Экспортировано: ${res.getOrNull()}", android.widget.Toast.LENGTH_SHORT).show()
                    selectedCardIds.clear()
                    isSelectionActive = false
                }
            }
        }
    }

    // Categories list for Pager Page mapping
    val categoriesList = remember(uiState.categories) { uiState.categories }
    val pageCount = 1 + categoriesList.size

    val initialPage = remember(uiState.selectedCategoryId, categoriesList) {
        if (uiState.selectedCategoryId == null) 0 else {
            val idx = categoriesList.indexOfFirst { it.id == uiState.selectedCategoryId }
            if (idx >= 0) idx + 1 else 0
        }
    }

    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    var manualTapTargetIndex by remember { mutableStateOf<Int?>(null) }

    val activeCategoryIndex by remember(categoriesList) {
        derivedStateOf {
            val manual = manualTapTargetIndex
            if (manual != null) {
                manual
            } else {
                val count = 1 + categoriesList.size
                if (count <= 1) 0 else {
                    (pagerState.currentPage + pagerState.currentPageOffsetFraction).roundToInt().coerceIn(0, count - 1)
                }
            }
        }
    }

    // Clear manual tap override as soon as pager settles
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            manualTapTargetIndex = null
        }
    }

    // Synchronize Swipe/Tap Pager Settled Page -> ViewModel Selected Category
    LaunchedEffect(pagerState.settledPage) {
        val page = pagerState.settledPage
        val targetCategoryId = if (page == 0) null else categoriesList.getOrNull(page - 1)?.id
        if (uiState.selectedCategoryId != targetCategoryId) {
            viewModel.onCategorySelected(targetCategoryId)
        }
    }

    val windowSizeInfo = MaterialThemeAdaptive
    val density = LocalDensity.current
    val maxCollapseDp = if (windowSizeInfo.heightType == WindowType.COMPACT || windowSizeInfo.isLandscape) 60.dp else 92.dp
    val maxCollapsePx = with(density) { maxCollapseDp.toPx() }

    var headerCollapseOffsetPx by remember(maxCollapsePx) { mutableFloatStateOf(-maxCollapsePx) }
    val settleAnim = remember { Animatable(-maxCollapsePx) }

    val expandFraction by remember {
        derivedStateOf {
            if (maxCollapsePx > 0f) {
                (1f - (-headerCollapseOffsetPx / maxCollapsePx)).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            // 1:1 Instantaneous Zero-Allocation Finger Tracking on Scroll (True 120 FPS)
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && headerCollapseOffsetPx > -maxCollapsePx) {
                    val prev = headerCollapseOffsetPx
                    headerCollapseOffsetPx = (headerCollapseOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                    return Offset(0f, headerCollapseOffsetPx - prev)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f && headerCollapseOffsetPx < 0f) {
                    val prev = headerCollapseOffsetPx
                    headerCollapseOffsetPx = (headerCollapseOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                    return Offset(0f, headerCollapseOffsetPx - prev)
                }
                return Offset.Zero
            }

            // Smooth 120 FPS Momentum Deceleration & Spring Settle (Standard Android fling)
            override suspend fun onPreFling(available: Velocity): Velocity {
                val vy = available.y
                if (vy < -150f && headerCollapseOffsetPx > -maxCollapsePx) {
                    coroutineScope.launch {
                        settleAnim.snapTo(headerCollapseOffsetPx)
                        settleAnim.animateTo(
                            targetValue = -maxCollapsePx,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 420f
                            )
                        ) {
                            headerCollapseOffsetPx = value
                        }
                    }
                }
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val vy = available.y
                if (vy > 150f && headerCollapseOffsetPx < 0f) {
                    coroutineScope.launch {
                        settleAnim.snapTo(headerCollapseOffsetPx)
                        settleAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 420f
                            )
                        ) {
                            headerCollapseOffsetPx = value
                        }
                    }
                } else if (headerCollapseOffsetPx < 0f && headerCollapseOffsetPx > -maxCollapsePx) {
                    val target = if (headerCollapseOffsetPx > -maxCollapsePx * 0.5f) 0f else -maxCollapsePx
                    coroutineScope.launch {
                        settleAnim.snapTo(headerCollapseOffsetPx)
                        settleAnim.animateTo(
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 420f
                            )
                        ) {
                            headerCollapseOffsetPx = value
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val selectionDimAlpha by animateFloatAsState(
        targetValue = if (isSelectionMode) 0.35f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "selectionDimAlpha"
    )

    // Auto-collapse top "cardify" banner smoothly when entering selection mode
    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode && headerCollapseOffsetPx > -maxCollapsePx) {
            settleAnim.snapTo(headerCollapseOffsetPx)
            settleAnim.animateTo(
                targetValue = -maxCollapsePx,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 420f
                )
            ) {
                headerCollapseOffsetPx = value
            }
        }
    }

    val onCardTap: (LoyaltyCard) -> Unit = { card ->
        if (isSelectionMode) {
            hapticHelper.performClick()
            if (selectedCardIds.contains(card.id)) {
                selectedCardIds.remove(card.id)
            } else {
                selectedCardIds.add(card.id)
            }
        } else {
            triggerHaptic()
            viewModel.onCardClicked(card)
        }
    }

    val onCardLongPress: (LoyaltyCard) -> Unit = { card ->
        hapticHelper.performClick()
        isSelectionActive = true
        if (!selectedCardIds.contains(card.id)) {
            selectedCardIds.add(card.id)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topBarHeight = 56.dp + (maxCollapseDp * expandFraction) + statusBarHeight

                val logoInteractionSource = remember { MutableInteractionSource() }
                val isLogoPressed by logoInteractionSource.collectIsPressedAsState()
                val logoPressScale by animateFloatAsState(
                    targetValue = if (isLogoPressed) 0.92f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "logoPressScale"
                )

                val brandTravelDistancePx = with(density) { (50.dp + ((maxCollapseDp - 46.dp) / 2)).toPx() }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .graphicsLayer { alpha = selectionDimAlpha },
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarHeight)
                            .padding(horizontal = windowSizeInfo.horizontalPadding)
                    ) {
                        // Single Unified Morphing Logo + "cardify" Title (Centered between search and chips in expanded state)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .graphicsLayer {
                                    val currentScale = (1f + (0.45f * expandFraction)) * logoPressScale
                                    scaleX = currentScale
                                    scaleY = currentScale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    translationY = expandFraction * brandTravelDistancePx
                                }
                                .clickable(
                                    interactionSource = logoInteractionSource,
                                    indication = null
                                ) {
                                    triggerHaptic()
                                    coroutineScope.launch {
                                        settleAnim.snapTo(headerCollapseOffsetPx)
                                        val target = if (expandFraction < 0.5f) 0f else -maxCollapsePx
                                        settleAnim.animateTo(
                                            targetValue = target,
                                            animationSpec = spring(
                                                dampingRatio = 0.88f,
                                                stiffness = 380f
                                            )
                                        ) {
                                            headerCollapseOffsetPx = value
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_app_logo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Text(
                                text = "cardify",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = GoogleSansFlexBrand,
                                    fontWeight = FontWeight(650),
                                    fontSize = 24.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Top-Right Action Button (Settings)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = null,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(onClick = {
                                    hapticHelper.performClick()
                                    onNavigateToSettings()
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.settings_title),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = currentNavTab,
                transitionSpec = {
                    val slideDuration = 280
                    val easing = FastOutSlowInEasing
                    if (targetState == 1) {
                        slideInHorizontally(animationSpec = tween(slideDuration, easing = easing)) { width -> width } togetherWith
                                slideOutHorizontally(animationSpec = tween(slideDuration, easing = easing)) { width -> -width }
                    } else {
                        slideInHorizontally(animationSpec = tween(slideDuration, easing = easing)) { width -> -width } togetherWith
                                slideOutHorizontally(animationSpec = tween(slideDuration, easing = easing)) { width -> width }
                    }
                },
                label = "navTabSlide",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { tab ->
                if (tab == 1) {
                    SearchScreen(
                        cards = uiState.allCards,
                        onCardClick = { card ->
                            viewModel.onCardClicked(card)
                        },
                        onClose = {
                            currentNavTab = 0
                        },
                        nestedScrollConnection = nestedScrollConnection
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CategoryFilterRow(
                                categories = uiState.categories,
                                selectedCategoryIndex = activeCategoryIndex,
                                onSelectCategory = { catId ->
                                    if (!isSelectionMode) {
                                        val targetPage = if (catId == null) 0 else {
                                            val idx = categoriesList.indexOfFirst { it.id == catId }
                                            if (idx >= 0) idx + 1 else 0
                                        }
                                        manualTapTargetIndex = targetPage
                                        if (pagerState.currentPage != targetPage) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = targetPage,
                                                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .padding(top = 4.dp, bottom = 4.dp)
                                    .graphicsLayer { alpha = selectionDimAlpha }
                            )

                            // 4. Cards Count & Layout Toggle Row (Subtle, Expressive)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 4.dp)
                                    .graphicsLayer { alpha = selectionDimAlpha },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.allCards.size} ${stringResource(R.string.all_cards_tab)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = GoogleSansFlexSlantedCount,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )

                                val sortButtonRotation by animateFloatAsState(
                                    targetValue = if (showSortBottomSheet) 180f else 0f,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                    label = "sortButtonRotation"
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = null,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (!isSelectionMode) {
                                                triggerHaptic()
                                                showSortBottomSheet = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Menu,
                                            contentDescription = stringResource(R.string.sort_dialog_title),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(sortButtonRotation)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3. Rounded Separator Surface for Cards (Matching Google Messages Reference)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 10.dp)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isSelectionMode,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        // Calculate page cards with favorites pinned to the top
                                        val pageCards = remember(uiState.allCards, page, categoriesList, uiState.sortOrder, uiState.isSortAscending) {
                                            val rawCards = if (page == 0) {
                                                uiState.allCards
                                            } else {
                                                val catId = categoriesList.getOrNull(page - 1)?.id
                                                if (catId != null) uiState.allCards.filter { it.categoryId == catId } else uiState.allCards
                                            }

                                            sortCardsList(rawCards, uiState.sortOrder, uiState.isSortAscending)
                                        }

                                        if (pageCards.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(bottom = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val isPageFilterActive = (uiState.allCardsCount > 0) && (page > 0)
                                                AnimatedEmptyWalletState(
                                                    onScanClick = onNavigateToScanner,
                                                    onManualClick = onNavigateToAddCard,
                                                    isSearchActive = isPageFilterActive
                                                )
                                            }
                                        } else {
                                            val pageListState = rememberLazyListState()
                                            val pageGridState = rememberLazyGridState()
                                            val adaptivePadding = PaddingValues(
                                                horizontal = windowSizeInfo.horizontalPadding,
                                                vertical = 6.dp
                                            )

                                            when (uiState.layoutMode) {
                                                LayoutMode.FULL_CARDS -> {
                                                    if (windowSizeInfo.isWideScreen) {
                                                        // On Tablets, Foldables, and Landscape: display cards in a responsive multi-column grid
                                                        LazyVerticalGrid(
                                                            state = pageGridState,
                                                            columns = GridCells.Adaptive(minSize = 340.dp),
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = adaptivePadding,
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                                        ) {
                                                            items(pageCards, key = { it.id }) { card ->
                                                                ExpressiveLoyaltyCard(
                                                                    card = card,
                                                                    searchQuery = "",
                                                                    isSelectionMode = isSelectionMode,
                                                                    isSelected = selectedCardIds.contains(card.id),
                                                                    onClick = { onCardTap(card) },
                                                                    onLongClick = { onCardLongPress(card) }
                                                                )
                                                            }
                                                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                                                Spacer(modifier = Modifier.height(100.dp))
                                                            }
                                                        }
                                                    } else {
                                                        // On Compact Phones: single-column full card stack
                                                        LazyColumn(
                                                            state = pageListState,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = adaptivePadding,
                                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                                        ) {
                                                            items(pageCards, key = { it.id }) { card ->
                                                                ExpressiveLoyaltyCard(
                                                                    card = card,
                                                                    searchQuery = "",
                                                                    isSelectionMode = isSelectionMode,
                                                                    isSelected = selectedCardIds.contains(card.id),
                                                                    onClick = { onCardTap(card) },
                                                                    onLongClick = { onCardLongPress(card) }
                                                                )
                                                            }
                                                            item { Spacer(modifier = Modifier.height(100.dp)) }
                                                        }
                                                    }
                                                }
                                                LayoutMode.LIST_ROWS -> {
                                                    if (windowSizeInfo.isTablet && windowSizeInfo.isLandscape) {
                                                        // 2-column rows on very wide tablet landscape
                                                        LazyVerticalGrid(
                                                            state = pageGridState,
                                                            columns = GridCells.Fixed(2),
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = adaptivePadding,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            items(pageCards, key = { it.id }) { card ->
                                                                CompactCardifyCardItem(
                                                                    card = card,
                                                                    searchQuery = "",
                                                                    isSelectionMode = isSelectionMode,
                                                                    isSelected = selectedCardIds.contains(card.id),
                                                                    onClick = { onCardTap(card) },
                                                                    onLongClick = { onCardLongPress(card) }
                                                                )
                                                            }
                                                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                                                Spacer(modifier = Modifier.height(100.dp))
                                                            }
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.TopCenter
                                                        ) {
                                                            LazyColumn(
                                                                state = pageListState,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .widthIn(max = 760.dp),
                                                                contentPadding = adaptivePadding,
                                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                itemsIndexed(pageCards, key = { _, it -> it.id }) { index, card ->
                                                                    CompactCardifyCardItem(
                                                                        card = card,
                                                                        searchQuery = "",
                                                                        index = index,
                                                                        totalCount = pageCards.size,
                                                                        isSelectionMode = isSelectionMode,
                                                                        isSelected = selectedCardIds.contains(card.id),
                                                                        onClick = { onCardTap(card) },
                                                                        onLongClick = { onCardLongPress(card) }
                                                                    )
                                                                }
                                                                item { Spacer(modifier = Modifier.height(100.dp)) }
                                                            }
                                                        }
                                                    }
                                                }
                                                LayoutMode.GRID_TWO_COLUMNS -> {
                                                    val adaptiveGridCols = windowSizeInfo.getAdaptiveGridColumns(isFullCardMode = false)
                                                    LazyVerticalGrid(
                                                        state = pageGridState,
                                                        columns = GridCells.Fixed(adaptiveGridCols),
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = adaptivePadding,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        items(pageCards, key = { it.id }) { card ->
                                                            ExpressiveLoyaltyCardGrid(
                                                                card = card,
                                                                searchQuery = "",
                                                                isSelectionMode = isSelectionMode,
                                                                isSelected = selectedCardIds.contains(card.id),
                                                                onClick = { onCardTap(card) },
                                                                onLongClick = { onCardLongPress(card) }
                                                            )
                                                        }
                                                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                                            Spacer(modifier = Modifier.height(100.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar (Smoothly slides down out of view synchronously with FAB)
        val navBarHideDistancePx = with(density) { 140.dp.toPx() }

        val navBarOffsetProgress by animateFloatAsState(
            targetValue = if (isSelectionMode) 1f else 0f,
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            label = "navBarOffset"
        )

        if (navBarOffsetProgress < 0.999f) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = navBarOffsetProgress * navBarHideDistancePx
                        alpha = (1f - navBarOffsetProgress).coerceIn(0f, 1f)
                    }
            ) {
                NavigationBarItem(
                    selected = currentNavTab == 0,
                    onClick = {
                        if (!isSelectionMode) {
                            triggerHaptic()
                            currentNavTab = 0
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.all_cards_tab)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.all_cards_tab),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = if (currentNavTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = currentNavTab == 1,
                    onClick = {
                        if (!isSelectionMode) {
                            triggerHaptic()
                            currentNavTab = 1
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.search_tab)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.search_tab),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = if (currentNavTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Floating Selection Toolbar (Positioned at bottom of screen where NavBar was)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(340, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(240, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(340, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = windowSizeInfo.horizontalPadding, end = windowSizeInfo.horizontalPadding, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill Container: Match CardDetailSheet style (height 68.dp, PillShape, secondaryContainer 0.75 alpha)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp),
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 10.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Cancel [X]
                        IconButton(
                            onClick = {
                                hapticHelper.performClick()
                                selectedCardIds.clear()
                                isSelectionActive = false
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cancel_action),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Center: "Выбрано: N" / "Selected: N" (Optimized font size and clear weight for full number visibility)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.selected_count, selectedCardIds.size),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Right Actions: Select All, Share
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val isAllSelected = selectedCardIds.size == uiState.allCards.size && uiState.allCards.isNotEmpty()
                            IconButton(
                                onClick = {
                                    hapticHelper.performClick()
                                    if (isAllSelected) {
                                        selectedCardIds.clear()
                                    } else {
                                        selectedCardIds.clear()
                                        selectedCardIds.addAll(uiState.allCards.map { it.id })
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                    contentDescription = if (isAllSelected) stringResource(R.string.deselect_all_action) else stringResource(R.string.select_all_action),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val hasSelection = selectedCardIds.isNotEmpty()
                            IconButton(
                                onClick = {
                                    if (hasSelection) {
                                        hapticHelper.performClick()
                                        showShareBundleBottomSheet = true
                                    }
                                },
                                enabled = hasSelection,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.share_action),
                                    tint = if (hasSelection) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Right Dedicated Delete Button: Match CardDetailSheet style (68.dp, RoundedCornerShape(24.dp))
                val hasSelection = selectedCardIds.isNotEmpty()
                Surface(
                    onClick = {
                        if (hasSelection) {
                            hapticHelper.performDestructiveWarning()
                            showDeleteSelectedDialog = true
                        }
                    },
                    enabled = hasSelection,
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (hasSelection) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    contentColor = if (hasSelection) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f),
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete_action),
                            tint = if (hasSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Speed Dial FAB (Anchored to bottom-end and smoothly hidden in selection mode)
        if (currentNavTab == 0) {
            M3ExpressiveSpeedDialFab(
                onScanClick = onNavigateToScanner,
                onManualClick = onNavigateToAddCard,
                isVisible = !isSelectionMode,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = windowSizeInfo.horizontalPadding, bottom = 96.dp)
            )
        }
    }

    // POS Mode / Detail Bottom Sheet (Rendered only when Wallet is the top visible destination)
    if (isWalletTop) {
        uiState.selectedCardForDetail?.let { card ->
            CardDetailSheet(
                card = card,
                onDismiss = { viewModel.onDismissCardDetail() },
                onEditCard = onNavigateToEditCard,
                onDeleteCard = { viewModel.onDeleteCard(it) },
                onToggleFavorite = { viewModel.onToggleFavorite(card) }
            )
        }
    }

    // Card Delete Confirmation Dialog (Context Menu & Screen trigger)
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
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
                        val idToDelete = card.id
                        cardToDelete = null
                        viewModel.onDeleteCard(idToDelete)
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
                TextButton(onClick = { cardToDelete = null }) {
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

    if (showSortBottomSheet) {
        SortAndLayoutBottomSheet(
            sortOrder = uiState.sortOrder,
            isSortAscending = uiState.isSortAscending,
            layoutMode = uiState.layoutMode,
            onSelectSortOrder = { order ->
                viewModel.setSortOrder(order)
            },
            onToggleSortDirection = {
                viewModel.toggleSortDirection()
            },
            onSelectLayoutMode = { mode ->
                viewModel.setLayoutMode(mode, context)
            },
            onDismiss = { showSortBottomSheet = false }
        )
    }

    if (showShareBundleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareBundleBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = BottomSheetTopShape,
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_bundle_dialog_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = OnestFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Option 1: Link
                    Surface(
                        onClick = {
                            hapticHelper.performClick()
                            showShareBundleBottomSheet = false
                            val selectedCardsList = uiState.allCards.filter { it.id in selectedCardIds }
                            val link = com.cardify.app.domain.util.CardDeepLinkHelper.createBundleDeepLink(selectedCardsList)
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, link)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.share_bundle_dialog_title)))
                        },
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
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
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.share_bundle_link_option),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OnestFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.share_bundle_link_subtitle),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Option 2: JSON
                    Surface(
                        onClick = {
                            hapticHelper.performClick()
                            showShareBundleBottomSheet = false
                            val fileName = "cardify_selected_${System.currentTimeMillis() / 1000}.json"
                            exportSelectedJsonLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.share_bundle_json_option),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OnestFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.share_bundle_json_subtitle),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.delete_selected_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontFamily = OnestFamily,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_selected_dialog_desc, selectedCardIds.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticHelper.performClick()
                        viewModel.deleteCards(selectedCardIds.toList())
                        selectedCardIds.clear()
                        isSelectionActive = false
                        showDeleteSelectedDialog = false
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteSelectedDialog = false },
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }
}

/**
 * Material 3 Expressive Sort & Layout Bottom Sheet
 * Matches the reference design with direction toggle card, radio-button criteria list,
 * and segmented switcher for layout modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortAndLayoutBottomSheet(
    sortOrder: SortOrder,
    isSortAscending: Boolean,
    layoutMode: LayoutMode,
    onSelectSortOrder: (SortOrder) -> Unit,
    onToggleSortDirection: () -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isOled = MaterialTheme.colorScheme.surface == Color.Black
    val hapticHelper = rememberHapticHelper()

    val bottomSheetContainerColor = when {
        isOled -> Color.Black
        isDark -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = BottomSheetTopShape,
        containerColor = bottomSheetContainerColor,
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
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 32.dp)
        ) {
            // Title "Сортировать по"
            Text(
                text = stringResource(R.string.sort_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OnestFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. Order Direction Card ("Порядок" / "По возрастанию" / "По убыванию")
            Surface(
                onClick = {
                    hapticHelper.performClick()
                    onToggleSortDirection()
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (isSortAscending) 0f else 180f,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "sortArrowRotation"
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(arrowRotation),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.sort_order_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = OnestFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSortAscending)
                                stringResource(R.string.sort_order_ascending)
                            else
                                stringResource(R.string.sort_order_descending),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = OnestFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Grouped Sort Criteria Items with 2.dp seams and RadioButtons
            val sortOptions = listOf(
                SortOrder.ALPHABETICAL to stringResource(R.string.sort_by_name),
                SortOrder.DATE_ADDED to stringResource(R.string.sort_by_date),
                SortOrder.FREQUENCY to stringResource(R.string.sort_by_frequency)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sortOptions.forEachIndexed { index, (order, label) ->
                    val isSelected = sortOrder == order
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        sortOptions.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    val itemColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (isDark) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }

                    Surface(
                        onClick = {
                            hapticHelper.performClick()
                            onSelectSortOrder(order)
                        },
                        shape = shape,
                        color = itemColor,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp
                                ),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Layout Mode Switcher Section (styled like Language switcher in Settings)
            Text(
                text = stringResource(R.string.layout_section_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = OnestFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            val layoutCardsLabel = stringResource(R.string.layout_cards)
            val layoutListLabel = stringResource(R.string.layout_list)
            val layoutGridLabel = stringResource(R.string.layout_grid)

            val layoutItems = remember(layoutCardsLabel, layoutListLabel, layoutGridLabel) {
                listOf(
                    SegmentItem(LayoutMode.FULL_CARDS, layoutCardsLabel, Icons.Rounded.ViewAgenda),
                    SegmentItem(LayoutMode.LIST_ROWS, layoutListLabel, Icons.Rounded.ViewHeadline),
                    SegmentItem(LayoutMode.GRID_TWO_COLUMNS, layoutGridLabel, Icons.Rounded.GridView)
                )
            }

            M3SettingsSegmentedSwitcher(
                items = layoutItems,
                selectedValue = layoutMode,
                showLabels = true,
                onSelect = {
                    onSelectLayoutMode(it)
                }
            )
        }
    }
}
