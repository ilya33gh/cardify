package com.cardify.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Standard Symmetrical Google Pixel Shapes (Expressive & Bold)
val PillShape = RoundedCornerShape(percent = 50)
val SquircleShape = RoundedCornerShape(20.dp)
val ExpressiveCardShape = RoundedCornerShape(20.dp)
val ExpressiveButtonShape = RoundedCornerShape(18.dp)
val BarcodeIslandShape = RoundedCornerShape(20.dp)
val BottomSheetTopShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
val ExpressiveBarcodeContainerShape = RoundedCornerShape(26.dp)
