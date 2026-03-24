package net.poopyfeed.pf.tour

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView

/** Scroll parents so [anchor] is on screen before a spotlight step. */
internal object TourScroll {

  fun ensureVisible(anchor: View) {
    if (anchor.width <= 0 || anchor.height <= 0) {
      anchor.post { ensureVisible(anchor) }
      return
    }

    var parent = anchor.parent
    while (parent is ViewGroup) {
      if (parent is NestedScrollView) {
        scrollNestedScrollViewToChild(parent, anchor)
        return
      }
      parent = parent.parent
    }

    val rect = Rect(0, 0, anchor.width, anchor.height)
    anchor.requestRectangleOnScreen(rect, false)
  }

  /**
   * Distance from the top of the NestedScrollView's direct child to [descendant], then scroll so
   * the target sits in the upper third with a little padding.
   */
  private fun scrollNestedScrollViewToChild(nsv: NestedScrollView, descendant: View) {
    val scrollChild = nsv.getChildAt(0) ?: return
    var y = 0
    var v: View? = descendant
    while (v != null && v !== scrollChild) {
      y += v.top
      v = v.parent as? View
    }
    val density = descendant.resources.displayMetrics.density
    val padding = (24 * density).toInt()
    val visibleHeight = nsv.height - nsv.paddingTop - nsv.paddingBottom
    val idealTop = (visibleHeight * 0.2f).toInt()
    val targetScroll = (y - idealTop + padding).coerceAtLeast(0)
    val maxScroll = (scrollChild.height - visibleHeight).coerceAtLeast(0)
    // Instant scroll so the spotlight open delay matches the final layout (smooth scroll can lag).
    nsv.scrollTo(0, targetScroll.coerceAtMost(maxScroll))
  }
}
