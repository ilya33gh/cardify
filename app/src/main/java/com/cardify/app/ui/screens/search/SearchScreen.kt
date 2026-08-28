package com.cardify.app.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.ui.components.ExpressiveLoyaltyCardRow
import com.cardify.app.ui.components.rememberHapticHelper
import com.cardify.app.ui.theme.GoogleSansFlexSlantedCount
import com.cardify.app.ui.theme.InterFamily
import com.cardify.app.ui.theme.MaterialThemeAdaptive
import com.cardify.app.ui.theme.OnestFamily

@Composable
fun SearchScreen(
    cards: List<LoyaltyCard>,
    onCardClick: (LoyaltyCard) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val hapticHelper = rememberHapticHelper()
    val windowSizeInfo = MaterialThemeAdaptive

    val filteredCards = remember(cards, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            emptyList()
        } else {
            cards.filter { card ->
                card.title.contains(query, ignoreCase = true) ||
                        card.barcodeValue.contains(query, ignoreCase = true) ||
                        card.notes.contains(query, ignoreCase = true) ||
                        (card.categoryName?.contains(query, ignoreCase = true) == true)
            }.sortedWith(
                compareByDescending<LoyaltyCard> { it.isFavorite }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val deactivateSearch: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val baseModifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            deactivateSearch()
        }

    val finalModifier = if (nestedScrollConnection != null) {
        baseModifier.nestedScroll(nestedScrollConnection)
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier
    ) {
        // Top Search Bar Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = windowSizeInfo.horizontalPadding, vertical = 6.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansFlexSlantedCount,
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
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Close / Deactivate 'X' icon button on the right
                IconButton(
                    onClick = {
                        hapticHelper.performClick()
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                            deactivateSearch()
                        } else {
                            deactivateSearch()
                            onClose()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close / Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Content Area
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        deactivateSearch()
                    }
            ) {
                val query = searchQuery.trim()

                when {
                    query.isEmpty() -> {
                        // Empty query prompt state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    deactivateSearch()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = stringResource(R.string.search_prompt_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.search_prompt_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = OnestFamily,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    filteredCards.isEmpty() -> {
                        // No results state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    deactivateSearch()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = stringResource(R.string.not_found_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = OnestFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.not_found_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = OnestFamily,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // Results List in compact row format with match highlighting
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = windowSizeInfo.horizontalPadding),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            itemsIndexed(filteredCards, key = { _, card -> card.id }) { index, card ->
                                ExpressiveLoyaltyCardRow(
                                    card = card,
                                    searchQuery = query,
                                    index = index,
                                    totalCount = filteredCards.size,
                                    onClick = {
                                        hapticHelper.performClick()
                                        deactivateSearch()
                                        onCardClick(card)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
