package net.poopyfeed.pf.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.poopyfeed.pf.ui.toast.ToastManager
import net.poopyfeed.pf.ui.toast.ToastManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ToastModule {

    @Binds
    @Singleton
    abstract fun bindToastManager(impl: ToastManagerImpl): ToastManager
}
