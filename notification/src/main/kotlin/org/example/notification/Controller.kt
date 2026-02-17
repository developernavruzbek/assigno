package org.example.notification

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/telegram")
class TelegramConnectionController(
    private val telegramConnectionService: TelegramConnectionService,
    private val notificationMessageService: NotificationMessageService
) {

    @GetMapping("/link/{employeeId}")
    fun getTelegramLink(@PathVariable employeeId: Long): String {
        return telegramConnectionService.getTelegramLink(employeeId)
    }

    @PostMapping("/notify")
    fun sendNotifications(@RequestBody request: BulkNotificationRequest) {
        notificationMessageService.sendBulk(request.employeeIds, request.message)
    }
}
