package org.example.notification

data class BulkNotificationRequest(
    val employeeIds: List<Long>,
    val message: String
)

data class TelegramUpdateRequest(
    val chatId: Long,
    val telegramUserId: Long
)