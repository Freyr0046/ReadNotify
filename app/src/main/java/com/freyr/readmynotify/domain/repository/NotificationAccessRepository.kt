package com.freyr.readmynotify.domain.repository

// LOCKED — do not modify during Phase 5
interface NotificationAccessRepository {
    fun isNotificationAccessGranted(): Boolean
}
