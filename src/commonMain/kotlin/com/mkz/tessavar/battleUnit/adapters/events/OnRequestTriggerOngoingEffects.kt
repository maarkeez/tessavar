package com.mkz.tessavar.battleUnit.adapters.events

import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent
import com.mkz.tessavar.battleUnit.usecases.commands.TriggerOngoingEffectsWhileExploring
import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.shared.domain.subscribe

class OnRequestTriggerOngoingEffects(
    private val triggerOngoingEffectsWhileExploring: TriggerOngoingEffectsWhileExploring,
    eventBus: EventBus,
) {
    val subscription =
        eventBus.subscribe<BattleUnitEvent.RequestTriggerOngoingEffects> { event ->
            triggerOngoingEffectsWhileExploring(battleUnitIds = event.battleUnitIds)
        }
}
