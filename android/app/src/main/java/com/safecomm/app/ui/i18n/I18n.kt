package com.safecomm.app.ui.i18n

import android.content.Context

object I18n {

    fun t(lang: AppLanguage, ru: String, uk: String, en: String): String {
        return when (lang) {
            AppLanguage.EN -> en
            AppLanguage.UK -> uk
            AppLanguage.RU -> ru
        }
    }

    fun t(context: Context, ru: String, uk: String, en: String): String {
        return t(LanguageManager.getLanguage(context), ru, uk, en)
    }

    // --- Common & App Info ---
    fun appTitle(c: Context) = "Varta GO"
    fun appTitle(lang: AppLanguage = AppLanguage.EN) = "Varta GO"
    fun appSubtitle(lang: AppLanguage) = t(
        lang,
        "Тактическая защищённая радиосвязь",
        "Тактичний захищений радіозв'язок",
        "Tactical Secure Radio Communications"
    )
    fun appSubtitle(c: Context) = appSubtitle(LanguageManager.getLanguage(c))

    fun encryptionBadge(lang: AppLanguage) = t(
        lang,
        "AES-256 Защищённый P2P",
        "AES-256 Захищений P2P",
        "AES-256 Retained P2P"
    )
    fun encryptionBadge(c: Context) = encryptionBadge(LanguageManager.getLanguage(c))

    fun online(lang: AppLanguage) = t(lang, "в сети", "у мережі", "online")
    fun online(c: Context) = online(LanguageManager.getLanguage(c))
    fun offline(lang: AppLanguage) = t(lang, "Не подключено", "Не підключено", "Disconnected")
    fun offline(c: Context) = offline(LanguageManager.getLanguage(c))
    fun connecting(lang: AppLanguage) = t(lang, "Подключение к сети...", "Підключення до мережі...", "Connecting to network...")
    fun connecting(c: Context) = connecting(LanguageManager.getLanguage(c))
    fun cancel(lang: AppLanguage) = t(lang, "Отмена", "Скасувати", "Cancel")
    fun cancel(c: Context) = cancel(LanguageManager.getLanguage(c))
    fun save(lang: AppLanguage) = t(lang, "Сохранить", "Зберегти", "Save")
    fun save(c: Context) = save(LanguageManager.getLanguage(c))
    fun close(lang: AppLanguage) = t(lang, "Закрыть", "Закрити", "Close")
    fun close(c: Context) = close(LanguageManager.getLanguage(c))
    fun continueBtn(lang: AppLanguage) = t(lang, "Продолжить", "Продовжити", "Continue")
    fun continueBtn(c: Context) = continueBtn(LanguageManager.getLanguage(c))
    fun you(lang: AppLanguage) = t(lang, "(Вы)", "(Ви)", "(You)")
    fun you(c: Context) = you(LanguageManager.getLanguage(c))
    fun copiedToClipboard(c: Context) = t(c, "Скопировано в буфер", "Скопійовано в буфер", "Copied to clipboard")

    // --- Language Selection ---
    fun selectLanguageTitle(lang: AppLanguage) = t(
        lang,
        "Выберите язык интерфейса",
        "Оберіть мову інтерфейсу",
        "Select Interface Language"
    )
    fun selectLanguageTitle(c: Context) = selectLanguageTitle(LanguageManager.getLanguage(c))
    fun selectLanguageSubtitle(lang: AppLanguage) = t(
        lang,
        "Вы можете изменить язык в любое время в настройках",
        "Ви можете змінити мову в будь-який час у налаштуваннях",
        "You can change the language anytime in settings"
    )
    fun selectLanguageSubtitle(c: Context) = selectLanguageSubtitle(LanguageManager.getLanguage(c))

    // --- Auth & Community Screen (Complete Trilingual) ---
    fun callSignFieldLabel(lang: AppLanguage) = t(lang, "Ваш позывной в эфире *", "Ваш позивний в ефірі *", "Your Callsign in Air *")
    fun callSignFieldLabel(c: Context) = callSignFieldLabel(LanguageManager.getLanguage(c))
    fun callSignFieldPlaceholder(lang: AppLanguage) = t(lang, "Комбат, Дозор-1...", "Комбат, Дозор-1...", "Falcon, Ranger-1...")
    fun callSignFieldPlaceholder(c: Context) = callSignFieldPlaceholder(LanguageManager.getLanguage(c))

    fun tabJoinRoom(lang: AppLanguage) = t(lang, "Войти в комнату", "Увійти в кімнату", "Join Room")
    fun tabJoinRoom(c: Context) = tabJoinRoom(LanguageManager.getLanguage(c))
    fun tabCreateRoom(lang: AppLanguage) = t(lang, "Создать комнату", "Створити кімнату", "Create Room")
    fun tabCreateRoom(c: Context) = tabCreateRoom(LanguageManager.getLanguage(c))

    fun joinRoomPrompt(lang: AppLanguage) = t(lang, "Введите название или номер комнаты, созданной командиром:", "Введіть назву або номер кімнати, створеної командиром:", "Enter room name or number created by commander:")
    fun joinRoomPrompt(c: Context) = joinRoomPrompt(LanguageManager.getLanguage(c))

    fun roomFieldLabel(lang: AppLanguage) = t(lang, "Номер или название комнаты *", "Номер або назва кімнати *", "Room number or name *")
    fun roomFieldLabel(c: Context) = roomFieldLabel(LanguageManager.getLanguage(c))
    fun roomFieldPlaceholder(lang: AppLanguage) = t(lang, "102 или Сектор-3", "102 або Сектор-3", "102 or Sector-3")
    fun roomFieldPlaceholder(c: Context) = roomFieldPlaceholder(LanguageManager.getLanguage(c))

    fun passwordFieldLabel(lang: AppLanguage) = t(lang, "Секретный пароль комнаты *", "Секретний пароль кімнати *", "Room Secret Password *")
    fun passwordFieldLabel(c: Context) = passwordFieldLabel(LanguageManager.getLanguage(c))
    fun passwordFieldPlaceholder(lang: AppLanguage) = t(lang, "Пароль для доступа в эфир", "Пароль для доступу в ефір", "Password for room access")
    fun passwordFieldPlaceholder(c: Context) = passwordFieldPlaceholder(LanguageManager.getLanguage(c))

    fun searchingRoomStatus(lang: AppLanguage) = t(lang, "Поиск комнаты в эфире...", "Пошук кімнати в ефірі...", "Searching room in air...")
    fun searchingRoomStatus(c: Context) = searchingRoomStatus(LanguageManager.getLanguage(c))

    fun roomFoundBanner(lang: AppLanguage, name: String) = t(lang, "Комната «$name» найдена!", "Кімната «$name» знайдена!", "Room «$name» found!")
    fun roomFoundBanner(c: Context, name: String) = roomFoundBanner(LanguageManager.getLanguage(c), name)

    fun roomCreatorLabel(lang: AppLanguage, owner: String) = t(lang, "Создатель: $owner", "Творець: $owner", "Creator: $owner")
    fun roomCreatorLabel(c: Context, owner: String) = roomCreatorLabel(LanguageManager.getLanguage(c), owner)

    fun creatorNoteTitle(lang: AppLanguage) = t(lang, "Записка от создателя комнаты:", "Записка від творця кімнати:", "Note from room creator:")
    fun creatorNoteTitle(c: Context) = creatorNoteTitle(LanguageManager.getLanguage(c))

    fun roomAbandonedWarning(lang: AppLanguage) = t(
        lang,
        "⚠️ В комнате сейчас никого нет (создатель вышел). Доступ открыт по общему паролю.",
        "⚠️ У кімнаті зараз нікого немає (творець вийшов). Доступ відкритий за загальним паролем.",
        "⚠️ Room is empty (creator left). Access open with common password."
    )
    fun roomAbandonedWarning(c: Context) = roomAbandonedWarning(LanguageManager.getLanguage(c))

    fun enterAirBtn(lang: AppLanguage) = t(lang, "📻 ВОЙТИ В ЭФИР", "📻 УВІЙТИ В ЕФІР", "📻 ENTER AIR")
    fun enterAirBtn(c: Context) = enterAirBtn(LanguageManager.getLanguage(c))

    fun createRoomPrompt(lang: AppLanguage) = t(lang, "Создайте новую закрытую комнату и станьте её владельцем:", "Створіть нову закриту кімнату та станьте її власником:", "Create a new closed room and become its owner:")
    fun createRoomPrompt(c: Context) = createRoomPrompt(LanguageManager.getLanguage(c))

    fun newRoomFieldLabel(lang: AppLanguage) = t(lang, "Название или номер комнаты *", "Назва або номер кімнати *", "Room name or number *")
    fun newRoomFieldLabel(c: Context) = newRoomFieldLabel(LanguageManager.getLanguage(c))
    fun newRoomFieldPlaceholder(lang: AppLanguage) = t(lang, "102, Дозор-1...", "102, Дозор-1...", "102, Sector-1...")
    fun newRoomFieldPlaceholder(c: Context) = newRoomFieldPlaceholder(LanguageManager.getLanguage(c))

    fun randomNumberBtn(lang: AppLanguage) = t(lang, "🎲 Номер", "🎲 Номер", "🎲 Random")
    fun randomNumberBtn(c: Context) = randomNumberBtn(LanguageManager.getLanguage(c))

    fun newPasswordFieldLabel(lang: AppLanguage) = t(lang, "Секретный пароль для комнаты *", "Секретний пароль для кімнати *", "Secret password for room *")
    fun newPasswordFieldLabel(c: Context) = newPasswordFieldLabel(LanguageManager.getLanguage(c))
    fun newPasswordFieldPlaceholder(lang: AppLanguage) = t(lang, "Придумайте пароль", "Придумайте пароль", "Create password")
    fun newPasswordFieldPlaceholder(c: Context) = newPasswordFieldPlaceholder(LanguageManager.getLanguage(c))

    fun yourExistingRoomNote(lang: AppLanguage) = t(
        lang,
        "Это ваша созданная ранее комната. При вводе правильного пароля её настройки будут обновлены, и вы войдете как владелец.",
        "Це ваша створена раніше кімната. При введенні правильного пароля її налаштування будуть оновлені, і ви увійдете як власник.",
        "This is your previously created room. With correct password, settings will be updated and you will enter as owner."
    )
    fun yourExistingRoomNote(c: Context) = yourExistingRoomNote(LanguageManager.getLanguage(c))

    fun roomRegisteredNote(lang: AppLanguage, name: String) = t(
        lang,
        "Комната «$name» уже зарегистрирована в сети. Если это ваша комната, введите её пароль для восстановления управления.",
        "Кімната «$name» вже зареєстрована в мережі. Якщо це ваша кімната, введіть її пароль для відновлення керування.",
        "Room «$name» is already registered. If this is your room, enter password to restore ownership."
    )
    fun roomRegisteredNote(c: Context, name: String) = roomRegisteredNote(LanguageManager.getLanguage(c), name)

    fun ephemeralTitle(lang: AppLanguage) = t(lang, "⏳ Временные сообщения (Автоудаление 24ч)", "⏳ Тимчасові повідомлення (Автовидалення 24г)", "⏳ Disappearing Messages (Auto-delete 24h)")
    fun ephemeralTitle(c: Context) = ephemeralTitle(LanguageManager.getLanguage(c))
    fun ephemeralDesc(lang: AppLanguage) = t(
        lang,
        "Все сообщения и метки удаляются через 24 часа для безопасности.",
        "Всі повідомлення та мітки видаляються через 24 години для безпеки.",
        "All messages and markers are deleted after 24 hours for security."
    )
    fun ephemeralDesc(c: Context) = ephemeralDesc(LanguageManager.getLanguage(c))

    fun telegramBridgeTitle(lang: AppLanguage) = t(lang, "✈️ Трансляция в Telegram (Закрытый канал)", "✈️ Трансляція в Telegram (Закритий канал)", "✈️ Telegram Bridge (Closed Channel)")
    fun telegramBridgeTitle(c: Context) = telegramBridgeTitle(LanguageManager.getLanguage(c))
    fun telegramBridgeEnabledDesc(lang: AppLanguage) = t(lang, "Включено: транслировать в Telegram", "Увімкнено: транслювати в Telegram", "Enabled: broadcast to Telegram")
    fun telegramBridgeEnabledDesc(c: Context) = telegramBridgeEnabledDesc(LanguageManager.getLanguage(c))
    fun telegramBridgeDisabledDesc(lang: AppLanguage) = t(lang, "Dark Room: общение абсолютно невидимо снаружи (только в Varta GO)", "Dark Room: спілкування абсолютно невидиме зовні (тільки у Varta GO)", "Dark Room: communication invisible outside (only in Varta GO)")
    fun telegramBridgeDisabledDesc(c: Context) = telegramBridgeDisabledDesc(LanguageManager.getLanguage(c))

    fun createAndEnterBtn(lang: AppLanguage) = t(lang, "🛡️ СОЗДАТЬ И ВОЙТИ В ЭФИР", "🛡️ СТВОРИТИ ТА УВІЙТИ В ЕФІР", "🛡️ CREATE & ENTER AIR")
    fun createAndEnterBtn(c: Context) = createAndEnterBtn(LanguageManager.getLanguage(c))

    fun telegramSectionTitle(lang: AppLanguage) = t(lang, "Подключение аккаунта Telegram (Опционально)", "Підключення акаунта Telegram (Опціонально)", "Connect Telegram Account (Optional)")
    fun telegramSectionTitle(c: Context) = telegramSectionTitle(LanguageManager.getLanguage(c))

    fun telegramAuthorizedBadge(lang: AppLanguage) = t(lang, "✅ Авторизован в Telegram", "✅ Авторизовано в Telegram", "✅ Authorized in Telegram")
    fun telegramAuthorizedBadge(c: Context) = telegramAuthorizedBadge(LanguageManager.getLanguage(c))
    fun telegramNotRequiredBadge(lang: AppLanguage) = t(lang, "Не требуется для автономного радиоэфира", "Не потрібно для автономного радіоефіру", "Not required for standalone radio")
    fun telegramNotRequiredBadge(c: Context) = telegramNotRequiredBadge(LanguageManager.getLanguage(c))

    fun telegramSessionActive(lang: AppLanguage, u: String) = t(lang, "Сессия активна: @$u", "Сесія активна: @$u", "Session active: @$u")
    fun telegramSessionActive(c: Context, u: String) = telegramSessionActive(LanguageManager.getLanguage(c), u)
    fun telegramLogoutBtn(lang: AppLanguage) = t(lang, "Выйти", "Вийти", "Log Out")
    fun telegramLogoutBtn(c: Context) = telegramLogoutBtn(LanguageManager.getLanguage(c))

    fun telegramPhonePrompt(lang: AppLanguage) = t(
        lang,
        "Если вы хотите управлять каналами через Telegram с ПК, укажите номер телефона:",
        "Якщо ви хочете керувати каналами через Telegram з ПК, вкажіть номер телефону:",
        "To manage channels via Telegram on PC, enter your phone number:"
    )
    fun telegramPhonePrompt(c: Context) = telegramPhonePrompt(LanguageManager.getLanguage(c))

    fun telegramPhoneLabel(lang: AppLanguage) = t(lang, "Номер телефона (0... или +380...)", "Номер телефону (0... або +380...)", "Phone number (0... or +380...)")
    fun telegramPhoneLabel(c: Context) = telegramPhoneLabel(LanguageManager.getLanguage(c))

    fun telegramSendCodeBtn(lang: AppLanguage) = t(lang, "📩 Получить код в Telegram", "📩 Отримати код у Telegram", "📩 Get Telegram Code")
    fun telegramSendCodeBtn(c: Context) = telegramSendCodeBtn(LanguageManager.getLanguage(c))

    fun telegramCodePrompt(lang: AppLanguage) = t(
        lang,
        "Введите 5-значный код, пришедший в приложение Telegram:",
        "Введіть 5-значний код, що надійшов у застосунок Telegram:",
        "Enter 5-digit code sent to your Telegram app:"
    )
    fun telegramCodePrompt(c: Context) = telegramCodePrompt(LanguageManager.getLanguage(c))

    fun telegram2faPrompt(lang: AppLanguage) = t(
        lang,
        "На аккаунте включена двухфакторная аутентификация (2FA). Введите ваш облачный пароль Telegram:",
        "На акаунті увімкнена хмарна 2FA. Введіть ваш хмарний пароль Telegram:",
        "Two-factor authentication (2FA) is enabled. Enter your Telegram cloud password:"
    )
    fun telegram2faPrompt(c: Context) = telegram2faPrompt(LanguageManager.getLanguage(c))

    fun codeLabel(lang: AppLanguage) = t(lang, "Код из Telegram", "Код з Telegram", "Telegram Code")
    fun codeLabel(c: Context) = codeLabel(LanguageManager.getLanguage(c))

    fun password2faLabel(lang: AppLanguage) = t(lang, "Облачный пароль 2FA", "Хмарний пароль 2FA", "2FA Cloud Password")
    fun password2faLabel(c: Context) = password2faLabel(LanguageManager.getLanguage(c))

    fun logOutTelegram(lang: AppLanguage) = t(lang, "Выйти из Telegram", "Вийти з Telegram", "Log out of Telegram")
    fun logOutTelegram(c: Context) = logOutTelegram(LanguageManager.getLanguage(c))

    fun roomLabel(lang: AppLanguage) = t(lang, "Код комнаты", "Код кімнати", "Room ID")
    fun roomLabel(c: Context) = roomLabel(LanguageManager.getLanguage(c))

    fun telegramConfirmBtn(lang: AppLanguage) = t(lang, "Войти", "Увійти", "Sign In")
    fun telegramConfirmBtn(c: Context) = telegramConfirmBtn(LanguageManager.getLanguage(c))

    fun callSignRequiredToast(lang: AppLanguage) = t(lang, "Укажите ваш позывной", "Вкажіть ваш позивний", "Enter your callsign")
    fun callSignRequiredToast(c: Context) = callSignRequiredToast(LanguageManager.getLanguage(c))

    fun roomNameRequiredToast(lang: AppLanguage) = t(lang, "Укажите номер или название комнаты", "Вкажіть номер або назву кімнати", "Enter room name or number")
    fun roomNameRequiredToast(c: Context) = roomNameRequiredToast(LanguageManager.getLanguage(c))

    fun passwordRequiredToast(lang: AppLanguage) = t(lang, "Введите пароль комнаты", "Введіть пароль кімнати", "Enter room password")
    fun passwordRequiredToast(c: Context) = passwordRequiredToast(LanguageManager.getLanguage(c))

    // Audio Modes
    fun audioModeSound(lang: AppLanguage) = t(lang, "Рация", "Рація", "Radio")
    fun audioModeSound(c: Context) = audioModeSound(LanguageManager.getLanguage(c))
    fun audioModeVibrate(lang: AppLanguage) = t(lang, "Вибро", "Вібро", "Vibrate")
    fun audioModeVibrate(c: Context) = audioModeVibrate(LanguageManager.getLanguage(c))
    fun audioModeSilent(lang: AppLanguage) = t(lang, "Без звука", "Без звуку", "Silent")
    fun audioModeSilent(c: Context) = audioModeSilent(LanguageManager.getLanguage(c))

    fun volumeBoostTitle(lang: AppLanguage) = t(lang, "🔊 Принудительное усиление звука (+12 dB)", "🔊 Примусове підсилення звуку (+12 dB)", "🔊 Forced Volume Boost (+12 dB)")
    fun volumeBoostTitle(c: Context) = volumeBoostTitle(LanguageManager.getLanguage(c))
    fun volumeBoostDesc(lang: AppLanguage) = t(
        lang,
        "Аппаратный турбо-буст динамика рации сверх лимита громкости телефона (Loudness Enhancer)",
        "Апаратний турбо-буст динаміка рації понад ліміт гучності телефону (Loudness Enhancer)",
        "Hardware turbo-boost for walkie-talkie speakerphone beyond system limit (Loudness Enhancer)"
    )
    fun volumeBoostDesc(c: Context) = volumeBoostDesc(LanguageManager.getLanguage(c))

    fun hazardNotificationChannel(lang: AppLanguage) = t(lang, "Varta GO: Экстренные метки", "Varta GO: Екстрені мітки", "Varta GO: Hazard Alerts")
    fun hazardNotificationChannel(c: Context) = hazardNotificationChannel(LanguageManager.getLanguage(c))
    fun voiceNotificationChannel(lang: AppLanguage) = t(lang, "Varta GO: Рация и сообщения", "Varta GO: Рація та повідомлення", "Varta GO: Radio & Voice")
    fun voiceNotificationChannel(c: Context) = voiceNotificationChannel(LanguageManager.getLanguage(c))
    fun playVoiceAction(lang: AppLanguage) = t(lang, "▶ Прослушать", "▶ Прослухати", "▶ Listen")
    fun playVoiceAction(c: Context) = playVoiceAction(LanguageManager.getLanguage(c))

    // Alert Expiry & Threat Clear
    fun alertDurationTitle(lang: AppLanguage) = t(lang, "Срок действия метки:", "Термін дії мітки:", "Alert Duration:")
    fun alertDurationTitle(c: Context) = alertDurationTitle(LanguageManager.getLanguage(c))

    fun alertDurationMinutes(lang: AppLanguage, m: Int) = t(lang, "$m мин", "$m хв", "$m min")
    fun alertDurationMinutes(c: Context, m: Int) = alertDurationMinutes(LanguageManager.getLanguage(c), m)

    fun alertDurationHours(lang: AppLanguage, h: Int) = t(lang, "$h ч", "$h год", "$h hr")
    fun alertDurationHours(c: Context, h: Int) = alertDurationHours(LanguageManager.getLanguage(c), h)

    fun clearThreatBtn(lang: AppLanguage) = t(lang, "✅ Угрозы нет (Отбой)", "✅ Загрози немає (Відбій)", "✅ No Threat (Clear)")
    fun clearThreatBtn(c: Context) = clearThreatBtn(LanguageManager.getLanguage(c))

    fun threatClearedCount(lang: AppLanguage, cur: Int, needed: Int) = t(lang, "Подтвердили: $cur/$needed", "Підтвердили: $cur/$needed", "Confirmed: $cur/$needed")
    fun threatClearedCount(c: Context, cur: Int, needed: Int) = threatClearedCount(LanguageManager.getLanguage(c), cur, needed)

    fun threatResolvedToast(lang: AppLanguage) = t(lang, "Метка снята: угроза не подтвердилась", "Мітку знято: загроза не підтвердилася", "Alert cleared: threat resolved")
    fun threatResolvedToast(c: Context) = threatResolvedToast(LanguageManager.getLanguage(c))

    fun alertClusterTitle(lang: AppLanguage, count: Int) = t(lang, "Группа меток ($count)", "Група міток ($count)", "Alert Cluster ($count)")
    fun alertClusterTitle(c: Context, count: Int) = alertClusterTitle(LanguageManager.getLanguage(c), count)

    // --- App Updates ---
    fun updatesSection(lang: AppLanguage) = t(lang, "📦 Обновления приложения", "📦 Оновлення застосунку", "📦 App Updates")
    fun updatesSection(c: Context) = updatesSection(LanguageManager.getLanguage(c))

    fun currentVersionLabel(lang: AppLanguage, v: String) = t(lang, "Версия: v$v", "Версія: v$v", "Version: v$v")
    fun currentVersionLabel(c: Context, v: String) = currentVersionLabel(LanguageManager.getLanguage(c), v)

    fun checkForUpdatesBtn(lang: AppLanguage) = t(lang, "🔄 Проверить обновления", "🔄 Перевірити оновлення", "🔄 Check for Updates")
    fun checkForUpdatesBtn(c: Context) = checkForUpdatesBtn(LanguageManager.getLanguage(c))

    fun reinstallUpdateBtn(lang: AppLanguage) = t(lang, "Принудительно переустановить", "Примусово перевстановити", "Reinstall latest from GitHub")
    fun reinstallUpdateBtn(c: Context) = reinstallUpdateBtn(LanguageManager.getLanguage(c))

    fun checkingUpdatesStatus(lang: AppLanguage) = t(lang, "Проверка релизов на GitHub...", "Перевірка релізів на GitHub...", "Checking GitHub releases...")
    fun checkingUpdatesStatus(c: Context) = checkingUpdatesStatus(LanguageManager.getLanguage(c))

    fun updateAvailableStatus(lang: AppLanguage, v: String) = t(lang, "🎉 Доступна новая версия: $v!", "🎉 Доступна нова версія: $v!", "🎉 New version available: $v!")
    fun updateAvailableStatus(c: Context, v: String) = updateAvailableStatus(LanguageManager.getLanguage(c), v)

    fun upToDateStatus(lang: AppLanguage, v: String) = t(lang, "✅ Установлена актуальная версия (v$v)", "✅ Встановлена актуальна версія (v$v)", "✅ Up to date (v$v)")
    fun upToDateStatus(c: Context, v: String) = upToDateStatus(LanguageManager.getLanguage(c), v)

    fun downloadingUpdateStatus(lang: AppLanguage, p: Int) = t(lang, "⬇️ Загрузка обновления: $p%", "⬇️ Завантаження оновлення: $p%", "⬇️ Downloading: $p%")
    fun downloadingUpdateStatus(c: Context, p: Int) = downloadingUpdateStatus(LanguageManager.getLanguage(c), p)

    fun readyToInstallStatus(lang: AppLanguage) = t(lang, "📦 Запуск установки APK...", "📦 Запуск встановлення APK...", "📦 Launching APK installer...")
    fun readyToInstallStatus(c: Context) = readyToInstallStatus(LanguageManager.getLanguage(c))

    fun installUpdateBtn(lang: AppLanguage) = t(lang, "⚡ Обновить сейчас", "⚡ Оновити зараз", "⚡ Update Now")
    fun installUpdateBtn(c: Context) = installUpdateBtn(LanguageManager.getLanguage(c))

    fun updateModalTitle(lang: AppLanguage, v: String) = t(lang, "⚡ Доступно обновление: $v", "⚡ Доступне оновлення: $v", "⚡ App Update Available: $v")
    fun updateModalTitle(c: Context, v: String) = updateModalTitle(LanguageManager.getLanguage(c), v)

    fun updateModalDesc(lang: AppLanguage) = t(
        lang,
        "Вышла новая версия приложения Varta GO.\nОбновитесь для стабильной связи и актуальной защиты.",
        "Вийшла нова версія застосунку Varta GO.\nОновіться для стабільного зв'язку та актуального захисту.",
        "A new version of Varta GO is available.\nPlease update for stable radio connectivity and enhanced security."
    )
    fun updateModalDesc(c: Context) = updateModalDesc(LanguageManager.getLanguage(c))

    fun whatsNew(lang: AppLanguage) = t(lang, "Что нового:", "Що нового:", "What's new:")
    fun whatsNew(c: Context) = whatsNew(LanguageManager.getLanguage(c))

    fun downloadingUpdateTitle(lang: AppLanguage) = t(lang, "⬇️ Загрузка обновления...", "⬇️ Завантаження оновлення...", "⬇️ Downloading update...")
    fun downloadingUpdateTitle(c: Context) = downloadingUpdateTitle(LanguageManager.getLanguage(c))

    fun downloadProgressText(lang: AppLanguage, p: Int) = t(lang, "Загрузка новой версии: $p%", "Завантаження нової версії: $p%", "Downloading new version: $p%")
    fun downloadProgressText(c: Context, p: Int) = downloadProgressText(LanguageManager.getLanguage(c), p)

    fun downloadWaitText(lang: AppLanguage) = t(
        lang,
        "Пожалуйста, подождите завершения загрузки. Установка начнется автоматически.",
        "Будь ласка, зачекайте завершення завантаження. Встановлення почнеться автоматично.",
        "Please wait for download to finish. Installation will start automatically."
    )
    fun downloadWaitText(c: Context) = downloadWaitText(LanguageManager.getLanguage(c))

    fun updateDownloadedTitle(lang: AppLanguage) = t(lang, "📦 Обновление загружено", "📦 Оновлення завантажено", "📦 Update Downloaded")
    fun updateDownloadedTitle(c: Context) = updateDownloadedTitle(LanguageManager.getLanguage(c))

    fun updateDownloadedDesc(lang: AppLanguage) = t(lang, "Файл обновления успешно загружен на устройство.", "Файл оновлення успішно завантажено на пристрій.", "Update file successfully downloaded.")
    fun updateDownloadedDesc(c: Context) = updateDownloadedDesc(LanguageManager.getLanguage(c))

    fun installNowPrompt(lang: AppLanguage) = t(lang, "Нажмите «Установить сейчас» для применения новой версии.", "Натисніть «Встановити зараз» для застосування нової версії.", "Tap «Install Now» to apply the new version.")
    fun installNowPrompt(c: Context) = installNowPrompt(LanguageManager.getLanguage(c))

    fun installNowBtn(lang: AppLanguage) = t(lang, "📦 Установить сейчас", "📦 Встановити зараз", "📦 Install Now")
    fun installNowBtn(c: Context) = installNowBtn(LanguageManager.getLanguage(c))

    fun updateErrorTitle(lang: AppLanguage) = t(lang, "⚠️ Ошибка обновления", "⚠️ Помилка оновлення", "⚠️ Update Error")
    fun updateErrorTitle(c: Context) = updateErrorTitle(LanguageManager.getLanguage(c))

    fun gotItBtn(lang: AppLanguage) = t(lang, "Понятно", "Зрозуміло", "Got it")
    fun gotItBtn(c: Context) = gotItBtn(LanguageManager.getLanguage(c))

    fun updateAvailableToast(lang: AppLanguage) = t(lang, "Найдено обновление! Нажмите «Обновить»", "Знайдено оновлення! Натисніть «Оновити»", "Update found! Tap to update")
    fun updateAvailableToast(c: Context) = updateAvailableToast(LanguageManager.getLanguage(c))

    fun downloadingApk(lang: AppLanguage) = t(lang, "Скачивание обновления...", "Завантаження оновлення...", "Downloading update...")
    fun downloadingApk(c: Context) = downloadingApk(LanguageManager.getLanguage(c))

    // --- Room Entry History ---
    fun historyRoomsTitle(c: Context) = t(c, "📜 Недавние комнаты", "📜 Нещодавні кімнати", "📜 Recent Rooms")
    fun historyRoomsSubtitle(c: Context) = t(
        c,
        "Быстрый вход в 1 клик и ваши права",
        "Швидкий вхід в 1 клік та ваші права",
        "Quick 1-tap entry and your roles"
    )
    fun historyFilterAll(c: Context, count: Int) = t(c, "Все ($count)", "Всі ($count)", "All ($count)")
    fun historyFilterAdmin(c: Context, count: Int) = t(c, "👑 Мои / Админ ($count)", "👑 Мої / Адмін ($count)", "👑 Mine / Admin ($count)")
    fun historyEmpty(c: Context) = t(c, "История входа пуста", "Історія входів порожня", "No room history yet")
    fun historyClearBtn(c: Context) = t(c, "Очистить историю", "Очистити історію", "Clear History")
    fun historyRoleOwner(c: Context) = t(c, "👑 Создатель", "👑 Творець", "👑 Creator")
    fun historyRoleAdmin(c: Context) = t(c, "🛡️ Админ", "🛡️ Адмін", "🛡️ Admin")
    fun historyRoleMember(c: Context) = t(c, "👤 Участник", "👤 Учасник", "👤 Member")
    fun historyQuickEnter(c: Context) = t(c, "Войти", "Увійти", "Enter")

    // --- Main Hub Screen (Waze Tactical) ---
    fun membersOnlineCount(lang: AppLanguage, count: Int) = t(lang, "$count в сети", "$count у мережі", "$count online")
    fun membersOnlineCount(c: Context, count: Int) = membersOnlineCount(LanguageManager.getLanguage(c), count)
    fun silentModeBadge(lang: AppLanguage) = t(lang, "🔇 Без звука", "🔇 Без звуку", "🔇 Silent")
    fun silentModeBadge(c: Context) = silentModeBadge(LanguageManager.getLanguage(c))
    fun vibrateModeBadge(lang: AppLanguage) = t(lang, "📳 Вибро", "📳 Вібро", "📳 Vibrate")
    fun vibrateModeBadge(c: Context) = vibrateModeBadge(LanguageManager.getLanguage(c))
    fun loudspeakerBadge(lang: AppLanguage) = t(lang, "🔊 Звук", "🔊 Звук", "🔊 Sound")
    fun loudspeakerBadge(c: Context) = loudspeakerBadge(LanguageManager.getLanguage(c))
    fun switchRoomTooltip(lang: AppLanguage) = t(lang, "Сменить комнату", "Змінити кімнату", "Switch Room")
    fun switchRoomTooltip(c: Context) = switchRoomTooltip(LanguageManager.getLanguage(c))
    fun voiceHistoryTooltip(lang: AppLanguage) = t(lang, "История рации", "Історія рації", "Radio History")
    fun voiceHistoryTooltip(c: Context) = voiceHistoryTooltip(LanguageManager.getLanguage(c))
    fun membersTooltip(lang: AppLanguage) = t(lang, "Участники", "Учасники", "Members")
    fun membersTooltip(c: Context) = membersTooltip(LanguageManager.getLanguage(c))
    fun settingsTooltip(lang: AppLanguage) = t(lang, "Настройки", "Налаштування", "Settings")
    fun settingsTooltip(c: Context) = settingsTooltip(LanguageManager.getLanguage(c))
    fun channelClear(lang: AppLanguage) = t(lang, "Радиоэфир свободен", "Радіоефір вільний", "Channel is clear")
    fun channelClear(c: Context) = channelClear(LanguageManager.getLanguage(c))
    fun youAreTransmitting(lang: AppLanguage) = t(lang, "ВЫ В ЭФИРЕ (Передача)", "ВИ В ЕФІРІ (Передача)", "TRANSMITTING...")
    fun youAreTransmitting(c: Context) = youAreTransmitting(LanguageManager.getLanguage(c))
    fun activeSpeakerVoice(lang: AppLanguage, speaker: String) = t(
        lang,
        "ГОЛОС В ЭФИРЕ: $speaker",
        "ГОЛОС В ЕФІРІ: $speaker",
        "VOICE IN AIR: $speaker"
    )
    fun activeSpeakerVoice(c: Context, speaker: String) = activeSpeakerVoice(LanguageManager.getLanguage(c), speaker)
    fun activeSpeakerBadge(lang: AppLanguage) = t(lang, "🔊 АКТИВНО", "🔊 АКТИВНО", "🔊 ACTIVE")
    fun activeSpeakerBadge(c: Context) = activeSpeakerBadge(LanguageManager.getLanguage(c))
    fun holdToTalk(lang: AppLanguage) = t(lang, "УДЕРЖИВАЙТЕ ДЛЯ СВЯЗИ", "УТРИМУЙТЕ ДЛЯ ЗВ'ЯЗКУ", "HOLD TO TALK")
    fun holdToTalk(c: Context) = holdToTalk(LanguageManager.getLanguage(c))
    fun dangerBtn(lang: AppLanguage) = t(lang, "МЕТКА", "МІТКА", "DANGER")
    fun dangerBtn(c: Context) = dangerBtn(LanguageManager.getLanguage(c))
    fun photoBtn(lang: AppLanguage) = t(lang, "ФОТО", "ФОТО", "PHOTO")
    fun photoBtn(c: Context) = photoBtn(LanguageManager.getLanguage(c))
    fun myLocationTooltip(lang: AppLanguage) = t(lang, "Моё местоположение", "Моє місцезнаходження", "My Location")
    fun myLocationTooltip(c: Context) = myLocationTooltip(LanguageManager.getLanguage(c))

    fun switchRoomConfirmTitle(lang: AppLanguage) = t(lang, "🚪 Сменить комнату?", "🚪 Змінити кімнату?", "🚪 Switch Room?")
    fun switchRoomConfirmTitle(c: Context) = switchRoomConfirmTitle(LanguageManager.getLanguage(c))
    fun switchRoomConfirmDesc(lang: AppLanguage, roomName: String) = t(
        lang,
        "Вы выйдете из текущего закрытого радиоканала «$roomName» и сможете выбрать другую группу или создать новый канал.\n\nВаша авторизация сохранится.",
        "Ви вийдете з поточного закритого радіоканалу «$roomName» і зможете обрати іншу групу або створити новий канал.\n\nВаша авторизація збережеться.",
        "You will disconnect from current channel «$roomName» and can switch to another room or create a new one.\n\nYour session remains saved."
    )
    fun switchRoomConfirmDesc(c: Context, roomName: String) = switchRoomConfirmDesc(LanguageManager.getLanguage(c), roomName)
    fun switchRoomConfirmAction(lang: AppLanguage) = t(lang, "Сменить комнату", "Змінити кімнату", "Switch Room")
    fun switchRoomConfirmAction(c: Context) = switchRoomConfirmAction(LanguageManager.getLanguage(c))

    // --- Hazard & Danger Report Dialogs ---
    fun placeDangerTitle(lang: AppLanguage) = t(lang, "📍 ПОСТАВИТЬ МЕТКУ", "📍 ПОСТАВИТИ МІТКУ", "📍 REPORT HAZARD")
    fun placeDangerTitle(c: Context) = placeDangerTitle(LanguageManager.getLanguage(c))
    fun chooseDangerDesc(lang: AppLanguage) = t(
        lang,
        "Выберите тип метки для оповещения всех участников группы:",
        "Оберіть тип мітки для сповіщення всіх учасників групи:",
        "Select hazard type to alert everyone in the room:"
    )
    fun chooseDangerDesc(c: Context) = chooseDangerDesc(LanguageManager.getLanguage(c))
    fun dangerDetailsHint(lang: AppLanguage) = t(
        lang,
        "Уточнение (напр. ул. Ленина, 5)",
        "Уточнення (напр. вул. Лесі Українки, 5)",
        "Details (e.g. cross street or building)"
    )
    fun dangerDetailsHint(c: Context) = dangerDetailsHint(LanguageManager.getLanguage(c))
    fun holdToBroadcastPrompt(lang: AppLanguage) = t(
        lang,
        "УДЕРЖИВАЙТЕ 2 сек ДЛЯ ОТПРАВКИ МЕТКИ",
        "УТРИМУЙТЕ 2 сек ДЛЯ ВІДПРАВКИ МІТКИ",
        "HOLD 2s TO BROADCAST HAZARD"
    )
    fun holdToBroadcastPrompt(c: Context) = holdToBroadcastPrompt(LanguageManager.getLanguage(c))
    fun holdingProgress(lang: AppLanguage, percent: Int) = t(
        lang,
        "УДЕРЖИВАЙТЕ ($percent%)",
        "УТРИМУЙТЕ ($percent%)",
        "HOLDING ($percent%)"
    )
    fun holdingProgress(c: Context, percent: Int) = holdingProgress(LanguageManager.getLanguage(c), percent)

    // Alert Durations
    fun alertDurationLabel(lang: AppLanguage) = t(lang, "Время действия метки:", "Час дії мітки:", "Alert Duration:")
    fun alertDurationLabel(c: Context) = alertDurationLabel(LanguageManager.getLanguage(c))
    fun duration15m(lang: AppLanguage) = t(lang, "15 мин", "15 хв", "15m")
    fun duration15m(c: Context) = duration15m(LanguageManager.getLanguage(c))
    fun duration30m(lang: AppLanguage) = t(lang, "30 мин", "30 хв", "30m")
    fun duration30m(c: Context) = duration30m(LanguageManager.getLanguage(c))
    fun duration1h(lang: AppLanguage) = t(lang, "1 час", "1 год", "1h")
    fun duration1h(c: Context) = duration1h(LanguageManager.getLanguage(c))
    fun duration2h(lang: AppLanguage) = t(lang, "2 часа", "2 год", "2h")
    fun duration2h(c: Context) = duration2h(LanguageManager.getLanguage(c))
    fun duration4h(lang: AppLanguage) = t(lang, "4 часа", "4 год", "4h")
    fun duration4h(c: Context) = duration4h(LanguageManager.getLanguage(c))
    fun defaultBadge(lang: AppLanguage) = t(lang, "(по умолч.)", "(за замовч.)", "(default)")
    fun defaultBadge(c: Context) = defaultBadge(LanguageManager.getLanguage(c))

    fun photoFixationTitle(lang: AppLanguage) = t(lang, "📷 ФОТОФИКСАЦИЯ ОПАСНОСТИ", "📷 ФОТОФІКСАЦІЯ НЕБЕЗПЕКИ", "📷 HAZARD PHOTO REPORT")
    fun photoFixationTitle(c: Context) = photoFixationTitle(LanguageManager.getLanguage(c))
    fun tapToTakePhoto(lang: AppLanguage) = t(lang, "Нажмите, чтобы сделать фото", "Натисніть, щоб зробити фото", "Tap to take a photo")
    fun tapToTakePhoto(c: Context) = tapToTakePhoto(LanguageManager.getLanguage(c))
    fun photoHazardTypeLabel(lang: AppLanguage) = t(lang, "Тип опасности:", "Тип небезпеки:", "Hazard type:")
    fun photoHazardTypeLabel(c: Context) = photoHazardTypeLabel(LanguageManager.getLanguage(c))
    fun photoDetailsHint(lang: AppLanguage) = t(
        lang,
        "Описание (напр. беспилотник в небе)",
        "Опис (напр. безпілотник у небі)",
        "Description (e.g. drone spotted)"
    )
    fun photoDetailsHint(c: Context) = photoDetailsHint(LanguageManager.getLanguage(c))
    fun holdToSendPhotoPrompt(lang: AppLanguage) = t(
        lang,
        "УДЕРЖИВАЙТЕ ДЛЯ ОТПРАВКИ",
        "УТРИМУЙТЕ ДЛЯ ВІДПРАВКИ",
        "HOLD TO SEND PHOTO"
    )
    fun holdToSendPhotoPrompt(c: Context) = holdToSendPhotoPrompt(LanguageManager.getLanguage(c))

    // --- Alert Detail Dialog ---
    fun distanceLabel(lang: AppLanguage) = t(lang, "Расстояние", "Відстань", "Distance")
    fun distanceLabel(c: Context) = distanceLabel(LanguageManager.getLanguage(c))
    fun reportedByLabel(lang: AppLanguage) = t(lang, "Сообщил", "Повідомив", "Reported by")
    fun reportedByLabel(c: Context) = reportedByLabel(LanguageManager.getLanguage(c))
    fun descriptionLabel(lang: AppLanguage) = t(lang, "Описание:", "Опис:", "Description:")
    fun descriptionLabel(c: Context) = descriptionLabel(LanguageManager.getLanguage(c))
    fun resolveAlertBtn(lang: AppLanguage) = t(
        lang,
        "Угрозы нет (Отбой)",
        "Загрози немає (Відбій)",
        "Threat Cleared"
    )
    fun resolveAlertBtn(c: Context) = resolveAlertBtn(LanguageManager.getLanguage(c))
    fun threatClearedVotes(lang: AppLanguage, current: Int, needed: Int) = t(
        lang,
        "Угрозы нет: $current/$needed подтвержд.",
        "Загрози немає: $current/$needed підтвердж.",
        "Threat clear: $current/$needed confirmed"
    )
    fun threatClearedVotes(c: Context, current: Int, needed: Int) = threatClearedVotes(LanguageManager.getLanguage(c), current, needed)
    fun alreadyVotedDismiss(lang: AppLanguage) = t(
        lang,
        "Вы уже подтвердили отсутствие угрозы. Ожидается подтверждение другого участника.",
        "Ви вже підтвердили відсутність загрози. Очікується підтвердження іншого учасника.",
        "You already confirmed all-clear. Awaiting confirmation from another member."
    )
    fun alreadyVotedDismiss(c: Context) = alreadyVotedDismiss(LanguageManager.getLanguage(c))

    // --- Voice History Dialog ---
    fun voiceHistoryTitle(lang: AppLanguage) = t(lang, "🎙 ИСТОРИЯ РАЦИИ", "🎙 ІСТОРІЯ РАЦІЇ", "🎙 RADIO HISTORY")
    fun voiceHistoryTitle(c: Context) = voiceHistoryTitle(LanguageManager.getLanguage(c))
    fun noVoiceMessages(lang: AppLanguage) = t(
        lang,
        "Голосовых сообщений пока нет",
        "Голосових повідомлень поки немає",
        "No voice messages yet"
    )
    fun noVoiceMessages(c: Context) = noVoiceMessages(LanguageManager.getLanguage(c))

    // --- Hazard Category Names ---
    fun alertTypeName(lang: AppLanguage, rawValue: String): String = when (rawValue.lowercase()) {
        "shelling" -> t(lang, "Обстрел", "Обстріл", "Shelling")
        "fire" -> t(lang, "Пожар", "Пожежа", "Fire")
        "medical" -> t(lang, "Медпомощь", "Меддопомога", "Medical")
        "police" -> t(lang, "Полиция", "Поліція", "Police")
        "military" -> t(lang, "Военные", "Військові", "Military")
        else -> t(lang, "Другое", "Інше", "Other")
    }
    fun alertTypeName(c: Context, rawValue: String): String = alertTypeName(LanguageManager.getLanguage(c), rawValue)

    // --- Settings Dialog ---
    fun settingsTitle(lang: AppLanguage) = t(lang, "Настройки комнаты", "Налаштування кімнати", "Room Settings")
    fun settingsTitle(c: Context) = settingsTitle(LanguageManager.getLanguage(c))
    fun languageSection(lang: AppLanguage) = t(lang, "Язык приложения", "Мова застосунку", "App Language")
    fun languageSection(c: Context) = languageSection(LanguageManager.getLanguage(c))
    fun audioSection(lang: AppLanguage) = t(lang, "Аудио и оповещения", "Аудіо та сповіщення", "Audio & Alerts")
    fun audioSection(c: Context) = audioSection(LanguageManager.getLanguage(c))
    fun muteSpeakerTitle(lang: AppLanguage) = t(lang, "Режим оповещения", "Режим сповіщення", "Notification Mode")
    fun muteSpeakerTitle(c: Context) = muteSpeakerTitle(LanguageManager.getLanguage(c))
    fun muteSpeakerDesc(lang: AppLanguage) = t(
        lang,
        "Переключение между громкой связью, вибро и без звука",
        "Перемикання між гучним зв'язком, вібро та без звуку",
        "Toggle between loudspeaker, vibrate and silent"
    )
    fun muteSpeakerDesc(c: Context) = muteSpeakerDesc(LanguageManager.getLanguage(c))
    fun storageSection(lang: AppLanguage) = t(lang, "Безопасность и хранение", "Безпека та зберігання", "Security & Storage")
    fun storageSection(c: Context) = storageSection(LanguageManager.getLanguage(c))
    fun autoDeleteToggle(lang: AppLanguage) = t(lang, "Самоуничтожение сообщений (24ч)", "Самознищення повідомлень (24 год)", "Self-Destruct Messages (24h)")
    fun autoDeleteToggle(c: Context) = autoDeleteToggle(LanguageManager.getLanguage(c))
    fun redirectionSection(lang: AppLanguage) = t(lang, "Сообщение для гостей пустой комнаты", "Повідомлення для гостей порожньої кімнати", "Announcement for room visitors")
    fun redirectionSection(c: Context) = redirectionSection(LanguageManager.getLanguage(c))
    fun redirectionHint(lang: AppLanguage) = t(lang, "Например: Мы перешли в комнату 26", "Наприклад: Ми перейшли в кімнату 26", "e.g. We moved to room 26")
    fun redirectionHint(c: Context) = redirectionHint(LanguageManager.getLanguage(c))
    fun saveRedirectionBtn(lang: AppLanguage) = t(lang, "Сохранить сообщение", "Зберегти повідомлення", "Save Announcement")
    fun saveRedirectionBtn(c: Context) = saveRedirectionBtn(LanguageManager.getLanguage(c))
    fun bannedMembersSection(lang: AppLanguage) = t(lang, "🚫 Заблокированные участники", "🚫 Заблоковані учасники", "🚫 Banned Members")
    fun bannedMembersSection(c: Context) = bannedMembersSection(LanguageManager.getLanguage(c))
    fun unbanBtn(lang: AppLanguage) = t(lang, "Разблокировать", "Розблокувати", "Unban")
    fun unbanBtn(c: Context) = unbanBtn(LanguageManager.getLanguage(c))
    fun noBannedUsers(lang: AppLanguage) = t(lang, "Нет заблокированных участников", "Немає заблокованих учасників", "No banned members")
    fun noBannedUsers(c: Context) = noBannedUsers(LanguageManager.getLanguage(c))
    fun leaveRoomBtn(lang: AppLanguage) = t(lang, "Покинуть комнату", "Вийти з кімнати", "Leave Room")
    fun leaveRoomBtn(c: Context) = leaveRoomBtn(LanguageManager.getLanguage(c))
    fun deleteRoomBtn(lang: AppLanguage) = t(lang, "🗑️ Удалить комнату совсем", "🗑️ Видалити кімнату назавжди", "🗑️ Delete Room Permanently")
    fun deleteRoomBtn(c: Context) = deleteRoomBtn(LanguageManager.getLanguage(c))
    fun deleteRoomConfirmTitle(lang: AppLanguage) = t(lang, "Удаление комнаты", "Видалення кімнати", "Delete Room")
    fun deleteRoomConfirmTitle(c: Context) = deleteRoomConfirmTitle(LanguageManager.getLanguage(c))
    fun deleteRoomConfirmText(lang: AppLanguage) = t(
        lang,
        "Вы действительно хотите навсегда удалить эту комнату? Все участники будут отключены, а комната стёрта из сети.",
        "Ви дійсно хочете назавжди видалити цю кімнату? Всі учасники будуть відключені, а кімната стерта з мережі.",
        "Are you sure you want to permanently delete this room? All members will be disconnected and the room erased from the network."
    )
    fun deleteRoomConfirmText(c: Context) = deleteRoomConfirmText(LanguageManager.getLanguage(c))
    fun deleteRoomConfirmAction(lang: AppLanguage) = t(lang, "Да, удалить комнату", "Так, видалити кімнату", "Yes, delete room")
    fun deleteRoomConfirmAction(c: Context) = deleteRoomConfirmAction(LanguageManager.getLanguage(c))

    // --- Super Admin ---
    fun superAdminSection(lang: AppLanguage) = t(lang, "⚡ Режим суперадмина", "⚡ Режим суперадміна", "⚡ Super Admin Mode")
    fun superAdminSection(c: Context) = superAdminSection(LanguageManager.getLanguage(c))
    fun superAdminStatusActive(lang: AppLanguage) = t(lang, "⚡ СУПЕРАДМИН АКТИВЕН", "⚡ СУПЕРАДМІН АКТИВНИЙ", "⚡ SUPER ADMIN ACTIVE")
    fun superAdminStatusActive(c: Context) = superAdminStatusActive(LanguageManager.getLanguage(c))
    fun superAdminDesc(lang: AppLanguage) = t(
        lang,
        "У вас есть права на удаление любых комнат и управление всей сетью.",
        "У вас є права на видалення будь-яких кімнат та керування всією мережею.",
        "You have privileges to delete any room and manage the entire network."
    )
    fun superAdminDesc(c: Context) = superAdminDesc(LanguageManager.getLanguage(c))
    fun superAdminPrompt(lang: AppLanguage) = t(lang, "Введите мастер-пароль суперадмина для активации:", "Введіть майстер-пароль суперадміна для активації:", "Enter super admin master passcode to activate:")
    fun superAdminPrompt(c: Context) = superAdminPrompt(LanguageManager.getLanguage(c))
    fun superAdminKeyPlaceholder(lang: AppLanguage) = t(lang, "Мастер-пароль суперадмина", "Майстер-пароль суперадміна", "Super Admin Master Passcode")
    fun superAdminKeyPlaceholder(c: Context) = superAdminKeyPlaceholder(LanguageManager.getLanguage(c))
    fun superAdminActivateBtn(lang: AppLanguage) = t(lang, "Активировать", "Активувати", "Activate")
    fun superAdminActivateBtn(c: Context) = superAdminActivateBtn(LanguageManager.getLanguage(c))
    fun wipeAllRoomsBtn(lang: AppLanguage) = t(lang, "💥 Удалить ВСЕ существующие комнаты сети", "💥 Видалити ВСІ кімнати мережі", "💥 Wipe ALL Network Rooms")
    fun wipeAllRoomsBtn(c: Context) = wipeAllRoomsBtn(LanguageManager.getLanguage(c))
    fun wipeAllRoomsConfirmTitle(lang: AppLanguage) = t(lang, "ВНИМАНИЕ: Очистка сети", "УВАГА: Очищення мережі", "WARNING: Network Wipe")
    fun wipeAllRoomsConfirmTitle(c: Context) = wipeAllRoomsConfirmTitle(LanguageManager.getLanguage(c))
    fun wipeAllRoomsConfirmText(lang: AppLanguage) = t(
        lang,
        "Это полностью удалит ВСЕ зарегистрированные комнаты на всех серверах брокеров. Никто не сможет войти в старые комнаты. Продолжить?",
        "Це повністю видалить ВСІ зареєстровані кімнати на всіх серверах брокерів. Ніхто не зможе увійти в старі кімнати. Продовжити?",
        "This will completely erase ALL registered rooms across all brokers. Nobody will be able to enter previous rooms. Proceed?"
    )
    fun wipeAllRoomsConfirmText(c: Context) = wipeAllRoomsConfirmText(LanguageManager.getLanguage(c))
    fun wipeAllRoomsConfirmAction(lang: AppLanguage) = t(lang, "Удалить все комнаты", "Видалити всі кімнати", "Wipe All Rooms")
    fun wipeAllRoomsConfirmAction(c: Context) = wipeAllRoomsConfirmAction(LanguageManager.getLanguage(c))

    // --- Group Members Dialog ---
    fun membersDialogTitle(lang: AppLanguage, count: Int) = t(
        lang,
        "Участники в эфире ($count)",
        "Учасники в ефірі ($count)",
        "Active Members ($count)"
    )
    fun membersDialogTitle(c: Context, count: Int) = membersDialogTitle(LanguageManager.getLanguage(c), count)
    fun ownerBadge(lang: AppLanguage) = t(lang, "👑 Создатель", "👑 Творець", "👑 Creator")
    fun ownerBadge(c: Context) = ownerBadge(LanguageManager.getLanguage(c))
    fun adminBadge(lang: AppLanguage) = t(lang, "🛡️ Админ", "🛡️ Адмін", "🛡️ Admin")
    fun adminBadge(c: Context) = adminBadge(LanguageManager.getLanguage(c))
    fun memberBadge(lang: AppLanguage) = t(lang, "👤 Участник", "👤 Учасник", "👤 Member")
    fun memberBadge(c: Context) = memberBadge(LanguageManager.getLanguage(c))
    fun superBadge(lang: AppLanguage) = t(lang, "⚡ СУПЕРАДМИН", "⚡ СУПЕРАДМІН", "⚡ SUPER ADMIN")
    fun superBadge(c: Context) = superBadge(LanguageManager.getLanguage(c))

    fun actionManageBtn(lang: AppLanguage) = t(lang, "Действие", "Дія", "Action")
    fun actionManageBtn(c: Context) = actionManageBtn(LanguageManager.getLanguage(c))

    fun makeAdminBtn(lang: AppLanguage) = t(lang, "👑 Назначить администратором", "👑 Призначити адміністратором", "👑 Make Administrator")
    fun makeAdminBtn(c: Context) = makeAdminBtn(LanguageManager.getLanguage(c))
    fun removeAdminBtn(lang: AppLanguage) = t(lang, "Снять права администратора", "Зняти права адміністратора", "Demote from Administrator")
    fun removeAdminBtn(c: Context) = removeAdminBtn(LanguageManager.getLanguage(c))
    fun kickBtn(lang: AppLanguage) = t(lang, "🚪 Выгнать сейчас (Kick)", "🚪 Вигнати зараз (Kick)", "🚪 Kick Member")
    fun kickBtn(c: Context) = kickBtn(LanguageManager.getLanguage(c))
    fun ban15mBtn(lang: AppLanguage) = t(lang, "⏳ Забанить на 15 минут", "⏳ Забанити на 15 хвилин", "⏳ Ban for 15 min")
    fun ban15mBtn(c: Context) = ban15mBtn(LanguageManager.getLanguage(c))
    fun ban1hBtn(lang: AppLanguage) = t(lang, "⏳ Забанить на 1 час", "⏳ Забанити на 1 годину", "⏳ Ban for 1 hour")
    fun ban1hBtn(c: Context) = ban1hBtn(LanguageManager.getLanguage(c))
    fun ban24hBtn(lang: AppLanguage) = t(lang, "⏳ Забанить на 24 часа", "⏳ Забанити на 24 години", "⏳ Ban for 24 hours")
    fun ban24hBtn(c: Context) = ban24hBtn(LanguageManager.getLanguage(c))
    fun banForeverBtn(lang: AppLanguage) = t(lang, "🚫 Забанить навсегда", "🚫 Забанити назавжди", "🚫 Ban Permanently")
    fun banForeverBtn(c: Context) = banForeverBtn(LanguageManager.getLanguage(c))

    // --- Notifications & Toasts ---
    fun roomDeletedNotice(lang: AppLanguage) = t(
        lang,
        "Комната была полностью удалена создателем сети",
        "Кімната була повністю видалена творцем мережі",
        "The room was completely deleted by the network creator"
    )
    fun roomDeletedNotice(c: Context) = roomDeletedNotice(LanguageManager.getLanguage(c))
    fun kickNotice(lang: AppLanguage) = t(
        lang,
        "Вы были исключены из комнаты администратором",
        "Вас було виключено з кімнати адміністратором",
        "You were kicked from the room by an administrator"
    )
    fun kickNotice(c: Context) = kickNotice(LanguageManager.getLanguage(c))
    fun banNotice(lang: AppLanguage, untilTime: String) = t(
        lang,
        "Вы заблокированы в этой комнате до $untilTime",
        "Вас заблоковано в цій кімнаті до $untilTime",
        "You are banned in this room until $untilTime"
    )
    fun banNotice(c: Context, untilTime: String) = banNotice(LanguageManager.getLanguage(c), untilTime)
    fun banPermanentNotice(lang: AppLanguage) = t(
        lang,
        "Вы заблокированы в этой комнате навсегда",
        "Вас заблоковано в цій кімнаті назавжди",
        "You are permanently banned in this room"
    )
    fun banPermanentNotice(c: Context) = banPermanentNotice(LanguageManager.getLanguage(c))

    // --- Avatar Selection ---
    fun avatarPresetTitle(lang: AppLanguage) = t(lang, "Выбор аватара", "Вибір аватара", "Select Avatar")
    fun avatarPresetTitle(c: Context) = avatarPresetTitle(LanguageManager.getLanguage(c))
    fun avatarCustomPhoto(lang: AppLanguage) = t(lang, "Выбрать из галереи", "Обрати з галереї", "Pick from Gallery")
    fun avatarCustomPhoto(c: Context) = avatarCustomPhoto(LanguageManager.getLanguage(c))

    // --- Telegram Error Mapper ---
    fun mapTelegramError(c: Context, raw: String): String {
        val upper = raw.uppercase()
        return when {
            upper.contains("PHONE_NUMBER_INVALID") -> t(
                c,
                "Некорректный номер телефона. Введите номер в международном формате.",
                "Некоректний номер телефону. Введіть номер у міжнародному форматі.",
                "Invalid phone number. Please enter in international format."
            )
            upper.contains("PHONE_CODE_INVALID") -> t(
                c,
                "Неверный 5-значный код. Проверьте сообщение в приложении Telegram.",
                "Невірний 5-значний код. Перевірте повідомлення у Telegram.",
                "Invalid 5-digit code. Check your Telegram message."
            )
            upper.contains("PHONE_CODE_EXPIRED") -> t(
                c,
                "Срок действия кода истёк. Запросите новый код.",
                "Термін дії коду минув. Запитайте новий код.",
                "Code expired. Please request a new code."
            )
            upper.contains("PASSWORD_HASH_INVALID") -> t(
                c,
                "Неверный облачный пароль 2FA Telegram. Попробуйте снова.",
                "Невірний хмарний пароль 2FA Telegram. Спробуйте знову.",
                "Invalid 2FA Telegram password. Try again."
            )
            upper.contains("PHONE_NUMBER_BANNED") -> t(
                c,
                "Этот номер телефона заблокирован в Telegram.",
                "Цей номер телефону заблоковано в Telegram.",
                "This phone number is banned in Telegram."
            )
            upper.contains("FLOOD_WAIT") -> t(
                c,
                "Слишком много запросов. Пожалуйста, подождите несколько минут.",
                "Забагато запитів. Будь ласка, зачекайте кілька хвилин.",
                "Too many requests. Please wait a few minutes."
            )
            upper.contains("PHONE_NUMBER_UNREGISTERED") -> t(
                c,
                "Этот номер не зарегистрирован в Telegram.",
                "Цей номер не зареєстровано в Telegram.",
                "Phone number is not registered in Telegram."
            )
            else -> t(c, "Ошибка Telegram: $raw", "Помилка Telegram: $raw", "Telegram error: $raw")
        }
    }
}
