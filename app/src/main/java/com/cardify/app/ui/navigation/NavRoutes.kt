package com.cardify.app.ui.navigation

import android.net.Uri

sealed class NavRoute(val route: String) {
    data object Wallet : NavRoute("wallet")
    data object Scanner : NavRoute("scanner")
    data object Settings : NavRoute("settings")

    data object AddCard : NavRoute("add_card?barcodeValue={barcodeValue}&formatName={formatName}") {
        fun createRoute(barcodeValue: String? = null, formatName: String? = null): String {
            val encodedValue = barcodeValue?.let { Uri.encode(it) } ?: ""
            val encodedFormat = formatName?.let { Uri.encode(it) } ?: ""
            return "add_card?barcodeValue=$encodedValue&formatName=$encodedFormat"
        }
    }

    data object EditCard : NavRoute("edit_card/{cardId}") {
        fun createRoute(cardId: Long): String {
            return "edit_card/$cardId"
        }
    }
}
