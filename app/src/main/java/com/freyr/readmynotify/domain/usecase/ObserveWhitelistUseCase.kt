package com.freyr.readmynotify.domain.usecase

import kotlinx.coroutines.flow.Flow

// LOCKED — do not modify during Phase 5
interface ObserveWhitelistUseCase {
    operator fun invoke(): Flow<Set<String>>
}
