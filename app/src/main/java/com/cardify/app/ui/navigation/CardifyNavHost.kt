package com.cardify.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cardify.app.data.repository.BackupRepository
import com.cardify.app.data.repository.CardRepository
import com.cardify.app.data.repository.CategoryRepository
import com.cardify.app.ui.components.PredictiveBackWrapper
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
    if (currentTime - lastNavTime > 400) {
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

    Box(modifier = modifier.fillMaxSize()) {
        // Base Layer: WalletScreen is permanently rendered underneath sub-screens
        WalletScreen(
            viewModel = walletViewModel,
            onNavigateToScanner = { navController.navigateDebounced(NavRoute.Scanner.route) },
            onNavigateToAddCard = { navController.navigateDebounced(NavRoute.AddCard.createRoute()) },
            onNavigateToEditCard = { cardId -> navController.navigateDebounced(NavRoute.EditCard.createRoute(cardId)) },
            onNavigateToSettings = { navController.navigateDebounced(NavRoute.Settings.route) }
        )

        // Overlay Navigation Host for sub-screens
        NavHost(
            navController = navController,
            startDestination = NavRoute.Wallet.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        ) {
            composable(NavRoute.Wallet.route) {
                // Wallet route inside NavHost is transparent as WalletScreen is drawn at base layer
                Spacer(modifier = Modifier.fillMaxSize())
            }

            composable(NavRoute.Scanner.route) {
                val scannerViewModel: CameraScannerViewModel = viewModel()
                PredictiveBackWrapper(
                    onBack = { navController.popBackStack() }
                ) {
                    CameraScannerScreen(
                        viewModel = scannerViewModel,
                        onNavigateBack = { navController.popBackStack() },
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
                    }
                )
            ) { backStackEntry ->
                val barcodeValue = backStackEntry.arguments?.getString("barcodeValue")
                val formatName = backStackEntry.arguments?.getString("formatName")

                val addEditViewModel: AddEditCardViewModel = viewModel(
                    factory = AddEditCardViewModel.Factory(
                        cardRepository = cardRepository,
                        categoryRepository = categoryRepository,
                        initialCardId = null,
                        initialBarcodeValue = barcodeValue,
                        initialFormatName = formatName
                    )
                )
                PredictiveBackWrapper(
                    onBack = { navController.popBackStack() }
                ) {
                    AddEditCardScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
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
                PredictiveBackWrapper(
                    onBack = { navController.popBackStack() }
                ) {
                    AddEditCardScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(NavRoute.Settings.route) {
                PredictiveBackWrapper(
                    onBack = { navController.popBackStack() }
                ) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
