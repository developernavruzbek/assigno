package org.example.notification

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("telegram")
class TelegramLinkController(
    private val telegramService: TelegramConnectionService
) {

    @PostMapping("/link")
    fun generateLink(): String {
        val userId = userId()!!.toLong()
        val orgId = currentOrgId()!!

        return telegramService.generateLinkToken(userId, orgId)
    }
}


@RestController
@RequestMapping("notification")
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping("/send")
    fun send(@RequestBody req: ActionRequest) {
        notificationService.sendNotification(req)
    }
}
