package net.poopyfeed.pf.tour

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.color.MaterialColors
import javax.inject.Inject
import javax.inject.Singleton
import net.poopyfeed.pf.R

@Singleton
class TourManager
@Inject
constructor(
    private val tourPreferences: TourPreferences,
    private val tourAnalytics: TourAnalytics,
) {

  @Volatile private var sequenceRunning: Boolean = false

  private var sequenceSteps: List<TourStep> = emptyList()
  private var sequencePart: Int = 0
  private var sequenceStepIndex: Int = 0

  fun shouldShowPart(part: Int): Boolean = tourPreferences.shouldShowPart(part)

  fun markPartComplete(part: Int) {
    tourPreferences.markPartSeen(part)
    tourAnalytics.logTourCompleted(part)
  }

  fun markPartSkipped(part: Int, step: Int) {
    tourPreferences.markPartSeen(part)
    tourAnalytics.logTourSkipped(part, step)
  }

  fun resetForReplay() {
    tourPreferences.resetAll()
  }

  /**
   * Shows each spotlight after scrolling [TourStep.scrollAnchor] into view when present, so targets
   * inside [androidx.core.widget.NestedScrollView] (and similar) are not missed below the fold.
   */
  fun showSequence(activity: Activity, part: Int, steps: List<TourStep>) {
    if (steps.isEmpty() || sequenceRunning) return
    sequenceRunning = true
    sequenceSteps = steps
    sequencePart = part
    sequenceStepIndex = 0
    tourAnalytics.logTourStarted(part)
    tourAnalytics.logTourStepViewed(part, 1)
    presentStep(activity)
  }

  private fun clearSequenceState() {
    sequenceSteps = emptyList()
    sequencePart = 0
    sequenceStepIndex = 0
  }

  private fun presentStep(activity: Activity) {
    if (!sequenceRunning) return
    if (activity.isFinishing || activity.isDestroyed) {
      sequenceRunning = false
      clearSequenceState()
      return
    }
    if (sequenceStepIndex >= sequenceSteps.size) {
      val completedPart = sequencePart
      sequenceRunning = false
      clearSequenceState()
      markPartComplete(completedPart)
      return
    }

    val step = sequenceSteps[sequenceStepIndex]
    val openSpotlight = openSpotlight@{
      if (!sequenceRunning || activity.isFinishing || activity.isDestroyed) return@openSpotlight
      TapTargetView.showFor(
          activity,
          step.target,
          object : TapTargetView.Listener() {
            override fun onTargetClick(view: TapTargetView) {
              super.onTargetClick(view)
              if (!sequenceRunning) return
              sequenceStepIndex++
              if (sequenceStepIndex < sequenceSteps.size) {
                tourAnalytics.logTourStepViewed(sequencePart, sequenceStepIndex + 1)
              }
              presentStep(activity)
            }

            override fun onTargetCancel(view: TapTargetView) {
              super.onTargetCancel(view)
              val skippedPart = sequencePart
              val skippedStep = sequenceStepIndex + 1
              sequenceRunning = false
              clearSequenceState()
              markPartSkipped(skippedPart, skippedStep)
            }
          },
      )
    }

    val anchor = step.scrollAnchor
    if (anchor != null) {
      anchor.post {
        if (!sequenceRunning) return@post
        TourScroll.ensureVisible(anchor)
        anchor.postDelayed(
            {
              if (!sequenceRunning) return@postDelayed
              openSpotlight()
            },
            SCROLL_SETTLE_MS,
        )
      }
    } else {
      openSpotlight()
    }
  }

  companion object {

    /**
     * Delay after layout before starting a tour (ms); keeps spotlights from jumping on first frame.
     */
    const val START_DELAY_MS: Long = 380L

    private const val SCROLL_SETTLE_MS = 200L

    fun buildTarget(
        context: Context,
        view: View,
        title: String,
        description: String,
        step: Int,
        totalSteps: Int,
    ): TapTarget {
      val fullDescription =
          context.getString(
              R.string.tour_spot_description_format,
              description,
              step,
              totalSteps,
          )
      return TapTarget.forView(view, title, fullDescription).applyTourChrome(context)
    }

    fun buildToolbarOverflowTarget(
        context: Context,
        toolbar: Toolbar,
        title: String,
        description: String,
        step: Int,
        totalSteps: Int,
    ): TapTarget {
      val fullDescription =
          context.getString(
              R.string.tour_spot_description_format,
              description,
              step,
              totalSteps,
          )
      return TapTarget.forToolbarOverflow(toolbar, title, fullDescription).applyTourChrome(context)
    }

    private fun TapTarget.applyTourChrome(context: Context): TapTarget {
      val titleFace = ResourcesCompat.getFont(context, R.font.dm_sans_bold) ?: Typeface.DEFAULT_BOLD
      val bodyFace = ResourcesCompat.getFont(context, R.font.dm_sans) ?: Typeface.DEFAULT
      val ringColor =
          MaterialColors.getColor(
              context,
              com.google.android.material.R.attr.colorSecondary,
              ContextCompat.getColor(context, R.color.orange_400),
          )
      val titleColor = ContextCompat.getColor(context, R.color.white)
      val descriptionColor = ContextCompat.getColor(context, R.color.slate_200)

      return this.outerCircleColorInt(ringColor)
          .outerCircleAlpha(0.9f)
          .titleTypeface(titleFace)
          .descriptionTypeface(bodyFace)
          .titleTextSize(20)
          .descriptionTextSize(15)
          .titleTextColorInt(titleColor)
          .descriptionTextColorInt(descriptionColor)
          .dimColorInt(Color.BLACK)
          // Jittered shadow + transparent cutout looks muddy; flat ring reads cleaner.
          .drawShadow(false)
          .cancelable(true)
          .transparentTarget(true)
          .tintTarget(false)
    }
  }
}
