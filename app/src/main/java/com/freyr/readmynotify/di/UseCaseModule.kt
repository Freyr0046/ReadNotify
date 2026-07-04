package com.freyr.readmynotify.di

import com.freyr.readmynotify.domain.usecase.BuildAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.impl.BuildAnnouncementUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindBuildAnnouncementUseCase(
        impl: BuildAnnouncementUseCaseImpl,
    ): BuildAnnouncementUseCase
}
