package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.IncomingNotification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BuildAnnouncementUseCaseImplTest {

    private val useCase = BuildAnnouncementUseCaseImpl()

    private fun notification(title: String?, content: String?) = IncomingNotification(
        packageName = "com.example.line",
        title = title,
        content = content,
        groupName = null,
        category = null,
        tag = "tag",
    )

    @Test
    fun `null title returns null`() {
        assertNull(useCase("Line", notification(title = null, content = "hello")))
    }

    @Test
    fun `null content returns null`() {
        assertNull(useCase("Line", notification(title = "Alice", content = null)))
    }

    @Test
    fun `empty content returns null`() {
        assertNull(useCase("Line", notification(title = "Alice", content = "")))
    }

    @Test
    fun `content of exactly 50 chars is not truncated`() {
        val content = "a".repeat(50)
        val result = useCase("Line", notification(title = "Alice", content = content))
        assertEquals("Line傳來新訊息，內容是：$content", result?.message)
    }

    @Test
    fun `content of 51 chars is truncated with suffix`() {
        val content = "a".repeat(51)
        val expectedBody = "a".repeat(50) + "...等省略內容"
        val result = useCase("Line", notification(title = "Alice", content = content))
        assertEquals("Line傳來新訊息，內容是：$expectedBody", result?.message)
    }

    @Test
    fun `content of 49 chars is not truncated`() {
        val content = "a".repeat(49)
        val result = useCase("Line", notification(title = "Alice", content = content))
        assertEquals("Line傳來新訊息，內容是：$content", result?.message)
    }

    @Test
    fun `url in content is replaced before truncation`() {
        val content = "看這個 https://example.com/very/long/path?query=1"
        val result = useCase("Line", notification(title = "Alice", content = content))
        assertEquals("Line傳來新訊息，內容是：看這個 網址", result?.message)
    }

    @Test
    fun `appLabel is preserved on the announcement`() {
        val result = useCase("Facebook", notification(title = "Bob", content = "hi"))
        assertEquals("Facebook", result?.appLabel)
    }
}
