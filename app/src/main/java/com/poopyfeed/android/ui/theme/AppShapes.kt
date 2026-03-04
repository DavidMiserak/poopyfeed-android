package com.poopyfeed.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * App-wide shapes for a soft, cohesive look.
 * Design: generous rounding for cards and surfaces; slightly smaller for buttons/chips.
 */
object AppShapes {
    /** Buttons, text fields, chips */
    val small = RoundedCornerShape(14.dp)

    /** Default cards, list items */
    val medium = RoundedCornerShape(20.dp)

    /** Hero cards, login form, prominent surfaces */
    val large = RoundedCornerShape(28.dp)

    /** Extra large (e.g. bottom sheets, modals) */
    val extraLarge = RoundedCornerShape(32.dp)

    /** Left accent strip on cards */
    val accentStrip = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
}
