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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.cardify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToAddCard: () -> Unit,
    onNavigateToEditCard: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var activeCardMenuId by remember { mutableStateOf<Long?>(null) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var cardToDelete by remember { mutableStateOf<LoyaltyCard?>(null) }

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

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    // System Back Gesture: Close search bar & clear keyboard/focus when search is active
    BackHandler(enabled = isSearchActive) {
        isSearchExpanded = false
        viewModel.onSearchQueryChanged("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Smooth rotating animation and color transition for search trigger button
    val searchButtonRotation by animateFloatAsState(
        targetValue = if (isSearchActive) 180f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "searchButtonRotation"
    )

    val isDark = isSystemInDarkTheme()

    val searchButtonContainerColor by animateColorAsState(
        targetValue = if (isSearchActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "searchButtonContainerColor"
    )

    val searchButtonContentColor by animateColorAsState(
        targetValue = if (isSearchActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "searchButtonContentColor"
    )

    // Auto-focus and open keyboard when search is opened
    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val isFilterActive = uiState.allCardsCount > 0 && (
            uiState.searchQuery.isNotBlank() ||
                    uiState.selectedCategoryId != null ||
                    uiState.onlyFavorites
            )

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

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxTitleHeightDp = 140.dp
    val maxTitleHeightPx = with(density) { maxTitleHeightDp.toPx() }

    val titleOffset = remember { Animatable(0f) }
    val titleFraction by remember {
        derivedStateOf { (titleOffset.value / maxTitleHeightPx).coerceIn(0f, 1f) }
    }

    val nestedScrollConnection = remember(maxTitleHeightPx) {
        object : NestedScrollConnection {
            // 1:1 Immediate Direct Finger Tracking on Scroll (Standard Android scroll feel)
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && titleOffset.value > 0f) {
                    val currentVal = titleOffset.value
                    val newOffset = (currentVal + delta).coerceIn(0f, maxTitleHeightPx)
                    val consumed = newOffset - currentVal
                    coroutineScope.launch {
                        titleOffset.snapTo(newOffset)
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f && titleOffset.value < maxTitleHeightPx) {
                    val currentVal = titleOffset.value
                    val newOffset = (currentVal + delta).coerceIn(0f, maxTitleHeightPx)
                    val consumedY = newOffset - currentVal
                    coroutineScope.launch {
                        titleOffset.snapTo(newOffset)
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            // Smooth 120 FPS Momentum Deceleration & Spring Settle (Standard Android fling)
            override suspend fun onPreFling(available: Velocity): Velocity {
                val vy = available.y
                if (vy < -250f && titleOffset.value > 0f) {
                    titleOffset.animateTo(
                        targetValue = 0f,
                        initialVelocity = vy,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = 380f
                        )
                    )
                    return Velocity.Zero
                }
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val vy = available.y
                if (vy > 250f && titleOffset.value < maxTitleHeightPx) {
                    titleOffset.animateTo(
                        targetValue = maxTitleHeightPx,
                        initialVelocity = vy,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = 380f
                        )
                    )
                    return Velocity.Zero
                } else if (titleOffset.value > 0f && titleOffset.value < maxTitleHeightPx) {
                    val target = if (titleOffset.value > maxTitleHeightPx * 0.45f) maxTitleHeightPx else 0f
                    titleOffset.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = 380f
                        )
                    )
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isSearchActive) {
                    isSearchExpanded = false
                    viewModel.onSearchQueryChanged("")
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val collapsedAlpha = (1f - (titleFraction * 1.8f)).coerceIn(0f, 1f)
                        val topLogoInteractionSource = remember { MutableInteractionSource() }
                        val isTopLogoPressed by topLogoInteractionSource.collectIsPressedAsState()
                        val topLogoScale by animateFloatAsState(
                            targetValue = if (isTopLogoPressed) 0.90f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "topLogoScale"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = collapsedAlpha
                                    scaleX = topLogoScale
                                    scaleY = topLogoScale
                                }
                                .clickable(
                                    interactionSource = topLogoInteractionSource,
                                    indication = null,
                                    enabled = collapsedAlpha > 0.05f
                                ) {
                                    triggerHaptic()
                                    coroutineScope.launch {
                                        titleOffset.animateTo(
                                            targetValue = maxTitleHeightPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.88f,
                                                stiffness = 380f
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_app_logo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Text(
                                text = "cardify",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SpaceGroteskFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = searchButtonContainerColor,
                                border = null,
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(onClick = {
                                    triggerHaptic()
                                    isSearchExpanded = !isSearchExpanded
                                }) {
                                    Crossfade(
                                        targetState = isSearchActive,
                                        animationSpec = tween(180),
                                        modifier = Modifier.rotate(searchButtonRotation),
                                        label = "searchIconCrossfade"
                                    ) { active ->
                                        Icon(
                                            imageVector = if (active) Icons.Default.Close else Icons.Outlined.Search,
                                            contentDescription = "Search",
                                            tint = searchButtonContentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = null,
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(onClick = {
                                    hapticHelper.performClick()
                                    onNavigateToSettings()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.settings_title),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (titleFraction > 0.01f) {
                        val expandedLogoInteractionSource = remember { MutableInteractionSource() }
                        val isExpandedLogoPressed by expandedLogoInteractionSource.collectIsPressedAsState()
                        val expandedLogoScale by animateFloatAsState(
                            targetValue = if (isExpandedLogoPressed) 0.94f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "expandedLogoScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxTitleHeightDp * titleFraction)
                                .padding(horizontal = 4.dp, vertical = 8.dp * titleFraction),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = titleFraction.coerceIn(0f, 1f)
                                        scaleX = expandedLogoScale
                                        scaleY = expandedLogoScale
                                    }
                                    .clickable(
                                        interactionSource = expandedLogoInteractionSource,
                                        indication = null
                                    ) {
                                        triggerHaptic()
                                        coroutineScope.launch {
                                            titleOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.88f,
                                                    stiffness = 380f
                                                )
                                            )
                                        }
                                    }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size((46 * titleFraction).coerceAtLeast(26f).dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_app_logo),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size((26 * titleFraction).coerceAtLeast(16f).dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "cardify",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = SpaceGroteskFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = (44 * titleFraction).coerceAtLeast(22f).sp,
                                        letterSpacing = (-1.0).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = !uiState.onlyFavorites,
                        onClick = {
                            triggerHaptic()
                            viewModel.onToggleOnlyFavorites(false)
                        },
                        icon = {
                            Icon(
                                imageVector = if (!uiState.onlyFavorites) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                contentDescription = stringResource(R.string.all_cards_tab)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.all_cards_tab),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = if (!uiState.onlyFavorites) FontWeight.Bold else FontWeight.Medium
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
                        selected = uiState.onlyFavorites,
                        onClick = {
                            triggerHaptic()
                            viewModel.onToggleOnlyFavorites(true)
                        },
                        icon = {
                            Icon(
                                imageVector = if (uiState.onlyFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorites_tab)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.favorites_tab),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = if (uiState.onlyFavorites) FontWeight.Bold else FontWeight.Medium
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(nestedScrollConnection)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
                        exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(160))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = null,
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    TextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        placeholder = {
                                            Text(
                                                text = stringResource(R.string.search_placeholder),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 17.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(focusRequester),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true
                                    )

                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CategoryFilterRow(
                        categories = uiState.categories,
                        selectedCategoryIndex = activeCategoryIndex,
                        onSelectCategory = { catId ->
                            val targetPage = if (catId == null) 0 else {
                                val idx = categoriesList.indexOfFirst { it.id == catId }
                                if (idx >= 0) idx + 1 else 0
                            }
                            manualTapTargetIndex = targetPage
                            if (pagerState.currentPage != targetPage) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = targetPage,
                                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                uiState.onlyFavorites -> "${uiState.allCards.size} ${stringResource(R.string.favorites_tab)}"
                                else -> "${uiState.allCards.size} ${stringResource(R.string.all_cards_tab)}"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                val sortRotation by animateFloatAsState(
                                    targetValue = if (showSortMenu) 90f else 0f,
                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                    label = "sortRotation"
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = null,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            showSortMenu = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SwapVert,
                                            contentDescription = "Sort",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(sortRotation)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    offset = DpOffset(0.dp, 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.width(220.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.sort_alphabetical),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.sortOrder == SortOrder.ALPHABETICAL) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == SortOrder.ALPHABETICAL) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setSortOrder(SortOrder.ALPHABETICAL)
                                            showSortMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.sort_date_added),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.sortOrder == SortOrder.DATE_ADDED) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == SortOrder.DATE_ADDED) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setSortOrder(SortOrder.DATE_ADDED)
                                            showSortMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.sort_frequency),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.sortOrder == SortOrder.FREQUENCY) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == SortOrder.FREQUENCY) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setSortOrder(SortOrder.FREQUENCY)
                                            showSortMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            Box {
                                val layoutRotation by animateFloatAsState(
                                    targetValue = if (showLayoutMenu) 90f else 0f,
                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                    label = "layoutRotation"
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = null,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            showLayoutMenu = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = when (uiState.layoutMode) {
                                                LayoutMode.FULL_CARDS -> Icons.Outlined.ViewAgenda
                                                LayoutMode.LIST_ROWS -> Icons.Outlined.ViewStream
                                                LayoutMode.GRID_TWO_COLUMNS -> Icons.Outlined.GridView
                                            },
                                            contentDescription = "Layout",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(layoutRotation)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showLayoutMenu,
                                    onDismissRequest = { showLayoutMenu = false },
                                    offset = DpOffset(0.dp, 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.width(220.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.layout_full_cards),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.layoutMode == LayoutMode.FULL_CARDS) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.ViewAgenda,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.layoutMode == LayoutMode.FULL_CARDS) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setLayoutMode(LayoutMode.FULL_CARDS, context)
                                            showLayoutMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.layout_list_rows),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.layoutMode == LayoutMode.LIST_ROWS) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.ViewStream,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.layoutMode == LayoutMode.LIST_ROWS) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setLayoutMode(LayoutMode.LIST_ROWS, context)
                                            showLayoutMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.layout_grid_2col),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFamily,
                                                    fontWeight = if (uiState.layoutMode == LayoutMode.GRID_TWO_COLUMNS) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.GridView,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.layoutMode == LayoutMode.GRID_TWO_COLUMNS) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            triggerHaptic()
                                            viewModel.setLayoutMode(LayoutMode.GRID_TWO_COLUMNS, context)
                                            showLayoutMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
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
                            AnimatedContent(
                                targetState = uiState.onlyFavorites,
                                transitionSpec = {
                                    val duration = 260
                                    if (targetState) {
                                        (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                                fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
                                                scaleIn(tween(duration, easing = FastOutSlowInEasing), initialScale = 0.95f)) togetherWith
                                                (slideOutHorizontally(tween(duration - 60, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                                        fadeOut(tween(duration - 60, easing = FastOutSlowInEasing)) +
                                                        scaleOut(tween(duration - 60, easing = FastOutSlowInEasing), targetScale = 0.95f))
                                    } else {
                                        (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                                fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
                                                scaleIn(tween(duration, easing = FastOutSlowInEasing), initialScale = 0.95f)) togetherWith
                                                (slideOutHorizontally(tween(duration - 60, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                                        fadeOut(tween(duration - 60, easing = FastOutSlowInEasing)) +
                                                        scaleOut(tween(duration - 60, easing = FastOutSlowInEasing), targetScale = 0.95f))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds(),
                                label = "favTabExpressiveTransition"
                            ) { targetOnlyFavs ->
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    // Calculate page cards directly from uiState.allCards filtered by targetOnlyFavs scene
                                    val pageCards = remember(uiState.allCards, targetOnlyFavs, page, categoriesList, uiState.sortOrder) {
                                        val favFiltered = uiState.allCards.filter { card ->
                                            !targetOnlyFavs || card.isFavorite
                                        }
                                        val rawCards = if (page == 0) {
                                            favFiltered
                                        } else {
                                            val catId = categoriesList.getOrNull(page - 1)?.id
                                            if (catId != null) favFiltered.filter { it.categoryId == catId } else favFiltered
                                        }

                                        when (uiState.sortOrder) {
                                            SortOrder.ALPHABETICAL -> rawCards.sortedBy { it.title.lowercase() }
                                            SortOrder.DATE_ADDED -> rawCards.sortedByDescending { it.createdAt }
                                            SortOrder.FREQUENCY -> rawCards.sortedWith(
                                                compareByDescending<LoyaltyCard> { it.useCount }
                                                    .thenByDescending { it.lastUsedAt }
                                                    .thenByDescending { it.createdAt }
                                            )
                                        }
                                    }

                                    if (pageCards.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(bottom = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isPageFilterActive = (uiState.allCardsCount > 0) && (
                                                uiState.searchQuery.isNotBlank() ||
                                                page > 0 ||
                                                targetOnlyFavs
                                            )
                                            AnimatedEmptyWalletState(
                                                onScanClick = onNavigateToScanner,
                                                onManualClick = onNavigateToAddCard,
                                                isSearchActive = isPageFilterActive
                                            )
                                        }
                                    } else {
                                        val pageListState = rememberLazyListState()
                                        val pageGridState = rememberLazyGridState()
                                        when (uiState.layoutMode) {
                                            LayoutMode.FULL_CARDS -> {
                                                LazyColumn(
                                                    state = pageListState,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    items(pageCards, key = { it.id }) { card ->
                                                        Box {
                                                            ExpressiveLoyaltyCard(
                                                                card = card,
                                                                searchQuery = uiState.searchQuery,
                                                                onClick = {
                                                                    triggerHaptic()
                                                                    viewModel.onCardClicked(card)
                                                                },
                                                                onLongClick = { touchOffset ->
                                                                    triggerHaptic()
                                                                    pressOffset = touchOffset
                                                                    activeCardMenuId = card.id
                                                                }
                                                            )

                                                            DesktopStyleCardContextMenu(
                                                                expanded = activeCardMenuId == card.id,
                                                                card = card,
                                                                pressOffset = pressOffset,
                                                                onDismissRequest = { activeCardMenuId = null },
                                                                onToggleFavorite = { viewModel.onToggleFavorite(card) },
                                                                onEdit = { onNavigateToEditCard(card.id) },
                                                                onDelete = { cardToDelete = card }
                                                            )
                                                        }
                                                    }
                                                    item { Spacer(modifier = Modifier.height(100.dp)) }
                                                }
                                            }
                                            LayoutMode.LIST_ROWS -> {
                                                LazyColumn(
                                                    state = pageListState,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(pageCards, key = { it.id }) { card ->
                                                        Box {
                                                            ExpressiveLoyaltyCardRow(
                                                                card = card,
                                                                searchQuery = uiState.searchQuery,
                                                                onClick = {
                                                                    triggerHaptic()
                                                                    viewModel.onCardClicked(card)
                                                                },
                                                                onLongClick = { touchOffset ->
                                                                    triggerHaptic()
                                                                    pressOffset = touchOffset
                                                                    activeCardMenuId = card.id
                                                                }
                                                            )

                                                            DesktopStyleCardContextMenu(
                                                                expanded = activeCardMenuId == card.id,
                                                                card = card,
                                                                pressOffset = pressOffset,
                                                                onDismissRequest = { activeCardMenuId = null },
                                                                onToggleFavorite = { viewModel.onToggleFavorite(card) },
                                                                onEdit = { onNavigateToEditCard(card.id) },
                                                                onDelete = { cardToDelete = card }
                                                            )
                                                        }
                                                    }
                                                    item { Spacer(modifier = Modifier.height(100.dp)) }
                                                }
                                            }
                                            LayoutMode.GRID_TWO_COLUMNS -> {
                                                LazyVerticalGrid(
                                                    state = pageGridState,
                                                    columns = GridCells.Fixed(2),
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(pageCards, key = { it.id }) { card ->
                                                        Box {
                                                            ExpressiveLoyaltyCardGrid(
                                                                card = card,
                                                                searchQuery = uiState.searchQuery,
                                                                onClick = {
                                                                    triggerHaptic()
                                                                    viewModel.onCardClicked(card)
                                                                },
                                                                onLongClick = { touchOffset ->
                                                                    triggerHaptic()
                                                                    pressOffset = touchOffset
                                                                    activeCardMenuId = card.id
                                                                }
                                                            )

                                                            DesktopStyleCardContextMenu(
                                                                expanded = activeCardMenuId == card.id,
                                                                card = card,
                                                                pressOffset = pressOffset,
                                                                onDismissRequest = { activeCardMenuId = null },
                                                                onToggleFavorite = { viewModel.onToggleFavorite(card) },
                                                                onEdit = { onNavigateToEditCard(card.id) },
                                                                onDelete = { cardToDelete = card }
                                                            )
                                                        }
                                                    }
                                                    item { Spacer(modifier = Modifier.height(100.dp)) }
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

        // Expandable Speed Dial FAB Menu (Anchored at Root Level - Dims Entire Window including Top and Bottom Bars)
        M3ExpressiveSpeedDialFab(
            onScanClick = onNavigateToScanner,
            onManualClick = onNavigateToAddCard,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 96.dp)
        )
    }

    // POS Mode / Detail Bottom Sheet
    uiState.selectedCardForDetail?.let { card ->
        CardDetailSheet(
            card = card,
            onDismiss = { viewModel.onDismissCardDetail() },
            onEditCard = onNavigateToEditCard,
            onDeleteCard = { viewModel.onDeleteCard(it) },
            onToggleFavorite = { viewModel.onToggleFavorite(card) }
        )
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
}

/**
 * Animated Desktop-Style Cursor Positioned Context Menu with Section Dividers & Tactile Haptic Vibration
 */
@Composable
private fun DesktopStyleCardContextMenu(
    expanded: Boolean,
    card: LoyaltyCard,
    pressOffset: Offset,
    onDismissRequest: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(expanded) {
        visibleState.targetState = expanded
    }

    if (!expanded && visibleState.isIdle && !visibleState.currentState) return

    val pxX = pressOffset.x.toInt()
    val pxY = pressOffset.y.toInt()

    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = remember(pxX, pxY) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    var x = anchorBounds.left + pxX
                    var y = anchorBounds.top + pxY

                    if (x + popupContentSize.width > windowSize.width - 16) {
                        x = (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16)
                    }
                    if (y + popupContentSize.height > windowSize.height - 16) {
                        y = (windowSize.height - popupContentSize.height - 16).coerceAtLeast(16)
                    }

                    return IntOffset(x, y)
                }
            }
        },
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + scaleIn(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialScale = 0.8f
            ),
            exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) + scaleOut(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
                targetScale = 0.8f
            )
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.width(220.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    // 1. Favorite toggle item
                    Surface(
                        onClick = {
                            onToggleFavorite()
                            onDismissRequest()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (card.isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = if (card.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (card.isFavorite) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // 2. Edit item
                    Surface(
                        onClick = {
                            onEdit()
                            onDismissRequest()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.edit_action),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // 3. Delete item
                    Surface(
                        onClick = {
                            onDelete()
                            onDismissRequest()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.delete_action),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
