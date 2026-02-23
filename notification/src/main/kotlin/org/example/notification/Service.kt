package org.example.notification

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.Instant
import java.util.UUID

@Service
class TelegramConnectionService(
    private val repo: TelegramConnectionRepository,
    private val organizationClient: OrganizationClient,
    @Value("\${telegram.bot.username}") private val botUsername: String
) {

    fun generateLinkToken(userId: Long, orgId: Long): String {
        val emp = organizationClient.getEmp(EmpRequest(userId, orgId))
        val org = organizationClient.getOne(orgId)
          println("Current : $org ==============")
        val token = UUID.randomUUID().toString()

        val conn = TelegramConnection(
            employeeLocalNumber = emp.localNumber,
            organizationId = orgId,
            organizationName = org.name,
            linkToken = token,
            tokenExpiresAt = Instant.now().plusSeconds(600),
            tokenUsed = false
        )

        repo.save(conn)

        return "https://t.me/$botUsername?start=$token"
    }

    fun confirmLink(token: String, chatId: Long, telegramUserId: Long): String? {
        val conn = repo.findByLinkToken(token) ?: return null

        if (conn.tokenUsed) return null
        if (conn.tokenExpiresAt!!.isBefore(Instant.now())) return null

        val alreadyLinked =
            repo.existsByEmployeeLocalNumberAndOrganizationIdAndChatId(
                conn.employeeLocalNumber,
                conn.organizationId,
                chatId
            )

        if (alreadyLinked) {
            return "ALREADY_LINKED"
        }

        conn.chatId = chatId
        conn.telegramUserId = telegramUserId
        conn.linkedAt = Instant.now()
        conn.tokenUsed = true

        repo.save(conn)
        return conn.organizationName
    }

    fun getConnections(employeeLocalNumbers: List<Long>, orgId: Long): List<TelegramConnection> =
        repo.findAllByEmployeeLocalNumberInAndOrganizationId(employeeLocalNumbers, orgId)
}
@Service
class NotificationService(
    private val telegramBotService: TelegramBotService,
    private val connectionService: TelegramConnectionService,
    private val notificationRepo: NotificationMessageRepository
) {

    fun sendNotification(req: ActionRequest) {

        val orgId = currentOrgId()!!
        println("Current organization: $orgId")

        val connections = connectionService
            .getConnections(req.employeeLocalNumbers, orgId)
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
