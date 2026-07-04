package com.freyr.readmynotify.domain.model

/**
 * title/content 為 nullable：依 PRD 3.1.1，任一為 null 或空字串時，
 * 呼叫端必須靜音跳過，不得進入 TTS 播報流程。
 */
data class IncomingNotification(
    val packageName: String,
    val title: String?,
    val content: String?,
    val groupName: String?,
    val category: String?,
    val tag: String?,
)
