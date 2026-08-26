package com.cardify.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.domain.model.CardCategory
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.theme.*

fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color = Color(0xFFFFD54F),
    highlightTextColor: Color = Color(0xFF1B1B1F)
): AnnotatedString {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.trim().lowercase()
        val queryLen = lowerQuery.length

        while (currentIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
            if (matchIndex == -1) {
                append(text.substring(currentIndex))
                break
            }
            if (matchIndex > currentIndex) {
                append(text.substring(currentIndex, matchIndex))
            }
            withStyle(
                style = SpanStyle(
                    background = highlightColor,
                    color = highlightTextColor,
                    fontWeight = FontWeight.Black
                )
            ) {
                append(text.substring(matchIndex, matchIndex + queryLen))
            }
            currentIndex = matchIndex + queryLen
        }
    }
}

/**
 * Google Material 3 Expressive Cardfolio Pass Card (Google Pay / Google Wallet Style)
 * Symmetrical 24.dp shape, harmonious 16.dp barcode island, clean brand monogram,
 * high-contrast solid colors, and tactile Google press physics (scale = 0.96f).
 */
@Composable
fun ExpressiveLoyaltyCard(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveCardShape,
    searchQuery: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }

    // Google Smooth Press Physics (scale = 0.96f)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ExpressiveMotion.PressScale else 1f,
        animationSpec = ExpressiveMotion.GoogleSmooth,
        label = "cardScale"
    )

    val solidCardColor = remember(card.colorHex) {
        CardColorPalette.getColor(card.colorHex)
    }

    val storeInitial = remember(card.title) {
        card.title.trim().take(1).uppercase()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(card.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { offset -> onLongClick(offset) }
                )
            },
        shape = shape,
        color = solidCardColor,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 1. Top Header: Minimal Google Pay Monogram Badge + Category Pill (Left) + Favorite Squircle (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Brand Monogram Circle
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.22f),
                        contentColor = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = storeInitial,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                        }
                    }

                    if (!card.categoryName.isNullOrBlank()) {
                        Surface(
                            shape = PillShape,
                            color = Color.Black.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = card.categoryName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Favorite Squircle Indicator (Settings style)
                if (card.isFavorite) {
                    Surface(
                        shape = SquircleShape,
                        color = Color.Black.copy(alpha = 0.22f),
                        contentColor = Color.White,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = ExpressivePink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Merchant / Card Title (Space Grotesk Bold with Match Highlight)
            Text(
                text = buildHighlightedText(card.title, searchQuery),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 27.sp
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Floating White Barcode Island (Concentric 16.dp Radius)
            Surface(
                shape = BarcodeIslandShape,
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (card.barcodeFormat.is2D) Icons.Default.QrCode2 else Icons.Default.ViewWeek,
                            contentDescription = null,
                            tint = Color(0xFF1E1E1E),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = buildHighlightedText(
                                text = card.barcodeValue,
                                query = searchQuery,
                                highlightColor = Color(0xFFFFD54F),
                                highlightTextColor = Color(0xFF1B1B1F)
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1E1E1E),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F3F4),
                        contentColor = Color(0xFF44474F)
                    ) {
                        Text(
                            text = card.barcodeFormat.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Horizontal List Row View for Loyalty Cards (Google Wallet Row Style)
 */
@Composable
fun ExpressiveLoyaltyCardRow(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = ExpressiveMotion.GoogleSmooth,
        label = "rowScale"
    )

    val solidCardColor = remember(card.colorHex) {
        CardColorPalette.getColor(card.colorHex)
    }

    val storeInitial = remember(card.title) {
        card.title.trim().take(1).uppercase()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(card.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { offset -> onLongClick(offset) }
                )
            },
        shape = ExpressiveRowCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = solidCardColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = storeInitial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildHighlightedText(card.title, searchQuery),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildHighlightedText(card.barcodeValue, searchQuery),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (card.isFavorite) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ExpressivePink.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = ExpressivePink,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = card.barcodeFormat.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2-Column Compact Grid Card View for Loyalty Cards (Google Wallet Grid Style)
 */
@Composable
fun ExpressiveLoyaltyCardGrid(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.GoogleSmooth,
        label = "gridScale"
    )

    val solidCardColor = remember(card.colorHex) {
        CardColorPalette.getColor(card.colorHex)
    }

    val storeInitial = remember(card.title) {
        card.title.trim().take(1).uppercase()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .pointerInput(card.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { offset -> onLongClick(offset) }
                )
            },
        shape = ExpressiveGridCardShape,
        color = solidCardColor,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = storeInitial,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                if (card.isFavorite) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.22f),
                        contentColor = Color.White,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = ExpressivePink,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = buildHighlightedText(card.title, searchQuery),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildHighlightedText(card.barcodeValue, searchQuery),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * M3 Expressive Connected Button Group (Segmented Pill Switcher)
 * Features smooth sliding active pill indicator with Google spring physics.
 */
@Composable
fun M3SegmentedTabGroup(
    allCount: Int,
    favoritesCount: Int,
    onlyFavorites: Boolean,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val sharedColorSpec = remember { tween<Color>(durationMillis = 210, easing = FastOutSlowInEasing) }
    val sharedDpSpec = remember { tween<Dp>(durationMillis = 210, easing = FastOutSlowInEasing) }

    // Dynamic Pill Indicator Color (Primary for All Cards, TertiaryContainer for Favorites)
    val indicatorColor by animateColorAsState(
        targetValue = if (!onlyFavorites) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = sharedColorSpec,
        label = "tabIndicatorColor"
    )

    // Dynamic Favorites Text/Icon Color (Theme-aware dynamic token)
    val favoritesContentColor by animateColorAsState(
        targetValue = if (onlyFavorites) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        animationSpec = sharedColorSpec,
        label = "favoritesContentColor"
    )

    val favoritesBadgeBgColor by animateColorAsState(
        targetValue = if (onlyFavorites) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = sharedColorSpec,
        label = "favoritesBadgeBgColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (!onlyFavorites) 0.dp else tabWidth,
                animationSpec = sharedDpSpec,
                label = "tabIndicatorOffset"
            )

            // Smooth Sliding Active Pill Indicator
            Surface(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(38.dp),
                shape = PillShape,
                color = indicatorColor,
                shadowElevation = 0.dp
            ) {}

            // Interactive Tab Touch Targets
            Row(modifier = Modifier.fillMaxWidth()) {
                // "All Cards" Segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSelectAll
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = if (!onlyFavorites) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.all_cards_tab),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (!onlyFavorites) FontWeight.Black else FontWeight.Bold
                            ),
                            color = if (!onlyFavorites) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (allCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = PillShape,
                                color = if (!onlyFavorites) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (!onlyFavorites) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = "$allCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // "Favorites" Segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSelectFavorites
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = favoritesContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.favorites_tab),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (onlyFavorites) FontWeight.Black else FontWeight.Bold
                            ),
                            color = favoritesContentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (favoritesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = PillShape,
                                color = favoritesBadgeBgColor,
                                contentColor = favoritesContentColor
                            ) {
                                Text(
                                    text = "$favoritesCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SegmentItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * Material 3 Expressive Larger Settings Segmented Switcher
 * Features distinct outer corner rounding, rectangular middle items, and smooth shape morphing.
 */
@Composable
fun <T> M3SettingsSegmentedSwitcher(
    items: List<SegmentItem<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hapticHelper = rememberHapticHelper()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = items.size
            items.forEachIndexed { index, item ->
                val isSelected = item.value == selectedValue

                // Dynamic Smooth Shape Morphing
                val topStartRadius by animateDpAsState(
                    targetValue = when {
                        index == 0 -> if (isSelected) 26.dp else 24.dp
                        else -> if (isSelected) 10.dp else 6.dp
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "topStartRadius"
                )
                val bottomStartRadius by animateDpAsState(
                    targetValue = when {
                        index == 0 -> if (isSelected) 26.dp else 24.dp
                        else -> if (isSelected) 10.dp else 6.dp
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "bottomStartRadius"
                )
                val topEndRadius by animateDpAsState(
                    targetValue = when {
                        index == count - 1 -> if (isSelected) 26.dp else 24.dp
                        else -> if (isSelected) 10.dp else 6.dp
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "topEndRadius"
                )
                val bottomEndRadius by animateDpAsState(
                    targetValue = when {
                        index == count - 1 -> if (isSelected) 26.dp else 24.dp
                        else -> if (isSelected) 10.dp else 6.dp
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "bottomEndRadius"
                )

                val segmentShape = remember(topStartRadius, bottomStartRadius, topEndRadius, bottomEndRadius) {
                    RoundedCornerShape(
                        topStart = topStartRadius,
                        bottomStart = bottomStartRadius,
                        topEnd = topEndRadius,
                        bottomEnd = bottomEndRadius
                    )
                }

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "segmentContainerColor"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "segmentContentColor"
                )

                Surface(
                    onClick = {
                        hapticHelper.performClick()
                        onSelect(item.value)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = segmentShape,
                    color = containerColor,
                    contentColor = contentColor,
                    border = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = contentColor
                            )
                            if (showLabels && item.label.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        } else if (isSelected && showLabels) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (showLabels && item.label.isNotBlank()) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (items.size >= 4) 13.sp else 13.5.sp
                                ),
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Large Adaptive Floating Split Button
 * Connected dual-segment pill button with adaptive theme-aware colors for left & right segments.
 * Height is 68.dp (2x larger) with direct action handling.
 */
@Composable
fun ExpressiveSplitButton(
    primaryText: String,
    primaryIcon: ImageVector,
    primaryColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    primaryContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onPrimaryClick: () -> Unit,
    secondaryIcon: ImageVector = Icons.Default.Add,
    secondaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leftShape = remember {
        RoundedCornerShape(
            topStart = 34.dp,
            bottomStart = 34.dp,
            topEnd = 12.dp,
            bottomEnd = 12.dp
        )
    }

    val rightShape = remember {
        RoundedCornerShape(
            topStart = 12.dp,
            bottomStart = 12.dp,
            topEnd = 34.dp,
            bottomEnd = 34.dp
        )
    }

    Row(
        modifier = modifier.height(68.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Primary Action Segment (Scanner / Hero Action)
        Surface(
            onClick = onPrimaryClick,
            shape = leftShape,
            color = primaryColor,
            contentColor = primaryContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = primaryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = primaryContentColor
                )
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = primaryContentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Right Secondary Action Segment (+ Manual Add Action directly without menu)
        Surface(
            onClick = onSecondaryClick,
            shape = rightShape,
            color = secondaryColor,
            contentColor = secondaryContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.height(68.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = secondaryContentColor
                )
            }
        }
    }
}

/**
 * M3 Expressive Floating Dock (Backgroundless Large Floating Split Button)
 */
@Composable
fun M3FloatingToolbarDock(
    onScanClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ExpressiveSplitButton(
            primaryText = stringResource(R.string.scan_action),
            primaryIcon = Icons.Default.QrCodeScanner,
            primaryColor = MaterialTheme.colorScheme.secondaryContainer,
            primaryContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onPrimaryClick = onScanClick,
            secondaryIcon = Icons.Default.Add,
            secondaryColor = MaterialTheme.colorScheme.primary,
            secondaryContentColor = MaterialTheme.colorScheme.onPrimary,
            onSecondaryClick = onManualClick
        )
    }
}

/**
 * Google Wallet Clean Empty State (Without duplicate action buttons)
 */
@Composable
fun AnimatedEmptyWalletState(
    onScanClick: () -> Unit,
    onManualClick: () -> Unit,
    isSearchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "googleWalletFloat")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walletFloat"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating Passes Illustration
        Box(
            modifier = Modifier.size(width = 220.dp, height = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(width = 150.dp, height = 95.dp)
                    .graphicsLayer {
                        rotationZ = -6f
                        translationX = -18.dp.toPx()
                        translationY = floatOffset.dp.toPx()
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = null
            ) {}

            Surface(
                modifier = Modifier
                    .size(width = 160.dp, height = 100.dp)
                    .graphicsLayer {
                        rotationZ = 4f
                        translationX = 12.dp.toPx()
                        translationY = (-floatOffset * 0.7f).dp.toPx()
                    },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSearchActive)
                stringResource(R.string.not_found_title)
            else
                stringResource(R.string.empty_wallet_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearchActive)
                stringResource(R.string.not_found_subtitle)
            else
                stringResource(R.string.empty_wallet_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Category Filter Row with Real-Time Synchronous Swipe Fill & Auto-Center Scrolling
 */
@Composable
fun CategoryFilterRow(
    categories: List<CardCategory>,
    selectedCategoryIndex: Int,
    onSelectCategory: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Smoothly scroll to center the active category
    LaunchedEffect(selectedCategoryIndex) {
        val layoutInfo = listState.layoutInfo
        val viewportWidth = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == selectedCategoryIndex }
        val itemSize = itemInfo?.size ?: with(density) { 90.dp.toPx() }.toInt()
        val centerOffset = -(viewportWidth / 2 - itemSize / 2)
        listState.animateScrollToItem(
            index = selectedCategoryIndex,
            scrollOffset = centerOffset
        )
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            val isSelected = selectedCategoryIndex == 0
            GooglePillChip(
                isSelected = isSelected,
                onClick = { onSelectCategory(null) },
                label = stringResource(R.string.all_categories),
                icon = Icons.Default.Apps
            )
        }

        itemsIndexed(categories, key = { _, cat -> cat.id }) { index, category ->
            val chipIndex = index + 1
            val isSelected = selectedCategoryIndex == chipIndex
            val localizedRes = getLocalizedCategoryRes(category.iconName)
            val displayName = if (localizedRes != null) stringResource(localizedRes) else category.name

            GooglePillChip(
                isSelected = isSelected,
                onClick = { onSelectCategory(category.id) },
                label = displayName,
                icon = getCategoryIcon(category.iconName)
            )
        }
    }
}

@Composable
fun GooglePillChip(
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector
) {
    val isDark = isSystemInDarkTheme()

    // Constant round pill shape for all chips
    val chipShape = remember { RoundedCornerShape(20.dp) }

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "chipBg"
    )
    val animatedContent by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "chipContent"
    )

    // Smooth subtle elastic pulse from center on selection or repeat click that returns to normal size (120 FPS)
    val pulseScaleX = remember { Animatable(1f) }
    var clickTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            pulseScaleX.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
            pulseScaleX.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            pulseScaleX.snapTo(1f)
        }
    }

    LaunchedEffect(clickTrigger) {
        if (clickTrigger > 0) {
            pulseScaleX.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
            pulseScaleX.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Surface(
        onClick = {
            clickTrigger++
            onClick()
        },
        shape = chipShape,
        color = animatedBg,
        contentColor = animatedContent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = Modifier
            .height(40.dp)
            .zIndex(if (isSelected) 2f else 1f)
            .graphicsLayer {
                scaleX = pulseScaleX.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = animatedContent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = animatedContent,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ColorPickerRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticHelper = rememberHapticHelper()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CardColorPalette.options.forEach { option ->
            val isSelected = selectedHex.equals(option.primaryHex, ignoreCase = true)
            val color = CardColorPalette.getColor(option.primaryHex)

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable {
                        hapticHelper.performClick()
                        onSelectHex(option.primaryHex)
                    }
                    .border(
                        width = if (isSelected) 3.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun getLocalizedCategoryRes(iconName: String): Int? {
    return when (iconName) {
        "shopping_cart" -> R.string.cat_supermarkets
        "checkroom" -> R.string.cat_clothing
        "local_pharmacy" -> R.string.cat_pharmacy
        "local_gas_station" -> R.string.cat_gas
        "restaurant" -> R.string.cat_restaurants
        "devices" -> R.string.cat_electronics
        "sports_esports" -> R.string.cat_entertainment
        "card_giftcard" -> R.string.cat_gifts
        "fitness_center" -> R.string.cat_fitness
        "local_cafe" -> R.string.cat_cafe
        else -> null
    }
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "checkroom" -> Icons.Default.Checkroom
        "local_pharmacy" -> Icons.Default.LocalPharmacy
        "local_gas_station" -> Icons.Default.LocalGasStation
        "restaurant" -> Icons.Default.Restaurant
        "devices" -> Icons.Default.Devices
        "sports_esports" -> Icons.Default.SportsEsports
        "card_giftcard" -> Icons.Default.CardGiftcard
        "fitness_center" -> Icons.Default.FitnessCenter
        "local_cafe" -> Icons.Default.LocalCafe
        else -> Icons.Default.Folder
    }
}

/**
 * Material 3 Expressive Speed Dial FAB Menu with Outside Tap Auto-Close Overlay
 */
@Composable
fun M3ExpressiveSpeedDialFab(
    onScanClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fabRotation"
    )

    // Full-screen backdrop overlay for auto-closing on outside tap
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isExpanded = false
                }
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Speed Dial Items (Expanded - Unified Pills matching photo)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 2 }
                ) + scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = 0.82f),
                exit = fadeOut(tween(140)) + slideOutVertically(
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    targetOffsetY = { it / 2 }
                ) + scaleOut(tween(140, easing = FastOutSlowInEasing), targetScale = 0.82f)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: Manual Action Pill
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExpanded = false
                            onManualClick()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.manual_action),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Item 2: Scan Code Action Pill
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExpanded = false
                            onScanClick()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.scan_action),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Primary FAB (Exact Requested Size: 72.dp)
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isExpanded = !isExpanded
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Card",
                    modifier = Modifier
                        .size(34.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

