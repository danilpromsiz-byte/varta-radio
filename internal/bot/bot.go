package bot

import (
	"context"
	"fmt"
	"log"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
)

// Bot manages the Telegram bot for SafeComm.
type Bot struct {
	api       *tgbotapi.BotAPI
	webAppURL string
}

// NewBot initializes a new Telegram bot instance.
func NewBot(token, webAppURL string) (*Bot, error) {
	if token == "" {
		return nil, fmt.Errorf("bot token is empty")
	}

	api, err := tgbotapi.NewBotAPI(token)
	if err != nil {
		return nil, fmt.Errorf("init telegram bot: %w", err)
	}

	log.Printf("[Bot] Authorized on account @%s", api.Self.UserName)
	return &Bot{
		api:       api,
		webAppURL: webAppURL,
	}, nil
}

// Start listens for updates until context is canceled.
func (b *Bot) Start(ctx context.Context) {
	u := tgbotapi.NewUpdate(0)
	u.Timeout = 60

	updates := b.api.GetUpdatesChan(u)

	for {
		select {
		case <-ctx.Done():
			b.api.StopReceivingUpdates()
			log.Println("[Bot] Bot stopped")
			return
		case update, ok := <-updates:
			if !ok {
				return
			}
			if update.Message == nil {
				continue
			}

			b.handleMessage(update.Message)
		}
	}
}

func (b *Bot) handleMessage(msg *tgbotapi.Message) {
	chatID := msg.Chat.ID

	if msg.IsCommand() {
		switch msg.Command() {
		case "start":
			b.sendStartMessage(chatID)
		case "help":
			b.sendHelpMessage(chatID)
		default:
			reply := tgbotapi.NewMessage(chatID, "Unknown command. Use /start to open SafeComm Mini App.")
			_, _ = b.api.Send(reply)
		}
		return
	}

	// Default reply
	b.sendStartMessage(chatID)
}

func (b *Bot) sendStartMessage(chatID int64) {
	welcomeText := "🛡️ **SafeComm — Ситуационная безопасность сообщества**\n\n" +
		"Закрытая система оперативного оповещения и голосовой связи для вашей группы.\n\n" +
		"• 📡 Рация прямого эфира в закрытых группах\n" +
		"• 🚨 Экстренные оповещения с геопозицией и фото\n" +
		"• 🗺️ Интерактивная карта безопасности и радиус тревоги\n\n" +
		"Нажмите кнопку ниже, чтобы открыть приложение:"

	reply := tgbotapi.NewMessage(chatID, welcomeText)
	reply.ParseMode = tgbotapi.ModeMarkdown

	if b.webAppURL != "" {
		webAppBtn := tgbotapi.InlineKeyboardButton{
			Text:   "🚀 Открыть SafeComm",
			WebApp: &tgbotapi.WebAppInfo{URL: b.webAppURL},
		}
		reply.ReplyMarkup = tgbotapi.NewInlineKeyboardMarkup(
			tgbotapi.NewInlineKeyboardRow(webAppBtn),
		)
	}

	if _, err := b.api.Send(reply); err != nil {
		log.Printf("[Bot] Failed to send start message: %v", err)
	}
}

func (b *Bot) sendHelpMessage(chatID int64) {
	helpText := "ℹ️ **Помощь SafeComm**\n\n" +
		"/start — Запустить приложение и открыть меню\n" +
		"/help — Справка по работе с сервисом\n\n" +
		"Для создания или входа в группу безопасности используйте кнопку запуска внутри приложения."

	reply := tgbotapi.NewMessage(chatID, helpText)
	reply.ParseMode = tgbotapi.ModeMarkdown
	if _, err := b.api.Send(reply); err != nil {
		log.Printf("[Bot] Failed to send help message: %v", err)
	}
}
