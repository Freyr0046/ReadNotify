package com.freyr.readmynotify.data.repository

import android.speech.tts.TextToSpeech
import com.freyr.readmynotify.domain.model.EngineErrorReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * TtsEngineRepositoryImpl 本身建立真正的 android.speech.tts.TextToSpeech
 * 實例，在無 Robolectric 的純 JVM 單元測試下無法執行（框架方法一律拋
 * "not mocked"）。因此只針對其抽出的純函式（OnInitListener 錯誤碼映射
 * 邏輯）撰寫單元測試，完整的初始化/語系流程於 Task 18 手動實機驗證。
 */
class TtsEngineRepositoryImplTest {

    @Test
    fun `SUCCESS status maps to no error`() {
        assertNull(mapInitStatusToErrorReason(TextToSpeech.SUCCESS))
    }

    @Test
    fun `non-SUCCESS status maps to TTS_ENGINE_NOT_INSTALLED`() {
        assertEquals(
            EngineErrorReason.TTS_ENGINE_NOT_INSTALLED,
            mapInitStatusToErrorReason(TextToSpeech.ERROR),
        )
    }

    @Test
    fun `LANG_MISSING_DATA maps to LANGUAGE_UNSUPPORTED`() {
        assertEquals(
            EngineErrorReason.LANGUAGE_UNSUPPORTED,
            mapLanguageResultToErrorReason(TextToSpeech.LANG_MISSING_DATA),
        )
    }

    @Test
    fun `LANG_NOT_SUPPORTED maps to LANGUAGE_UNSUPPORTED`() {
        assertEquals(
            EngineErrorReason.LANGUAGE_UNSUPPORTED,
            mapLanguageResultToErrorReason(TextToSpeech.LANG_NOT_SUPPORTED),
        )
    }

    @Test
    fun `LANG_AVAILABLE maps to no error`() {
        assertNull(mapLanguageResultToErrorReason(TextToSpeech.LANG_AVAILABLE))
    }
}
