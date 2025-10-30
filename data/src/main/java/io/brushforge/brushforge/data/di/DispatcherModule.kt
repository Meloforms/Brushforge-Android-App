package io.brushforge.brushforge.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.brushforge.brushforge.core.common.CoroutineDispatchers
import io.brushforge.brushforge.core.common.DefaultCoroutineDispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    fun provideDispatchers(): CoroutineDispatchers = DefaultCoroutineDispatchers
}
