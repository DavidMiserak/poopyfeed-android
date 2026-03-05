package net.poopyfeed.pf

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.robolectric.Robolectric

/** Empty activity annotated with @AndroidEntryPoint for hosting fragments in Hilt tests. */
@AndroidEntryPoint class HiltTestActivity : AppCompatActivity()

/**
 * Launch a [Fragment] inside [HiltTestActivity] so that Hilt DI is available. Uses
 * [Robolectric.buildActivity] to avoid manifest registration.
 *
 * @param fragmentArgs optional arguments for the fragment
 * @param beforeAdd optional callback run after the activity is created but before the fragment is
 *   added. Use to e.g. set NavController on the container so it is available when fragment
 *   lifecycle reaches STARTED.
 * @param action block runs on the fragment after it is attached and its view is created.
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    noinline beforeAdd: ((Activity) -> Unit)? = null,
    crossinline action: T.() -> Unit = {},
) {
  val activity =
      Robolectric.buildActivity(HiltTestActivity::class.java).create().start().resume().get()

  beforeAdd?.invoke(activity)

  val fragment =
      activity.supportFragmentManager.fragmentFactory.instantiate(
          checkNotNull(T::class.java.classLoader),
          T::class.java.name,
      )
  fragment.arguments = fragmentArgs
  activity.supportFragmentManager
      .beginTransaction()
      .add(android.R.id.content, fragment, "")
      .commitNow()

  @Suppress("UNCHECKED_CAST") (fragment as T).action()
}
