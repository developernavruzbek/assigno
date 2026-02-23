package org.example.notification

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
@Component
class NotificationTelegramBot(
    @Value("\${telegram.bot.token}") private val tokenValue: String,
    @Value("\${telegram.bot.username}") private val usernameValue: String,
    private val connectionService: TelegramConnectionService,
) : TelegramLongPollingBot() {

    override fun getBotToken(): String = tokenValue
    override fun getBotUsername(): String = usernameValue

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage()) return
        val msg = update.message!!

        if (msg.text.startsWith("/start")) {
            val token = msg.text.replace("/start", "").trim()

            if (token.isBlank()) {
                execute(
                    SendMessage(
                        msg.chatId.toString(),
                        "❗️ Iltimos, tokenni yuboring:\n/start <token>"
                    )
                )
                return
            }

            val orgName = connectionService.confirmLink(
                token = token,
                chatId = msg.chatId,
                telegramUserId = msg.from.id
            )
            if (orgName != null) {
                execute(
                    SendMessage(
                        msg.chatId.toString(),
                        "✅ Sizning Telegram hisobingiz **$orgName** tashkilot botiga muvaffaqiyatli bog‘landi!"
                    )
                )
            } else {
                execute(
                    SendMessage(
                        msg.chatId.toString(),
                        "❌ Token xato yoki eskirgan. Iltimos, tekshirib qayta yuboring."
                    )
                )
            }
        }
    }

}

