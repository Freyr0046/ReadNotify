package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.TtsEngineState
import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveEngineStateUseCaseImpl
    @Inject
    constructor(
        private val repository: TtsEngineRepository,
    ) : ObserveEngineStateUseCase {
        override fun invoke(): StateFlow<TtsEngineState> = repository.engineState
    }
