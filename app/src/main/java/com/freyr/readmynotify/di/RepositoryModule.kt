package com.freyr.readmynotify.di

import com.freyr.readmynotify.data.repository.InstalledAppRepositoryImpl
import com.freyr.readmynotify.data.repository.NotificationAccessRepositoryImpl
import com.freyr.readmynotify.data.repository.NotificationWhitelistRepositoryImpl
import com.freyr.readmynotify.data.repository.SpeechQueueRepositoryImpl
import com.freyr.readmynotify.data.repository.TtsEngineRepositoryImpl
import com.freyr.readmynotify.domain.repository.InstalledAppRepository
import com.freyr.readmynotify.domain.repository.NotificationAccessRepository
import com.freyr.readmynotify.domain.repository.NotificationWhitelistRepository
import com.freyr.readmynotify.domain.repository.SpeechQueueRepository
import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindNotificationWhitelistRepository(
        impl: NotificationWhitelistRepositoryImpl,
    ): NotificationWhitelistRepository

    @Binds
    abstract fun bindInstalledAppRepository(
        impl: InstalledAppRepositoryImpl,
    ): InstalledAppRepository

    @Binds
    abstract fun bindNotificationAccessRepository(
        impl: NotificationAccessRepositoryImpl,
    ): NotificationAccessRepository

    @Binds
    abstract fun bindTtsEngineRepository(
        impl: TtsEngineRepositoryImpl,
    ): TtsEngineRepository

    @Binds
    abstract fun bindSpeechQueueRepository(
        impl: SpeechQueueRepositoryImpl,
    ): SpeechQueueRepository
}
