package net.poopyfeed.pf.tour

import android.app.Activity
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourManager
@Inject
constructor(
    private val tourPreferences: TourPreferences,
    private val tourAnalytics: TourAnalytics,
) {

  private var currentStep = 0

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

  fun showSequence(activity: Activity, part: Int, targets: List<TapTarget>) {
    if (targets.isEmpty()) return
    currentStep = 0
    tourAnalytics.logTourStarted(part)
    tourAnalytics.logTourStepViewed(part, 1)

    TapTargetSequence(activity)
        .targets(targets)
        .continueOnCancel(false)
        .listener(
            object : TapTargetSequence.Listener {
              override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
                currentStep++
                tourAnalytics.logTourStepViewed(part, currentStep + 1)
              }

              override fun onSequenceFinish() {
                markPartComplete(part)
              }

              override fun onSequenceCanceled(lastTarget: TapTarget) {
                markPartSkipped(part, currentStep + 1)
              }
            })
        .start()
  }

  companion object {
    fun buildTarget(
        view: View,
        title: String,
        description: String,
    ): TapTarget =
        TapTarget.forView(view, title, description)
            .outerCircleAlpha(0.96f)
            .titleTextSize(20)
            .descriptionTextSize(16)
            .drawShadow(true)
            .cancelable(true)
            .tintTarget(true)
            .transparentTarget(false)
  }
}
