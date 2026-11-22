package com.mkz.tessavar.screen.adapters.presentation

data class BattleUnitInfo(
    val unitId: String,
    val remainingHealth: Int,
    val maximumHealth: Int,
    val remainingMana: Int,
    val maximumMana: Int,
    val effects: List<EffectsView.Effect>,
    val isHumanPlayers: Boolean,
)
