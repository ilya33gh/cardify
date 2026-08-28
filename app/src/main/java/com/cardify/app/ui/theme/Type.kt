package com.cardify.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cardify.app.R

// Google Sans Flex Typography System (Material 3 Expressive)

@OptIn(ExperimentalTextApi::class)
fun createGoogleSansFlex(
    weight: Int = 400,
    roundness: Float = 0f,
    width: Float = 100f,
    grade: Float = 0f,
    slant: Float = 0f
): FontFamily = FontFamily(
    // 1. Primary Latin & Expressive font with Roundness (ROND)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("ROND", roundness),
            FontVariation.Setting("wdth", width),
            FontVariation.Setting("GRAD", grade),
            FontVariation.slant(slant)
        )
    ),
    // 2. Cyrillic (Russian) fallback with matching weight, width, and grade
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("wdth", width),
            FontVariation.Setting("GRAD", grade),
            FontVariation.slant(slant)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexFamily = FontFamily(
    // Thin (100)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(100), FontVariation.Setting("ROND", 0f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(100))
    ),
    // ExtraLight (200)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(200), FontVariation.Setting("ROND", 0f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(200))
    ),
    // Light (300)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300), FontVariation.Setting("ROND", 0f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    // Normal / Regular (400)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.Setting("ROND", 0f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    // Medium (500)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("ROND", 45f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    // SemiBold (600)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.Setting("ROND", 60f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    // Bold (700)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.Setting("ROND", 75f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    // ExtraBold (800)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.Setting("ROND", 85f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    // Black (900)
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", 90f))
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

// Brand rounded variant for expressive headings & chips
val GoogleSansFlexBrand = createGoogleSansFlex(weight = 650, roundness = 85f, width = 102f)
val GoogleSansFlexDisplay = createGoogleSansFlex(weight = 600, roundness = 65f)
val GoogleSansFlexSection = createGoogleSansFlex(weight = 500, roundness = 45f)
val GoogleSansFlexChip = createGoogleSansFlex(weight = 550, roundness = 75f)
val GoogleSansFlexCardTitle = createGoogleSansFlex(weight = 550, roundness = 40f)
val GoogleSansFlexButton = createGoogleSansFlex(weight = 600, roundness = 60f)
val GoogleSansFlexRounded = GoogleSansFlexDisplay

// Slanted Expressive Variants (for tactical M3 accents)
val GoogleSansFlexSlantedBadge = createGoogleSansFlex(weight = 600, roundness = 60f, slant = -6f)
val GoogleSansFlexSlantedNote = createGoogleSansFlex(weight = 400, roundness = 20f, slant = -4f)
val GoogleSansFlexSlantedCount = createGoogleSansFlex(weight = 500, roundness = 30f, slant = -4f)
val GoogleSansFlexSlantedHint = createGoogleSansFlex(weight = 400, roundness = 15f, slant = -4f)

// Semantic Expressive Families
val ExpressiveDisplayFamily = GoogleSansFlexDisplay
val ExpressiveBodyFamily = GoogleSansFlexFamily

// Backward compatibility aliases
val ManropeFamily = GoogleSansFlexFamily
val OnestFamily = GoogleSansFlexFamily
val OutfitFamily = GoogleSansFlexFamily
val FigtreeFamily = GoogleSansFlexFamily
val SpaceGroteskFamily = GoogleSansFlexFamily
val InterFamily = GoogleSansFlexFamily

// JetBrains Mono for Barcodes and Card Numbers
val JetBrainsMonoFamily = FontFamily(
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.Medium),
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.SemiBold),
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.Bold)
)

val CardNumberFontFamily = JetBrainsMonoFamily

val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlexBrand,
        fontWeight = FontWeight(650),
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.15).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlexSection,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlexSection,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlexChip,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlexChip,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFlexChip,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)
