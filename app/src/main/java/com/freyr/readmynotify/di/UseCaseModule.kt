package com.freyr.readmynotify.di

import com.freyr.readmynotify.domain.usecase.BuildAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import com.freyr.readmynotify.domain.usecase.EnqueueAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import com.freyr.readmynotify.domain.usecase.impl.BuildAnnouncementUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.CheckNotificationAccessUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.EnqueueAnnouncementUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.GetInstalledAppsUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.InitializeTtsEngineUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.ObserveEngineStateUseCaseImpl
import com.freyr.readmynotify.domain.usecase.impl.ObservePlaybackStateUseCaseImpl
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

    @Binds
    abstract fun bindCheckNotificationAccessUseCase(
        impl: CheckNotificationAccessUseCaseImpl,
    ): CheckNotificationAccessUseCase

    @Binds
    abstract fun bindInitializeTtsEngineUseCase(
        impl: InitializeTtsEngineUseCaseImpl,
    ): InitializeTtsEngineUseCase

    @Binds
    abstract fun bindObserveEngineStateUseCase(
        impl: ObserveEngineStateUseCaseImpl,
    ): ObserveEngineStateUseCase

    @Binds
    abstract fun bindEnqueueAnnouncementUseCase(
        impl: EnqueueAnnouncementUseCaseImpl,
    ): EnqueueAnnouncementUseCase

    @Binds
    abstract fun bindObservePlaybackStateUseCase(
        impl: ObservePlaybackStateUseCaseImpl,
    ): ObservePlaybackStateUseCase
}
