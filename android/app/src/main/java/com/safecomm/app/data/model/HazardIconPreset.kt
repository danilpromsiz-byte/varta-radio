package com.safecomm.app.data.model

import android.content.Context
import com.safecomm.app.ui.i18n.AppLanguage

data class HazardIconPreset(
    val id: String,
    val emoji: String,
    val nameRu: String,
    val nameUa: String,
    val nameEn: String
) {
    fun localizedName(lang: AppLanguage): String = when (lang) {
        AppLanguage.RU -> nameRu
        AppLanguage.UK -> nameUa
        AppLanguage.EN -> nameEn
    }
}

val HAZARD_ICON_PRESETS = listOf(
    // 1. Classical Camo Turtle (Default)
    HazardIconPreset("turtle", "🐢", "Черепаха (Камуфляж)", "Черепаха (Камуфляж)", "Camo Turtle (Default)"),
    
    // 2. Military Gear & Personnel
    HazardIconPreset("helmet", "🪖", "Тактический шлем", "Тактичний шолом", "Tactical Helmet"),
    HazardIconPreset("patrol", "👁️", "Патруль / Наблюдение", "Патруль / Спостереження", "Patrol / Recon"),
    HazardIconPreset("vest", "🦺", "Инспектор / Жилет", "Інспектор / Жилет", "Inspector / Vest"),
    HazardIconPreset("scout", "🦅", "Воздушная разведка", "Повітряна розвідка", "Air Recon / Scout"),
    HazardIconPreset("group", "🐺", "Группа прикрытия", "Група прикриття", "Cover Group"),
    
    // 3. Barriers & Checkpoints
    HazardIconPreset("checkpoint", "🛑", "Блокпост / Стоп", "Блокпост / Стоп", "Checkpoint / Stop"),
    HazardIconPreset("barrier", "🚧", "Заграждение / Блок", "Загородження / Блок", "Roadblock / Barrier"),
    HazardIconPreset("restricted", "🚷", "Проход закрыт", "Прохід закрито", "Restricted Area"),
    HazardIconPreset("shield", "🛡️", "Оборонный рубеж", "Оборонний рубіж", "Defense Perimeter"),
    
    // 4. Technology & Recon
    HazardIconPreset("drone", "🚁", "БПЛА / Дрон", "БПЛА / Дрон", "Recon Drone / UAV"),
    HazardIconPreset("radar", "📡", "Радар / РЭБ", "Радар / РЕБ", "Radar / EW Station"),
    HazardIconPreset("radio", "📻", "Пост связи", "Пост зв'язку", "Comms Station"),
    HazardIconPreset("searchlight", "🔦", "Ночной дозор", "Нічний дозір", "Night Searchlight"),
    
    // 5. Extreme & Tactical Danger
    HazardIconPreset("danger", "⚠️", "Особая опасность", "Особлива небезпека", "Hazard Warning"),
    HazardIconPreset("target", "🎯", "Сектор под прицелом", "Сектор під прицілом", "Targeted Sector"),
    HazardIconPreset("mine", "⚡", "Опасность мин", "Небезпека мін", "Minefield / Threat"),
    HazardIconPreset("camp", "⛺", "Полевой лагерь", "Польовий табір", "Field Camp"),
    HazardIconPreset("biohazard", "☣️", "Биоугроза", "Біозагроза", "Biohazard"),
    HazardIconPreset("radiation", "☢️", "Радиация / Зона", "Радіація / Зона", "Radiation Hazard")
)

object HazardIconManager {
    private const val PREFS_NAME = "varta_hazard_icon_prefs"
    private const val KEY_MILITARY_ICON = "military_hazard_icon"

    fun getMilitaryIcon(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MILITARY_ICON, "🐢") ?: "🐢"
    }

    fun setMilitaryIcon(context: Context, icon: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MILITARY_ICON, icon).apply()
    }

    fun getPresetByEmoji(emoji: String): HazardIconPreset {
        return HAZARD_ICON_PRESETS.firstOrNull { it.emoji == emoji } ?: HAZARD_ICON_PRESETS[0]
    }
}
