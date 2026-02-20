package org.example.notification

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.Instant
import java.util.UUID

@Service
class TelegramConnectionService(
    private val repo: TelegramConnectionRepository,
    private val organizationClient: OrganizationClient
) {

    fun generateLinkToken(userId: Long, orgId: Long): String {
        val emp = organizationClient.getEmp(EmpRequest(userId, orgId))

        val token = UUID.randomUUID().toString()

        val conn = repo.findByEmployeeId(emp.id) ?: TelegramConnection(employeeId = emp.id)

        conn.linkToken = token
        conn.tokenExpiresAt = Instant.now().plusSeconds(600)
        conn.tokenUsed = false

        repo.save(conn)
        return token
    }

    fun confirmLink(token: String, chatId: Long, telegramUserId: Long): Boolean {
        val conn = repo.findByLinkToken(token) ?: return false
        if (conn.tokenUsed) return false
        if (conn.tokenExpiresAt!!.isBefore(Instant.now())) return false

        conn.chatId = chatId
        conn.telegramUserId = telegramUserId
        conn.linkedAt = Instant.now()
        conn.tokenUsed = true

        repo.save(conn)
        return true
    }

    fun getConnections(employeeIds: List<Long>): List<TelegramConnection> =
        repo.findAllByEmployeeIdIn(employeeIds)
}


@Service
class NotificationService(
    private val telegramBotService: TelegramBotService,
    private val connectionService: TelegramConnectionService,
    private val taskClient: TaskClient,
    private val notificationRepo: NotificationMessageRepository
) {

    fun sendNotification(req: ActionRequest) {
        val employees = taskClient.getEmployee(req.taskId)
        val employeeIds = employees.map { it.accountId }

        val connections = connectionService.getConnections(employeeIds)
            .filter { it.chatId != null }

        connections.forEach {
            telegramBotService.sendMessage(it.chatId!!, req.content)

            notificationRepo.save(
                NotificationMessage(
                    employeeId = it.employeeId,
                    message = req.content,
                    status = NotificationStatus.SENT,
                    sentAt = Instant.now()
                )
            )
        }
    }
}


@Service
class TelegramBotService(
    private val bot: NotificationTelegramBot
) {
    fun sendMessage(chatId: Long, text: String) {
        bot.execute(SendMessage(chatId.toString(), text))
    }
}
