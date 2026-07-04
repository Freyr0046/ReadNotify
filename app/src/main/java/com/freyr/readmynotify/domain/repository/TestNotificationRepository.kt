package com.freyr.readmynotify.domain.repository

/**
 * 非 Phase 3 鎖定合約的一部分（詳見 doc/phase4-tasks.md Task 7 備註）：
 * SendTestNotificationUseCase 需要透過 Android 通知系統發出一則測試通知，
 * 但 Domain 層不可持有 Context，因此在實作階段新增此介面把該動作下放到
 * Data 層，UseCase 只依賴這個介面。
 */
interface TestNotificationRepository {
    suspend fun postTestNotification(): Result<Unit>
}
