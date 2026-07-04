package com.freyr.readmynotify.di

import com.freyr.readmynotify.data.repository.NotificationWhitelistRepositoryImpl
import com.freyr.readmynotify.domain.repository.NotificationWhitelistRepository
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
}
