package com.mkz.tessavar.battleUnit.adapters.events

import com.mkz.tessavar.battle.domain.BattleEvent
import com.mkz.tessavar.battleUnit.usecases.commands.EndStacksAtZero
import com.mkz.tessavar.battleUnit.usecases.commands.VanishSummons
import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.shared.domain.subscribe

class OnBattleFinished(
    vanishSummons: VanishSummons,
    endStacksAtZero: EndStacksAtZero,
    eventBus: EventBus,
) {
    val subscription =
        eventBus.subscribe<BattleEvent.BattleFinished> {
            endStacksAtZero()
            vanishSummons()
        }
}
