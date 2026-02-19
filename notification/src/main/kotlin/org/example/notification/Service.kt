package org.example.notification

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID


import org.telegram.telegrambots.meta.api.methods.send.SendMessage

interface TelegramSender {
    fun send(chatId: Long, text: String): Boolean
}

interface TelegramConnectionService {

    fun generateLinkToken(employeeId: Long): String

    fun getTelegramLink(employeeId: Long): String

    fun linkTelegram(
        token: String,
        chatId: Long,
        telegramUserId: Long
    )

    fun getChatId(employeeId: Long): Long?
}

interface NotificationMessageService {
    fun send(employeeId: Long, message: String)
    fun sendBulk(employeeIds: List<Long>, message: String)
}

@Service
class TelegramSenderImpl(
    private val bot: TelegramBot
) : TelegramSender {

    override fun send(chatId: Long, text: String): Boolean {
        return try {
            bot.execute(SendMessage(chatId.toString(), text))
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Service
@Transactional
class TelegramConnectionServiceImpl(
    private val repo: TelegramConnectionRepository,
    private val employeeClient: EmployeeClient,
    @Value("\${telegram.bot.username}") private val botUsername: String
) : TelegramConnectionService {

    override fun generateLinkToken(employeeId: Long): String {

        val conn = repo.findById(employeeId)
            .orElse(TelegramConnection(employeeId = employeeId))

        conn.linkToken = UUID.randomUUID().toString()
        conn.tokenExpiresAt = Instant.now().plusSeconds(900)
        conn.tokenUsed = false

        repo.save(conn)

        return conn.linkToken!!
    }

    override fun getTelegramLink(employeeId: Long): String {
        val token = generateLinkToken(employeeId)
        return "https://t.me/$botUsername?start=$token"
    }

    override fun linkTelegram(
        token: String,
        chatId: Long,
        telegramUserId: Long
    ) {

        val conn = repo.findByLinkToken(token)
            ?: throw IllegalArgumentException("Invalid token")

        if (conn.tokenUsed)
            throw IllegalStateException("Token already used")

        if (conn.tokenExpiresAt!!.isBefore(Instant.now()))
            throw IllegalStateException("Token expired")

        conn.chatId = chatId
        conn.telegramUserId = telegramUserId
        conn.linkedAt = Instant.now()
        conn.tokenUsed = true

        repo.save(conn)

        employeeClient.updateTelegram(
            conn.employeeId,
            TelegramUpdateRequest(chatId, telegramUserId)
        )
    }

    override fun getChatId(employeeId: Long): Long? {
        return repo.findById(employeeId)
            .map { it.chatId }
            .orElse(null)
    }
}

@Service
@Transactional
class NotificationMessageServiceImpl(
    private val messageRepo: NotificationMessageRepository,
    private val connectionService: TelegramConnectionService,
    private val telegramSender: TelegramSender
) : NotificationMessageService {

    override fun send(employeeId: Long, message: String) {

        val chatId = connectionService.getChatId(employeeId) ?: return

        val ok = telegramSender.send(chatId, message)

        if (ok) {
            messageRepo.save(
                NotificationMessage(
                    employeeId = employeeId,
                    message = message,
                    sentAt = Instant.now()
                )
            )
        }
    }

    override fun sendBulk(employeeIds: List<Long>, message: String) {
        employeeIds.forEach { employeeId ->
            send(employeeId, message)
        }
    }
}