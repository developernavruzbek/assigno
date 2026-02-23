package org.example.notification

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.Instant
import java.util.UUID
import kotlin.math.log

@Service
class TelegramConnectionService(
    private val repo: TelegramConnectionRepository,
    private val organizationClient: OrganizationClient,
    @Value("\${telegram.bot.username}") private val botUsername: String
) {
    fun generateLinkToken(userId: Long, orgId: Long): String {
        val emp = organizationClient.getEmp(EmpRequest(userId, orgId))

        val token = UUID.randomUUID().toString()

        val conn = repo.findByEmployeeLocalNumber(emp.localNumber)
            ?: TelegramConnection(employeeLocalNumber = emp.localNumber)

        conn.organizationName = organizationClient.getOne(orgId).name
        conn.linkToken = token
        conn.tokenExpiresAt = Instant.now().plusSeconds(600)
        conn.tokenUsed = false

        repo.save(conn)

        return "https://t.me/$botUsername?start=$token"
    }

    fun confirmLink(token: String, chatId: Long, telegramUserId: Long): String? {
        val conn = repo.findByLinkToken(token) ?: return null
        if (conn.tokenUsed) return null
        if (conn.tokenExpiresAt!!.isBefore(Instant.now())) return null

        conn.chatId = chatId
        conn.telegramUserId = telegramUserId
        conn.linkedAt = Instant.now()
        conn.tokenUsed = true

        repo.save(conn)
        return conn.organizationName   // 👈 MUHIM!
    }

    fun getConnections(employeeLocalNumbers: List<Long>): List<TelegramConnection> =
        repo.findAllByEmployeeLocalNumberIn(employeeLocalNumbers)

}

@Service
class NotificationService(
    private val telegramBotService: TelegramBotService,
    private val connectionService: TelegramConnectionService,
    private val notificationRepo: NotificationMessageRepository
) {

    fun sendNotification(req: ActionRequest) {
        println("======${req.employeeLocalNumbers}============")
        val connections = connectionService
            .getConnections(req.employeeLocalNumbers)
            .filter { it.chatId != null }

        connections.forEach {
            telegramBotService.sendMessage(it.chatId!!, req.content)

            notificationRepo.save(
                NotificationMessage(
                    employeeLocalNumber = it.employeeLocalNumber,
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
