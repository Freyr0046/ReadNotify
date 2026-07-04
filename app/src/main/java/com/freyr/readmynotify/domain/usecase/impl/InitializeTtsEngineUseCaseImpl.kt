package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import javax.inject.Inject

class InitializeTtsEngineUseCaseImpl @Inject constructor(
    private val repository: TtsEngineRepository,
) : InitializeTtsEngineUseCase {

    override suspend fun invoke(): Result<Unit> = repository.initialize()
}
