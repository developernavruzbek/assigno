package org.example.notification

import org.springframework.web.bind.annotation.*
/*
@RestController
@RequestMapping("/telegram")
class TelegramController(
    private val telegramConnectionService: TelegramConnectionService,
    private val notificationMessageService: NotificationMessageService
) {

    @GetMapping("/link")
    fun getLink(): String = telegramConnectionService.generateLinkForCurrentUser()

    @PostMapping("/notify")
    fun sendTaskMessage(@RequestBody request: ActionRequest) =
        notificationMessageService.sendTaskMessage(request.taskId, request.content)
}


 */
@RestController
@RequestMapping("telegram")
class TelegramLinkController(
    private val telegramService: TelegramConnectionService,
    private val orgClient: OrganizationClient
) {

    @PostMapping("/link")
    fun generateLink(): String {
        val userId = userId()!!.toLong()
        val orgId = currentOrgId()!!
        println("==========${currentOrgId()!!} ===================")


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
