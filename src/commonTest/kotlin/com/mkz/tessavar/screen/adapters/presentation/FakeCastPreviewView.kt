package com.mkz.tessavar.screen.adapters.presentation

class FakeCastPreviewView : CastPreviewView(GameSceneResourcesLoader(), MessagesMother.messages()) {
    data class DisplayedCastPreview(
        val abilityId: String,
        val casterBattleUnit: BattleUnitInfo,
        val manaCost: Int,
        val recipients: List<Recipient>,
        val casterRemainingHealth: Int,
    )

    val displayedCastPreviews = mutableListOf<DisplayedCastPreview>()
    var hideCount = 0
        private set

    override fun display(
        abilityId: String,
        casterBattleUnit: BattleUnitInfo,
        manaCost: Int,
        recipients: List<Recipient>,
        casterRemainingHealth: Int,
    ) {
        displayedCastPreviews += DisplayedCastPreview(abilityId, casterBattleUnit, manaCost, recipients, casterRemainingHealth)
    }

    override fun hide() {
        hideCount++
    }
}
