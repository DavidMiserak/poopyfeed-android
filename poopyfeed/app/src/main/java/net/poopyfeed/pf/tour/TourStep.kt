package net.poopyfeed.pf.tour

import android.view.View
import com.getkeepsafe.taptargetview.TapTarget

/**
 * One tour spotlight. [scrollAnchor] is scrolled into view before the step is shown (e.g. inside a
 * [androidx.core.widget.NestedScrollView]); use `null` for targets that are already on screen (e.g.
 * toolbar overflow).
 */
data class TourStep(
    val scrollAnchor: View?,
    val target: TapTarget,
)
