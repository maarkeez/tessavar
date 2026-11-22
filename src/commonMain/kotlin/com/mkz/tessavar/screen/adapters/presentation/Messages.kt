package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.localization.adapters.presentation.LocalizationApi
import com.mkz.tessavar.localization.domain.Localization.Dto.LanguageDto
import com.mkz.tessavar.world.domain.World

class Messages(
    private val localizationApi: LocalizationApi,
    private val worldId: String = World.CAMPAIGN_ID,
) {
    operator fun get(
        text: UiMessage,
        vararg arguments: Pair<String, Any>,
    ): String = localizationApi.translate(text.key, arguments.toArguments())

    fun plural(
        text: UiMessage,
        count: Int,
        vararg arguments: Pair<String, Any>,
    ): String = localizationApi.translatePlural(text.key, count, arguments.toArguments())

    fun gold(amount: Int): String = get(UiMessage.GOLD, "gold" to amount)

    fun unitName(unitId: String): String = localizationApi.translate("unit.$unitId.name")

    fun abilityName(abilityId: String): String = localizationApi.translate("ability.$abilityId.name")

    fun effectVerb(effectId: String): String = localizationApi.translate("effect.$effectId.verb")

    fun effectName(effectId: String): String = localizationApi.translate("effect.$effectId.name")

    fun effectCondition(effectId: String): String = localizationApi.translate("effect.$effectId.condition")

    fun lootableItemName(lootableItemId: String): String = localizationApi.translate("lootableItem.$lootableItemId.name")

    fun npcName(npcId: String): String = localizationApi.translate("npc.$npcId.name")

    fun npcOwnLine(npcId: String): String = localizationApi.translate("npc.$npcId.line")

    fun playerName(playerId: String): String = localizationApi.translate("player.$playerId.name")

    fun terrainName(terrainId: String): String = localizationApi.translate("terrain.$terrainId.name")

    fun questName(
        questId: String,
        english: String,
    ): String = worldText("quest.$questId.name", english)

    fun questDescription(
        questId: String,
        english: String,
    ): String = worldText("quest.$questId.description", english)

    fun questTask(
        questId: String,
        taskIndex: Int,
        english: String,
    ): String = worldText("quest.$questId.task.$taskIndex", english)

    fun questLine(
        questId: String,
        line: String,
        english: String,
    ): String = worldText("quest.$questId.line.$line", english)

    private fun worldText(
        key: String,
        english: String,
    ): String = localizationApi.translate("world.$worldId.$key", fallback = english)

    fun languages(): List<LanguageDto> = localizationApi.searchLanguages()

    fun selectLanguage(languageCode: String) = localizationApi.selectLanguage(languageCode)

    private fun Array<out Pair<String, Any>>.toArguments(): Map<String, String> = associate { (name, value) -> name to value.toString() }
}
