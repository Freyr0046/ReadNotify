package com.freyr.readmynotify.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.freyr.readmynotify.domain.model.EngineErrorReason
import com.freyr.readmynotify.domain.model.TtsEngineState
import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * status 非 SUCCESS 一律視為「未安裝可用引擎」（PRD 3.1.3 唯一明確定義的
 * 初始化失敗情境）；純函式，供單元測試直接驗證映射邏輯，不需要真正建立
 * TextToSpeech 實例。
 */
internal fun mapInitStatusToErrorReason(status: Int): EngineErrorReason? =
    if (status == TextToSpeech.SUCCESS) null else EngineErrorReason.TTS_ENGINE_NOT_INSTALLED

internal fun mapLanguageResultToErrorReason(languageResult: Int): EngineErrorReason? =
    if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
        languageResult == TextToSpeech.LANG_NOT_SUPPORTED
    ) {
        EngineErrorReason.LANGUAGE_UNSUPPORTED
    } else {
        null
    }

@Singleton
class TtsEngineRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TtsEngineRepository {
        private val _engineState = MutableStateFlow<TtsEngineState>(TtsEngineState.NotReady)
        override val engineState: StateFlow<TtsEngineState> = _engineState

        private var textToSpeech: TextToSpeech? = null

        override suspend fun initialize(): Result<Unit> =
            runCatching {
                // runInitialCheck() 在每次重新檢查（重試、權限恢復）都會呼叫這裡，
                // 若不先關閉舊的引擎連線，重複初始化會累積洩漏底層 TTS 服務綁定。
                textToSpeech?.shutdown()
                textToSpeech = null

                suspendCancellableCoroutine { continuation ->
                    lateinit var tts: TextToSpeech
                    tts =
                        TextToSpeech(context) { status ->
                            val initErrorReason = mapInitStatusToErrorReason(status)
                            when {
                                initErrorReason != null -> {
                                    _engineState.value = TtsEngineState.Error(initErrorReason)
                                    if (continuation.isActive) {
                                        continuation.resumeWith(
                                            Result.failure(IllegalStateException("TTS init failed: status=$status")),
                                        )
                                    }
                                }

                                else -> {
                                    val languageErrorReason =
                                        mapLanguageResultToErrorReason(tts.setLanguage(Locale.TRADITIONAL_CHINESE))
                                    if (languageErrorReason != null) {
                                        _engineState.value = TtsEngineState.Error(languageErrorReason)
                                        if (continuation.isActive) {
                                            continuation.resumeWith(
                                                Result.failure(IllegalStateException("Language not supported")),
                                            )
                                        }
                                    } else {
                                        textToSpeech = tts
                                        _engineState.value = TtsEngineState.Ready
                                        if (continuation.isActive) {
                                            continuation.resumeWith(Result.success(Unit))
                                        }
                                    }
                                }
                            }
                        }
                }
            }.onFailure {
                if (_engineState.value !is TtsEngineState.Error) {
                    _engineState.value = TtsEngineState.Error(EngineErrorReason.INIT_FAILED)
                }
            }

        override suspend fun speak(
            text: String,
            utteranceId: String,
        ): Result<Unit> =
            runCatching {
                val tts = textToSpeech ?: error("TTS engine not initialized")

                suspendCancellableCoroutine { continuation ->
                    tts.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit

                            override fun onDone(utteranceId: String?) {
                                if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                            }

                            override fun onError(utteranceId: String?) {
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        Result.failure(IllegalStateException("TextToSpeech speak error")),
                                    )
                                }
                            }
                        },
                    )

                    val speakResult = tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
                    if (speakResult == TextToSpeech.ERROR && continuation.isActive) {
                        continuation.resumeWith(Result.failure(IllegalStateException("speak() returned ERROR")))
                    }
                }
            }
    }
