package net.poopyfeed.pf.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.poopyfeed.pf.ui.toast.ToastManager
import net.poopyfeed.pf.ui.toast.ToastManagerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class ToastModule {

  @Binds @Singleton abstract fun bindToastManager(impl: ToastManagerImpl): ToastManager
}
