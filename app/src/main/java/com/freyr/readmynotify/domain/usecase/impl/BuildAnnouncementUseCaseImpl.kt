package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.model.IncomingNotification
import com.freyr.readmynotify.domain.usecase.BuildAnnouncementUseCase
import javax.inject.Inject

class BuildAnnouncementUseCaseImpl @Inject constructor() : BuildAnnouncementUseCase {

    override fun invoke(appLabel: String, notification: IncomingNotification): Announcement? {
        val title = notification.title
        val content = notification.content
        if (title.isNullOrEmpty() || content.isNullOrEmpty()) return null

        val withoutUrls = content.replace(URL_REGEX, URL_PLACEHOLDER)
        val body = if (withoutUrls.length > MAX_CONTENT_LENGTH) {
            withoutUrls.take(MAX_CONTENT_LENGTH) + TRUNCATION_SUFFIX
        } else {
            withoutUrls
        }

        return Announcement(
            appLabel = appLabel,
            message = "${appLabel}傳來新訊息，內容是：$body",
        )
    }

    private companion object {
        val URL_REGEX = Regex("https?://\\S+")
        const val URL_PLACEHOLDER = "網址"
        const val MAX_CONTENT_LENGTH = 50
        const val TRUNCATION_SUFFIX = "...等省略內容"
    }
}
