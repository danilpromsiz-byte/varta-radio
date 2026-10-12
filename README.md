# 🛡️ SafeComm — Community Safety App

Приложение безопасности на базе Telegram Mini App для защищённой коммуникации в сообществах.

## Возможности

### 🎙️ Рация (Walkie-Talkie)
- Push-to-Talk голосовая связь в реальном времени
- Мгновенная трансляция голоса всем участникам группы
- История всех голосовых сообщений с возможностью прослушивания

### 🚨 Тревога
- Оповещение всех участников об опасности
- 6 типов опасностей с уникальными иконками и звуками
- Привязка к геолокации — отображение на карте
- Звуковое уведомление зависит от расстояния (настраиваемый радиус)
- Защита от ложных нажатий: выбор типа → long-press 2 сек

### 📷 Фотофиксация
- Фото опасности с автоматической привязкой к геолокации
- Мгновенное оповещение всех участников
- Отображение на общей карте

### 👥 Сообщества
- Закрытые группы с паролем и invite-кодом
- Независимые никнеймы и аватары
- Управление участниками (администраторы и участники)

## Технологии

| Компонент | Технология |
|-----------|-----------|
| Backend | Go 1.21+ |
| Bot | telegram-bot-api v5 |
| Frontend | Telegram Mini App (vanilla JS) |
| Real-time | WebSocket (gorilla/websocket) |
| Database | SQLite (modernc.org/sqlite) |
| Maps | Leaflet.js + OpenStreetMap |
| Audio | Web Audio API + MediaRecorder |

## Быстрый старт

### 1. Предварительные требования
- Go 1.21+
- Telegram Bot Token (от [@BotFather](https://t.me/BotFather))

### 2. Создание бота
1. Напишите `/newbot` в [@BotFather](https://t.me/BotFather)
2. Получите токен бота
3. Включите Mini App: `/newapp` → привяжите URL вашего сервера

### 3. Запуск

```bash
# Клонирование и переход в папку
cd c:\WORK\GO\safecomm

# Установка зависимостей
go mod tidy

# Настройка переменных окружения
set BOT_TOKEN=your_telegram_bot_token
set JWT_SECRET=your_jwt_secret_key_at_least_32_chars
set PORT=8080

# Запуск
go run cmd/server/main.go
```

### 4. Для разработки с HTTPS (необходим для Mini App)

```bash
# Используйте ngrok для проксирования
ngrok http 8080
```

Затем укажите полученный HTTPS URL в настройках Mini App в BotFather.

## Структура проекта

```
safecomm/
├── cmd/server/main.go          # Точка входа
├── internal/
│   ├── api/                    # HTTP API handlers
│   │   ├── router.go
│   │   ├── middleware.go
│   │   ├── auth.go
│   │   ├── groups.go
│   │   ├── voice.go
│   │   └── alerts.go
│   ├── bot/bot.go              # Telegram Bot
│   ├── db/                     # SQLite database
│   │   ├── db.go
│   │   └── queries.go
│   ├── geo/geo.go              # Geolocation utils
│   ├── models/models.go        # Data structures
│   └── ws/hub.go               # WebSocket hub
├── web/                        # Telegram Mini App
│   ├── index.html
│   ├── css/app.css
│   ├── js/
│   │   ├── app.js
│   │   ├── map.js
│   │   ├── voice.js
│   │   ├── alerts.js
│   │   └── ws.js
│   └── assets/
│       ├── icons/              # SVG маркеры
│       └── sounds/             # Звуки тревог
├── uploads/                    # Загруженные файлы
│   ├── voice/
│   └── photos/
├── go.mod
└── README.md
```

## API Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/auth/init` | Аутентификация через Telegram initData |
| POST | `/api/profile` | Создание/обновление профиля |
| GET | `/api/profile` | Получение профиля |
| PUT | `/api/settings` | Обновление настроек |
| POST | `/api/groups` | Создание группы |
| POST | `/api/groups/join` | Вступление в группу |
| GET | `/api/groups` | Список групп пользователя |
| GET | `/api/groups/{id}/members` | Участники группы |
| DELETE | `/api/groups/{id}/members/{uid}` | Удаление участника |
| POST | `/api/groups/{id}/voice` | Отправка голосового |
| GET | `/api/groups/{id}/voice` | История голосовых |
| POST | `/api/groups/{id}/alerts` | Создание тревоги |
| GET | `/api/groups/{id}/alerts` | Активные тревоги |
| PUT | `/api/groups/{id}/alerts/{aid}/resolve` | Снятие тревоги |
| WS | `/ws?token=JWT&group_id=ID` | WebSocket real-time |

## Переменные окружения

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `BOT_TOKEN` | Telegram Bot API token | *обязательно* |
| `JWT_SECRET` | Ключ для подписи JWT | *обязательно* |
| `PORT` | Порт HTTP сервера | `8080` |
| `DB_PATH` | Путь к файлу SQLite | `data/safecomm.db` |

## Лицензия

MIT
