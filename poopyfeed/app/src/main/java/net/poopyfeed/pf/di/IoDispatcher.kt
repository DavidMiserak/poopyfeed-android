package net.poopyfeed.pf.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention

/**
 * Qualifier for the IO [kotlinx.coroutines.CoroutineDispatcher] (e.g.
 * [kotlinx.coroutines.Dispatchers.IO]).
 */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
