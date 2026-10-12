package com.safecomm.app.ui.i18n

import android.content.Context

object I18n {

    fun t(context: Context, ru: String, uk: String, en: String): String {
        return when (LanguageManager.getLanguage(context)) {
            AppLanguage.EN -> en
            AppLanguage.UK -> uk
            AppLanguage.RU -> ru
        }
    }

    // --- Common & App Info ---
    fun appTitle(c: Context) = "Varta GO"
    fun appSubtitle(c: Context) = t(
        c,
        "Тактическая защищённая радиосвязь",
        "Тактичний захищений радіозв'язок",
        "Tactical Secure Radio Communications"
    )
    fun encryptionBadge(c: Context) = t(
        c,
        "AES-256 Защищённый P2P",
        "AES-256 Захищений P2P",
        "AES-256 Retained P2P"
    )

    fun online(c: Context) = t(c, "В сети", "У мережі", "Online")
    fun offline(c: Context) = t(c, "Не подключено", "Не підключено", "Disconnected")
    fun connecting(c: Context) = t(c, "Подключение к сети...", "Підключення до мережі...", "Connecting to network...")
    fun cancel(c: Context) = t(c, "Отмена", "Скасувати", "Cancel")
    fun save(c: Context) = t(c, "Сохранить", "Зберегти", "Save")
    fun close(c: Context) = t(c, "Закрыть", "Закрити", "Close")
    fun continueBtn(c: Context) = t(c, "Продолжить", "Продовжити", "Continue")
    fun you(c: Context) = t(c, "(Вы)", "(Ви)", "(You)")
    fun copiedToClipboard(c: Context) = t(c, "Скопировано в буфер", "Скопійовано в буфер", "Copied to clipboard")

    // --- Language Selection ---
    fun selectLanguageTitle(c: Context) = t(
        c,
        "Выберите язык интерфейса",
        "Оберіть мову інтерфейсу",
        "Select Interface Language"
    )
    fun selectLanguageSubtitle(c: Context) = t(
        c,
        "Вы можете изменить язык в любое время в настройках",
        "Ви можете змінити мову в будь-який час у налаштуваннях",
        "You can change the language anytime in settings"
    )

    // --- Auth & Community Screen ---
    fun tabConnect(c: Context) = t(c, "Подключение", "Підключення", "Connect")
    fun tabCreate(c: Context) = t(c, "Создать комнату", "Створити кімнату", "Create Room")
    fun callSignLabel(c: Context) = t(c, "Ваш позывной", "Ваш позивний", "Your Callsign")
    fun callSignHint(c: Context) = t(c, "Например: Сокол-7", "Наприклад: Сокіл-7", "e.g. Falcon-7")
    fun roomLabel(c: Context) = t(c, "Номер или название комнаты", "Номер або назва кімнати", "Room Number or Name")
    fun roomHint(c: Context) = t(c, "Например: 24 или Штаб", "Наприклад: 24 або Штаб", "e.g. 24 or Base")
    fun passwordLabel(c: Context) = t(c, "Ключ доступа (пароль)", "Ключ доступу (пароль)", "Access Key (Password)")
    fun passwordHint(c: Context) = t(c, "Пароль для защиты комнаты", "Пароль для захисту кімнати", "Password to protect room")
    fun enterRadioBtn(c: Context) = t(c, "Войти в эфир", "Увійти в ефір", "Enter Radio")
    fun createRoomBtn(c: Context) = t(c, "Создать комнату и войти", "Створити кімнату та увійти", "Create Room & Enter")
    fun darkRoomTitle(c: Context) = t(c, "🔒 Тёмная комната (по умолчанию)", "🔒 Темна кімната (за замовчуванням)", "🔒 Dark Room (default)")
    fun darkRoomDesc(c: Context) = t(
        c,
        "Общение строго внутри Varta GO. Никто в обычном Telegram не увидит эту комнату.",
        "Спілкування виключно всередині Varta GO. Ніхто у звичайному Telegram не побачить цю кімнату.",
        "Communication strictly inside Varta GO. Nobody in standard Telegram will see this room."
    )
    fun ephemeralTitle(c: Context) = t(c, "⏳ Самоуничтожение (24 часа)", "⏳ Самознищення (24 години)", "⏳ Self-Destruct (24 hours)")
    fun ephemeralDesc(c: Context) = t(
        c,
        "Сообщения, радиозаписи и метки карты автоматически стираются через 24 часа.",
        "Повідомлення, радіозаписи та мітки карти автоматично стираються через 24 години.",
        "Messages, voice records and map markers automatically vanish after 24 hours."
    )
    fun telegramBridgeTitle(c: Context) = t(c, "Трансляция в Telegram", "Трансляція в Telegram", "Relay to Telegram")
    fun telegramBridgeDesc(c: Context) = t(
        c,
        "Дублировать голосовые сообщения и метки в Telegram",
        "Дублювати голосові повідомлення та мітки в Telegram",
        "Relay voice messages and markers to Telegram"
    )
    fun telegramAuthRequired(c: Context) = t(c, "Требуется авторизация в Telegram", "Потрібна авторизація в Telegram", "Telegram login required")
    fun telegramLoginTitle(c: Context) = t(c, "Авторизация в Telegram (Опционально)", "Авторизація в Telegram (Опціонально)", "Telegram Login (Optional)")
    fun phoneLabel(c: Context) = t(c, "Номер телефона", "Номер телефону", "Phone Number")
    fun sendCodeBtn(c: Context) = t(c, "Получить код", "Отримати код", "Send Code")
    fun codeLabel(c: Context) = t(c, "Код из Telegram", "Код із Telegram", "Telegram Code")
    fun confirmCodeBtn(c: Context) = t(c, "Подтвердить код", "Підтвердити код", "Confirm Code")
    fun password2faLabel(c: Context) = t(c, "Облачный пароль 2FA", "Хмарний пароль 2FA", "2FA Cloud Password")
    fun confirmPasswordBtn(c: Context) = t(c, "Войти", "Увійти", "Sign In")
    fun logOutTelegram(c: Context) = t(c, "Выйти из Telegram", "Вийти з Telegram", "Log Out Telegram")
    fun checkingRoom(c: Context) = t(c, "Проверка комнаты...", "Перевірка кімнати...", "Checking room...")
    fun roomAvailable(c: Context) = t(c, "Комната свободна для создания", "Кімната вільна для створення", "Room name is available")
    fun roomExists(c: Context) = t(c, "Комната существует (введите пароль)", "Кімната існує (введіть пароль)", "Room exists (enter password)")
    fun roomInvalidName(c: Context) = t(c, "Введите название комнаты", "Введіть назву кімнати", "Enter room name")
    fun callSignRequired(c: Context) = t(c, "Укажите ваш позывной", "Вкажіть ваш позивний", "Enter your callsign")

    // --- App Updates ---
    fun updatesSection(c: Context) = t(c, "📦 Обновления приложения", "📦 Оновлення застосунку", "📦 App Updates")
    fun currentVersionLabel(c: Context, v: String) = t(c, "Версия: v$v", "Версія: v$v", "Version: v$v")
    fun checkForUpdatesBtn(c: Context) = t(c, "🔄 Проверить обновления", "🔄 Перевірити оновлення", "🔄 Check for Updates")
    fun reinstallUpdateBtn(c: Context) = t(c, "Принудительно переустановить", "Примусово перевстановити", "Reinstall latest from GitHub")
    fun checkingUpdatesStatus(c: Context) = t(c, "Проверка релизов на GitHub...", "Перевірка релізів на GitHub...", "Checking GitHub releases...")
    fun updateAvailableStatus(c: Context, v: String) = t(c, "🎉 Доступна новая версия: $v!", "🎉 Доступна нова версія: $v!", "🎉 New version available: $v!")
    fun upToDateStatus(c: Context, v: String) = t(c, "✅ Установлена актуальная версия (v$v)", "✅ Встановлена актуальна версія (v$v)", "✅ Up to date (v$v)")
    fun downloadingUpdateStatus(c: Context, p: Int) = t(c, "⬇️ Загрузка обновления: $p%", "⬇️ Завантаження оновлення: $p%", "⬇️ Downloading: $p%")
    fun readyToInstallStatus(c: Context) = t(c, "📦 Запуск установки APK...", "📦 Запуск встановлення APK...", "📦 Launching APK installer...")
    fun installUpdateBtn(c: Context) = t(c, "⚡ Обновить сейчас", "⚡ Оновити зараз", "⚡ Update Now")
    fun updateModalTitle(c: Context, v: String) = t(c, "⚡ Доступно обновление: $v", "⚡ Доступне оновлення: $v", "⚡ App Update Available: $v")
    fun updateModalDesc(c: Context) = t(
        c,
        "Вышла новая версия приложения Varta GO.\nОбновитесь для стабильной связи и актуальной защиты.",
        "Вийшла нова версія застосунку Varta GO.\nОновіться для стабільного зв'язку та актуального захисту.",
        "A new version of Varta GO is available.\nPlease update for stable radio connectivity and enhanced security."
    )
    fun whatsNew(c: Context) = t(c, "Что нового:", "Що нового:", "What's new:")
    fun downloadingUpdateTitle(c: Context) = t(c, "⬇️ Загрузка обновления...", "⬇️ Завантаження оновлення...", "⬇️ Downloading update...")
    fun downloadProgressText(c: Context, p: Int) = t(c, "Загрузка новой версии: $p%", "Завантаження нової версії: $p%", "Downloading new version: $p%")
    fun downloadWaitText(c: Context) = t(c, "Пожалуйста, подождите завершения загрузки. Установка начнется автоматически.", "Будь ласка, зачекайте завершення завантаження. Встановлення почнеться автоматично.", "Please wait for download to finish. Installation will start automatically.")
    fun updateDownloadedTitle(c: Context) = t(c, "📦 Обновление загружено", "📦 Оновлення завантажено", "📦 Update Downloaded")
    fun updateDownloadedDesc(c: Context) = t(c, "Файл обновления успешно загружен на устройство.", "Файл оновлення успішно завантажено на пристрій.", "Update file successfully downloaded.")
    fun installNowPrompt(c: Context) = t(c, "Нажмите «Установить сейчас» для применения новой версии.", "Натисніть «Встановити зараз» для застосування нової версії.", "Tap «Install Now» to apply the new version.")
    fun installNowBtn(c: Context) = t(c, "📦 Установить сейчас", "📦 Встановити зараз", "📦 Install Now")
    fun updateErrorTitle(c: Context) = t(c, "⚠️ Ошибка обновления", "⚠️ Помилка оновлення", "⚠️ Update Error")
    fun gotItBtn(c: Context) = t(c, "Понятно", "Зрозуміло", "Got it")
    fun updateAvailableToast(c: Context) = t(c, "Найдено обновление! Нажмите «Обновить»", "Знайдено оновлення! Натисніть «Оновити»", "Update found! Tap to update")
    fun downloadingApk(c: Context) = t(c, "Скачивание обновления...", "Завантаження оновлення...", "Downloading update...")

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
    fun membersOnlineCount(c: Context, count: Int) = t(c, "$count в сети", "$count у мережі", "$count online")
    fun silentModeBadge(c: Context) = t(c, "🔇 Без звука", "🔇 Без звуку", "🔇 Muted")
    fun loudspeakerBadge(c: Context) = t(c, "🔊 Рация", "🔊 Рація", "🔊 Radio")
    fun switchRoomTooltip(c: Context) = t(c, "Сменить комнату", "Змінити кімнату", "Switch Room")
    fun voiceHistoryTooltip(c: Context) = t(c, "История рации", "Історія рації", "Radio History")
    fun membersTooltip(c: Context) = t(c, "Участники", "Учасники", "Members")
    fun settingsTooltip(c: Context) = t(c, "Настройки", "Налаштування", "Settings")
    fun channelClear(c: Context) = t(c, "Радиоэфир свободен", "Радіоефір вільний", "Channel is clear")
    fun youAreTransmitting(c: Context) = t(c, "ВЫ В ЭФИРЕ (Передача)", "ВИ В ЕФІРІ (Передача)", "TRANSMITTING...")
    fun activeSpeakerVoice(c: Context, speaker: String) = t(
        c,
        "ГОЛОС В ЭФИРЕ: $speaker",
        "ГОЛОС В ЕФІРІ: $speaker",
        "VOICE IN AIR: $speaker"
    )
    fun activeSpeakerBadge(c: Context) = t(c, "🔊 АКТИВНО", "🔊 АКТИВНО", "🔊 ACTIVE")
    fun holdToTalk(c: Context) = t(c, "УДЕРЖИВАЙТЕ ДЛЯ СВЯЗИ", "УТРИМУЙТЕ ДЛЯ ЗВ'ЯЗКУ", "HOLD TO TALK")
    fun dangerBtn(c: Context) = t(c, "МЕТКА", "МІТКА", "DANGER")
    fun photoBtn(c: Context) = t(c, "ФОТО", "ФОТО", "PHOTO")
    fun myLocationTooltip(c: Context) = t(c, "Моё местоположение", "Моє місцезнаходження", "My Location")

    fun switchRoomConfirmTitle(c: Context) = t(c, "🚪 Сменить комнату?", "🚪 Змінити кімнату?", "🚪 Switch Room?")
    fun switchRoomConfirmDesc(c: Context, roomName: String) = t(
        c,
        "Вы выйдете из текущего закрытого радиоканала «$roomName» и сможете выбрать другую группу или создать новый канал.\n\nВаша авторизация сохранится.",
        "Ви вийдете з поточного закритого радіоканалу «$roomName» і зможете обрати іншу групу або створити новий канал.\n\nВаша авторизація збережеться.",
        "You will disconnect from current channel «$roomName» and can switch to another room or create a new one.\n\nYour session remains saved."
    )
    fun switchRoomConfirmAction(c: Context) = t(c, "Сменить комнату", "Змінити кімнату", "Switch Room")

    // --- Hazard & Danger Report Dialogs ---
    fun placeDangerTitle(c: Context) = t(c, "📍 ПОСТАВИТЬ МЕТКУ", "📍 ПОСТАВИТИ МІТКУ", "📍 REPORT HAZARD")
    fun chooseDangerDesc(c: Context) = t(
        c,
        "Выберите тип метки для оповещения всех участников группы:",
        "Оберіть тип мітки для сповіщення всіх учасників групи:",
        "Select hazard type to alert everyone in the room:"
    )
    fun dangerDetailsHint(c: Context) = t(
        c,
        "Уточнение (напр. ул. Ленина, 5)",
        "Уточнення (напр. вул. Лесі Українки, 5)",
        "Details (e.g. cross street or building)"
    )
    fun holdToBroadcastPrompt(c: Context) = t(
        c,
        "УДЕРЖИВАЙТЕ 2 сек ДЛЯ ОТПРАВКИ МЕТКИ",
        "УТРИМУЙТЕ 2 сек ДЛЯ ВІДПРАВКИ МІТКИ",
        "HOLD 2s TO BROADCAST HAZARD"
    )
    fun holdingProgress(c: Context, percent: Int) = t(
        c,
        "УДЕРЖИВАЙТЕ ($percent%)",
        "УТРИМУЙТЕ ($percent%)",
        "HOLDING ($percent%)"
    )

    fun photoFixationTitle(c: Context) = t(c, "📷 ФОТОФИКСАЦИЯ ОПАСНОСТИ", "📷 ФОТОФІКСАЦІЯ НЕБЕЗПЕКИ", "📷 HAZARD PHOTO REPORT")
    fun tapToTakePhoto(c: Context) = t(c, "Нажмите, чтобы сделать фото", "Натисніть, щоб зробити фото", "Tap to take a photo")
    fun photoHazardTypeLabel(c: Context) = t(c, "Тип опасности:", "Тип небезпеки:", "Hazard type:")
    fun photoDetailsHint(c: Context) = t(
        c,
        "Описание (напр. беспилотник в небе)",
        "Опис (напр. безпілотник у небі)",
        "Description (e.g. drone spotted)"
    )
    fun holdToSendPhotoPrompt(c: Context) = t(
        c,
        "УДЕРЖИВАЙТЕ ДЛЯ ОТПРАВКИ",
        "УТРИМУЙТЕ ДЛЯ ВІДПРАВКИ",
        "HOLD TO SEND PHOTO"
    )

    // --- Alert Detail Dialog ---
    fun distanceLabel(c: Context) = t(c, "Расстояние", "Відстань", "Distance")
    fun reportedByLabel(c: Context) = t(c, "Сообщил", "Повідомив", "Reported by")
    fun descriptionLabel(c: Context) = t(c, "Описание:", "Опис:", "Description:")
    fun resolveAlertBtn(c: Context) = t(
        c,
        "Снять тревогу (Опасность миновала)",
        "Зняти тривогу (Небезпека минула)",
        "Resolve Hazard (All Clear)"
    )

    // --- Voice History Dialog ---
    fun voiceHistoryTitle(c: Context) = t(c, "🎙 ИСТОРИЯ РАЦИИ", "🎙 ІСТОРІЯ РАЦІЇ", "🎙 RADIO HISTORY")
    fun noVoiceMessages(c: Context) = t(
        c,
        "Голосовых сообщений пока нет",
        "Голосових повідомлень поки немає",
        "No voice messages yet"
    )

    // --- Hazard Category Names ---
    fun alertTypeName(c: Context, rawValue: String): String = when (rawValue.lowercase()) {
        "shelling" -> t(c, "Обстрел", "Обстріл", "Shelling")
        "fire" -> t(c, "Пожар", "Пожежа", "Fire")
        "medical" -> t(c, "Медпомощь", "Меддопомога", "Medical")
        "police" -> t(c, "Полиция", "Поліція", "Police")
        "military" -> t(c, "Военные", "Військові", "Military")
        else -> t(c, "Другое", "Інше", "Other")
    }

    // --- Settings Dialog ---
    fun settingsTitle(c: Context) = t(c, "Настройки комнаты", "Налаштування кімнати", "Room Settings")
    fun languageSection(c: Context) = t(c, "Язык приложения", "Мова застосунку", "App Language")
    fun audioSection(c: Context) = t(c, "Аудио и громкая связь", "Аудіо та гучний зв'язок", "Audio & Loudspeaker")
    fun muteSpeakerTitle(c: Context) = t(c, "Режим тишины (без звука)", "Режим тиші (без звуку)", "Silent Mode (Mute)")
    fun muteSpeakerDesc(c: Context) = t(
        c,
        "Входящие голосовые сообщения не воспроизводятся вслух",
        "Вхідні голосові повідомлення не відтворюються вголос",
        "Incoming voice messages will not play out loud"
    )
    fun storageSection(c: Context) = t(c, "Безопасность и хранение", "Безпека та зберігання", "Security & Storage")
    fun autoDeleteToggle(c: Context) = t(c, "Самоуничтожение сообщений (24ч)", "Самознищення повідомлень (24 год)", "Self-Destruct Messages (24h)")
    fun redirectionSection(c: Context) = t(c, "Сообщение для гостей пустой комнаты", "Повідомлення для гостей порожньої кімнати", "Announcement for room visitors")
    fun redirectionHint(c: Context) = t(c, "Например: Мы перешли в комнату 26", "Наприклад: Ми перейшли в кімнату 26", "e.g. We moved to room 26")
    fun saveRedirectionBtn(c: Context) = t(c, "Сохранить сообщение", "Зберегти повідомлення", "Save Announcement")
    fun bannedMembersSection(c: Context) = t(c, "🚫 Заблокированные участники", "🚫 Заблоковані учасники", "🚫 Banned Members")
    fun unbanBtn(c: Context) = t(c, "Разблокировать", "Розблокувати", "Unban")
    fun noBannedUsers(c: Context) = t(c, "Нет заблокированных участников", "Немає заблокованих учасників", "No banned members")
    fun leaveRoomBtn(c: Context) = t(c, "Покинуть комнату", "Вийти з кімнати", "Leave Room")
    fun deleteRoomBtn(c: Context) = t(c, "🗑️ Удалить комнату совсем", "🗑️ Видалити кімнату назавжди", "🗑️ Delete Room Permanently")
    fun deleteRoomConfirmTitle(c: Context) = t(c, "Удаление комнаты", "Видалення кімнати", "Delete Room")
    fun deleteRoomConfirmText(c: Context) = t(
        c,
        "Вы действительно хотите навсегда удалить эту комнату? Все участники будут отключены, а комната стёрта из сети.",
        "Ви дійсно хочете назавжди видалити цю кімнату? Всі учасники будуть відключені, а кімната стерта з мережі.",
        "Are you sure you want to permanently delete this room? All members will be disconnected and the room erased from the network."
    )
    fun deleteRoomConfirmAction(c: Context) = t(c, "Да, удалить комнату", "Так, видалити кімнату", "Yes, delete room")

    // --- Super Admin ---
    fun superAdminSection(c: Context) = t(c, "⚡ Режим суперадмина", "⚡ Режим суперадміна", "⚡ Super Admin Mode")
    fun superAdminStatusActive(c: Context) = t(c, "⚡ СУПЕРАДМИН АКТИВЕН", "⚡ СУПЕРАДМІН АКТИВНИЙ", "⚡ SUPER ADMIN ACTIVE")
    fun superAdminDesc(c: Context) = t(
        c,
        "У вас есть права на удаление любых комнат и управление всей сетью.",
        "У вас є права на видалення будь-яких кімнат та керування всією мережею.",
        "You have privileges to delete any room and manage the entire network."
    )
    fun superAdminKeyPlaceholder(c: Context) = t(c, "Мастер-пароль суперадмина", "Майстер-пароль суперадміна", "Super Admin Master Passcode")
    fun superAdminActivateBtn(c: Context) = t(c, "Активировать", "Активувати", "Activate")
    fun wipeAllRoomsBtn(c: Context) = t(c, "💥 Удалить ВСЕ существующие комнаты сети", "💥 Видалити ВСІ кімнати мережі", "💥 Wipe ALL Network Rooms")
    fun wipeAllRoomsConfirmTitle(c: Context) = t(c, "ВНИМАНИЕ: Очистка сети", "УВАГА: Очищення мережі", "WARNING: Network Wipe")
    fun wipeAllRoomsConfirmText(c: Context) = t(
        c,
        "Это полностью удалит ВСЕ зарегистрированные комнаты на всех серверах брокеров. Никто не сможет войти в старые комнаты. Продолжить?",
        "Це повністю видалить ВСІ зареєстровані кімнати на всіх серверах брокерів. Ніхто не зможе увійти в старі кімнати. Продовжити?",
        "This will completely erase ALL registered rooms across all brokers. Nobody will be able to enter previous rooms. Proceed?"
    )
    fun wipeAllRoomsConfirmAction(c: Context) = t(c, "Удалить все комнаты", "Видалити всі кімнати", "Wipe All Rooms")

    // --- Group Members Dialog ---
    fun membersDialogTitle(c: Context, count: Int) = t(
        c,
        "Участники в эфире ($count)",
        "Учасники в ефірі ($count)",
        "Active Members ($count)"
    )
    fun ownerBadge(c: Context) = t(c, "👑 Создатель", "👑 Творець", "👑 Creator")
    fun adminBadge(c: Context) = t(c, "🛡️ Админ", "🛡️ Адмін", "🛡️ Admin")
    fun memberBadge(c: Context) = t(c, "👤 Участник", "👤 Учасник", "👤 Member")
    fun superBadge(c: Context) = t(c, "⚡ СУПЕРАДМИН", "⚡ СУПЕРАДМІН", "⚡ SUPER ADMIN")

    fun makeAdminBtn(c: Context) = t(c, "👑 Назначить администратором", "👑 Призначити адміністратором", "👑 Make Administrator")
    fun removeAdminBtn(c: Context) = t(c, "Снять права администратора", "Зняти права адміністратора", "Demote from Administrator")
    fun kickBtn(c: Context) = t(c, "🚪 Выгнать сейчас (Kick)", "🚪 Вигнати зараз (Kick)", "🚪 Kick Member")
    fun ban15mBtn(c: Context) = t(c, "⏳ Забанить на 15 минут", "⏳ Забанити на 15 хвилин", "⏳ Ban for 15 min")
    fun ban1hBtn(c: Context) = t(c, "⏳ Забанить на 1 час", "⏳ Забанити на 1 годину", "⏳ Ban for 1 hour")
    fun ban24hBtn(c: Context) = t(c, "⏳ Забанить на 24 часа", "⏳ Забанити на 24 години", "⏳ Ban for 24 hours")
    fun banForeverBtn(c: Context) = t(c, "🚫 Забанить навсегда", "🚫 Забанити назавжди", "🚫 Ban Permanently")

    // --- Notifications & Toasts ---
    fun roomDeletedNotice(c: Context) = t(
        c,
        "Комната была полностью удалена создателем сети",
        "Кімната була повністю видалена творцем мережі",
        "The room was completely deleted by the network creator"
    )
    fun kickNotice(c: Context) = t(
        c,
        "Вы были исключены из комнаты администратором",
        "Вас було виключено з кімнати адміністратором",
        "You were kicked from the room by an administrator"
    )
    fun banNotice(c: Context, untilTime: String) = t(
        c,
        "Вы заблокированы в этой комнате до $untilTime",
        "Вас заблоковано в цій кімнаті до $untilTime",
        "You are banned in this room until $untilTime"
    )
    fun banPermanentNotice(c: Context) = t(
        c,
        "Вы заблокированы в этой комнате навсегда",
        "Вас заблоковано в цій кімнаті назавжди",
        "You are permanently banned in this room"
    )

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
