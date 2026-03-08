package net.poopyfeed.pf

import org.robolectric.shadows.ShadowLooper

/**
 * Idles the main looper until [condition] returns true or [maxIterations] is reached.
 *
 * Use this instead of fixed `repeat(N) { ShadowLooper.idleMainLooper() }` so tests wait for a
 * concrete UI or navigation state rather than a magic number of looper ticks. Pass a condition that
 * reflects the desired outcome (e.g. nav destination, view visibility). [maxIterations] is a safety
 * cap to avoid infinite loops if the condition never becomes true.
 */
fun idleMainLooperUntil(maxIterations: Int = 200, condition: () -> Boolean) {
  var i = 0
  while (!condition() && i < maxIterations) {
    ShadowLooper.idleMainLooper()
    i++
  }
}
