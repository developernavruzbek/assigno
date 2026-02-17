package org.example.notification

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class TelegramBot(
    private val telegramConnectionService: TelegramConnectionService,
    @Value("\${telegram.bot.username}") private val username: String,
    @Value("\${telegram.bot.token}") private val token: String
) : TelegramLongPollingBot(token) {

    override fun getBotUsername() = username

    @Deprecated("Deprecated in Java")
    override fun getBotToken() = token

    override fun onUpdateReceived(update: Update) {

        val msg = update.message ?: return
        val text = msg.text ?: return

        try {
            if (text.startsWith("/start ")) {

                val linkToken = text.removePrefix("/start ").trim()

                telegramConnectionService.linkTelegram(
                    linkToken,
                    msg.chat.id,
                    msg.from.id
                )

                execute(
                    SendMessage(
                        msg.chat.id.toString(),
                        "✅ HRMS Telegram connected"
                    )
                )
            }

        } catch (e: Exception) {
            execute(
                SendMessage(
                    msg.chat.id.toString(),
                    "❌ Linking failed: ${e.message}"
                )
            )
        }
    }
}