package com.freyr.readmynotify.di

import com.freyr.readmynotify.domain.usecase.BuildAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import com.freyr.readmynotify.domain.usecase.impl.BuildAnnouncementUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.GetInstalledAppsUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.ObserveWhitelistUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.SetAppWhitelistedUseCaseImpl
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

    @Binds
    abstract fun bindObserveWhitelistUseCase(
        impl: ObserveWhitelistUseCaseImpl,
    ): ObserveWhitelistUseCase

    @Binds
    abstract fun bindSetAppWhitelistedUseCase(
        impl: SetAppWhitelistedUseCaseImpl,
    ): SetAppWhitelistedUseCase

    @Binds
    abstract fun bindGetInstalledAppsUseCase(
        impl: GetInstalledAppsUseCaseImpl,
    ): GetInstalledAppsUseCase
}
