package com.freyr.readmynotify.di

import javax.inject.Qualifier

/**
 * 生命週期綁定 App 進程、非結構化的背景 CoroutineScope，僅供需要跨越單次
 * ViewModel/Service 呼叫存活的背景工作使用（例如 SpeechQueueRepositoryImpl
 * 的佇列消費迴圈）。一般業務邏輯應使用 viewModelScope 或呼叫端自身的
 * suspend 函式邊界，不應預設注入此 scope。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationCoroutineScope
