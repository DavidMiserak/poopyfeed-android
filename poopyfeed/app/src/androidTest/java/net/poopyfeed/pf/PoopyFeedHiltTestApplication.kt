package net.poopyfeed.pf

import dagger.hilt.android.testing.CustomTestApplication

/**
 * Declares a Hilt test Application that extends [PoopyFeedApplicationBase]. Hilt generates
 * [PoopyFeedHiltTestApplication_Application], which provides [Configuration.Provider] (and thus
 * WorkManager initialization) while supporting Hilt instrumented tests. The base cannot be
 * [PoopyFeedApplication] because @CustomTestApplication forbids a @HiltAndroidApp value.
 */
@CustomTestApplication(PoopyFeedApplicationBase::class) interface PoopyFeedHiltTestApplication
