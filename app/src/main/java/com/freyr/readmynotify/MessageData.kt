package com.freyr.readmynotify

data class MessageData(
    val title: String? = "",
    val content: String? = "",
    val packageName: String? = "",
    val time: Long
)
