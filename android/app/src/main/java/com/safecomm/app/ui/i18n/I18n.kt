package com.safecomm.app.ui.i18n

import android.content.Context

object I18n {

    fun t(context: Context, ru: String, uk: String, en: String): String {
        return when (LanguageManager.getLanguage(context)) {
            AppLanguage.UK -> uk
            AppLanguage.EN -> en
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

    fun online(c: Context) = t(c, "В сети", "У мережі", "Online")
    fun offline(c: Context) = t(c, "Не подключено", "Не підключено", "Disconnected")
    fun connecting(c: Context) = t(c, "Подключение...", "Підключення...", "Connecting...")
    fun cancel(c: Context) = t(c, "Отмена", "Скасувати", "Cancel")
    fun save(c: Context) = t(c, "Сохранить", "Зберегти", "Save")
    fun close(c: Context) = t(c, "Закрыть", "Закрити", "Close")
    fun continueBtn(c: Context) = t(c, "Продолжить", "Продовжити", "Continue")
    fun you(c: Context) = t(c, "(Вы)", "(Ви)", "(You)")

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
    fun superAdminQuickBtn(c: Context) = t(c, "⚡ Суперадмин", "⚡ Суперадмін", "⚡ Super Admin")

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

    // --- Main Hub Screen ---
    fun tabRadio(c: Context) = t(c, "Рация", "Рація", "Radio")
    fun tabChat(c: Context) = t(c, "Чат", "Чат", "Chat")
    fun tabMap(c: Context) = t(c, "Карта", "Карта", "Map")
    fun tabSos(c: Context) = t(c, "SOS", "SOS", "SOS")

    fun holdToTalk(c: Context) = t(c, "УДЕРЖИВАЙТЕ ДЛЯ СВЯЗИ", "УТРИМУЙТЕ ДЛЯ ЗВ'ЯЗКУ", "HOLD TO TALK")
    fun transmitting(c: Context) = t(c, "В ЭФИРЕ...", "В ЕФІРІ...", "TRANSMITTING...")
    fun receiving(c: Context, speaker: String) = t(
        c,
        "ПРИЁМ: $speaker",
        "ПРИЙОМ: $speaker",
        "RECEIVING: $speaker"
    )
    fun chatInputHint(c: Context) = t(c, "Сообщение в эфир...", "Повідомлення в ефір...", "Message to room...")
    fun send(c: Context) = t(c, "Отправить", "Надіслати", "Send")
    fun dangerBtn(c: Context) = t(c, "Метка опасности", "Мітка небезпеки", "Danger Mark")
    fun photoBtn(c: Context) = t(c, "Фото с места", "Фото з місця", "Photo Report")
    fun sosBtn(c: Context) = t(c, "СИГНАЛ БЕДСТВИЯ", "СИГНАЛ БІДИ", "DISTRESS BEACON")
    fun cancelSosBtn(c: Context) = t(c, "ОТМЕНА СИГНАЛА SOS", "СКАСУВАТИ СИГНАЛ SOS", "CANCEL SOS ALERT")

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
}
