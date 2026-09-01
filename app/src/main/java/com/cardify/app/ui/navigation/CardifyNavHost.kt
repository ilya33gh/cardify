package com.cardify.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.cardify.app.data.repository.BackupRepository
import com.cardify.app.data.repository.CardRepository
import com.cardify.app.data.repository.CategoryRepository
import com.cardify.app.ui.screens.addedit.AddEditCardScreen
import com.cardify.app.ui.screens.addedit.AddEditCardViewModel
import com.cardify.app.ui.screens.scanner.CameraScannerScreen
import com.cardify.app.ui.screens.scanner.CameraScannerViewModel
import com.cardify.app.ui.screens.settings.SettingsScreen
import com.cardify.app.ui.screens.settings.SettingsViewModel
import com.cardify.app.ui.screens.wallet.WalletScreen
import com.cardify.app.ui.screens.wallet.WalletViewModel

private var lastNavTime = 0L

/**
 * Debounced navigation to prevent opening multiple screens on rapid clicks.
 */
fun NavHostController.navigateDebounced(route: String) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastNavTime > 350) {
        lastNavTime = currentTime
        navigate(route) { launchSingleTop = true }
    }
}

@Composable
fun CardifyNavHost(
    navController: NavHostController,
    cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    backupRepository: BackupRepository,
    modifier: Modifier = Modifier
) {
    // Pre-warm ViewModels so state (e.g. categories, cards) is pre-loaded BEFORE transition for 120 FPS smoothness
    val walletViewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.Factory(cardRepository, categoryRepository)
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(categoryRepository, backupRepository)
    )

    val onNavigateToScanner = remember(navController) { { navController.navigateDebounced(NavRoute.Scanner.route) } }
    val onNavigateToAddCard = remember(navController) { { navController.navigateDebounced(NavRoute.AddCard.createRoute()) } }
    val onNavigateToEditCard = remember(navController) { { cardId: Long -> navController.navigateDebounced(NavRoute.EditCard.createRoute(cardId)) } }
    val onNavigateToSettings = remember(navController) { { navController.navigateDebounced(NavRoute.Settings.route) } }
    val onPopBack: () -> Unit = remember(navController) { { navController.popBackStack(); Unit } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isWalletTop = currentRoute == null || currentRoute == NavRoute.Wallet.route

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // Base Layer: WalletScreen is permanently rendered underneath sub-screens
        WalletScreen(
            viewModel = walletViewModel,
            isWalletTop = isWalletTop,
            backupRepository = backupRepository,
            onNavigateToScanner = onNavigateToScanner,
            onNavigateToAddCard = onNavigateToAddCard,
            onNavigateToEditCard = onNavigateToEditCard,
            onNavigateToSettings = onNavigateToSettings,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Navigation Host for sub-screens
        NavHost(
            navController = navController,
            startDestination = NavRoute.Wallet.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> fullWidth }
                )
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
        ) {
            composable(NavRoute.Wallet.route) {
                // Transparent placeholder while WalletScreen is drawn at base layer
                Spacer(modifier = Modifier.fillMaxSize())
            }

            composable(NavRoute.Scanner.route) {
                val scannerViewModel: CameraScannerViewModel = viewModel()
                CameraScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = onPopBack,
                    onBarcodeScanned = { value, format ->
                        navController.navigate(NavRoute.AddCard.createRoute(value, format)) {
                            popUpTo(NavRoute.Scanner.route) { inclusive = true }
                        }
                    },
                    onNavigateToManualAdd = {
                        navController.navigate(NavRoute.AddCard.createRoute()) {
                            popUpTo(NavRoute.Scanner.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = NavRoute.AddCard.route,
                arguments = listOf(
                    navArgument("barcodeValue") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("formatName") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("colorHex") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("notes") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("categoryName") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val barcodeValue = backStackEntry.arguments?.getString("barcodeValue")?.takeIf { it.isNotBlank() }
                val formatName = backStackEntry.arguments?.getString("formatName")?.takeIf { it.isNotBlank() }
                val title = backStackEntry.arguments?.getString("title")?.takeIf { it.isNotBlank() }
                val colorHex = backStackEntry.arguments?.getString("colorHex")?.takeIf { it.isNotBlank() }
                val notes = backStackEntry.arguments?.getString("notes")?.takeIf { it.isNotBlank() }
                val categoryName = backStackEntry.arguments?.getString("categoryName")?.takeIf { it.isNotBlank() }

                val addEditViewModel: AddEditCardViewModel = viewModel(
                    factory = AddEditCardViewModel.Factory(
                        cardRepository = cardRepository,
                        categoryRepository = categoryRepository,
                        initialCardId = null,
                        initialBarcodeValue = barcodeValue,
                        initialFormatName = formatName,
                        initialTitle = title,
                        initialColorHex = colorHex,
                        initialNotes = notes,
                        initialCategoryName = categoryName
                    )
                )
                AddEditCardScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = onPopBack
                )
            }

            composable(
                route = NavRoute.EditCard.route,
                arguments = listOf(
                    navArgument("cardId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L

                val addEditViewModel: AddEditCardViewModel = viewModel(
                    factory = AddEditCardViewModel.Factory(
                        cardRepository = cardRepository,
                        categoryRepository = categoryRepository,
                        initialCardId = cardId,
                        initialBarcodeValue = null,
                        initialFormatName = null
                    )
                )
                AddEditCardScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = onPopBack
                )
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = onPopBack,
                    onNavigateToAddCard = { barcodeValue, formatName, title, colorHex, notes, categoryName ->
                        navController.navigate(
                            NavRoute.AddCard.createRoute(
                                barcodeValue = barcodeValue,
                                formatName = formatName,
                                title = title,
                                colorHex = colorHex,
                                notes = notes,
                                categoryName = categoryName
                            )
                        )
                    }
                )
            }
        }
    }
}
