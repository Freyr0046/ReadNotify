package com.freyr.readmynotify.domain.model

enum class EngineErrorReason {
    /** 系統未安裝任何 TTS 引擎 */
    TTS_ENGINE_NOT_INSTALLED,

    /** OnInitListener 回傳 LANG_MISSING_DATA / LANG_NOT_SUPPORTED */
    LANGUAGE_UNSUPPORTED,

    /** 其他初始化失敗 */
    INIT_FAILED,
}
