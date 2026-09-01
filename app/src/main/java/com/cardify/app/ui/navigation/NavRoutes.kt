package com.cardify.app.ui.navigation

import android.net.Uri

sealed class NavRoute(val route: String) {
    data object Wallet : NavRoute("wallet")
    data object Scanner : NavRoute("scanner")
    data object Settings : NavRoute("settings")

    data object AddCard : NavRoute("add_card?barcodeValue={barcodeValue}&formatName={formatName}&title={title}&colorHex={colorHex}&notes={notes}&categoryName={categoryName}") {
        fun createRoute(
            barcodeValue: String? = null,
            formatName: String? = null,
            title: String? = null,
            colorHex: String? = null,
            notes: String? = null,
            categoryName: String? = null
        ): String {
            val encodedValue = barcodeValue?.let { Uri.encode(it) } ?: ""
            val encodedFormat = formatName?.let { Uri.encode(it) } ?: ""
            val encodedTitle = title?.let { Uri.encode(it) } ?: ""
            val encodedColor = colorHex?.let { Uri.encode(it) } ?: ""
            val encodedNotes = notes?.let { Uri.encode(it) } ?: ""
            val encodedCategory = categoryName?.let { Uri.encode(it) } ?: ""
            return "add_card?barcodeValue=$encodedValue&formatName=$encodedFormat&title=$encodedTitle&colorHex=$encodedColor&notes=$encodedNotes&categoryName=$encodedCategory"
        }
    }

    data object EditCard : NavRoute("edit_card/{cardId}") {
        fun createRoute(cardId: Long): String {
            return "edit_card/$cardId"
        }
    }
}
