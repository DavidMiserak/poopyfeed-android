package net.poopyfeed.pf.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import net.poopyfeed.pf.ui.toast.TestToastManager
import net.poopyfeed.pf.ui.toast.ToastManager

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ToastModule::class])
abstract class TestToastModule {

  @Binds @Singleton abstract fun bindTestToastManager(impl: TestToastManager): ToastManager
}
