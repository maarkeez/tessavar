package com.mkz.tessavar.telemetry.domain

import com.mkz.tessavar.telemetry.domain.Telemetry.Dto.ConsentStatusDto
import com.mkz.tessavar.telemetry.domain.TelemetryEvent.ProductEventRecorded
import com.mkz.tessavar.telemetry.domain.TelemetryEvent.ProductEventRecorded.Name
import kotlin.jvm.JvmInline

@ConsistentCopyVisibility
data class Telemetry private constructor(
    private val consent: Consent,
    private val installId: InstallId?,
    private val sessionId: SessionId?,
    private val referral: Referral?,
    private val gameId: GameId?,
    private val events: Set<TelemetryEvent>,
) {
    companion object {
        const val PRIVACY_NOTICE_VERSION = 4

        const val CONSENT_ASKED_AGAIN_AFTER_DAYS = 180

        private const val MILLISECONDS_PER_DAY = 86_400_000.0

        fun notAsked(): Telemetry =
            Telemetry(
                consent = Consent(status = ConsentStatusDto.NOT_ASKED, noticeVersion = null, decidedAt = null),
                installId = null,
                sessionId = null,
                referral = null,
                gameId = null,
                events = emptySet(),
            )

        fun read(dto: Dto): Telemetry =
            Telemetry(
                consent = Consent(status = dto.consent.status, noticeVersion = dto.consent.noticeVersion, decidedAt = dto.consent.decidedAt),
                installId = dto.installId?.let(::InstallId),
                sessionId = dto.sessionId?.let(::SessionId),
                referral = Referral.of(dto.referral),
                gameId = dto.gameId?.let(::GameId),
                events = emptySet(),
            )
    }

    fun toDto() =
        Dto(
            consent = Dto.ConsentDto(status = consent.status, noticeVersion = consent.noticeVersion, decidedAt = consent.decidedAt),
            installId = installId?.value,
            sessionId = sessionId?.value,
            referral = referral?.value,
            gameId = gameId?.value,
        )

    fun startSession(
        sessionId: String,
        referral: String? = null,
    ): Telemetry = copy(sessionId = SessionId(sessionId), referral = Referral.of(referral))

    fun giveConsent(
        installId: String,
        decidedAt: Double,
    ): Telemetry =
        copy(
            consent = Consent(status = ConsentStatusDto.GIVEN, noticeVersion = PRIVACY_NOTICE_VERSION, decidedAt = decidedAt),
            installId = this.installId ?: InstallId(installId),
        )

    fun declineConsent(decidedAt: Double): Telemetry =
        copy(
            consent = Consent(status = ConsentStatusDto.DECLINED, noticeVersion = PRIVACY_NOTICE_VERSION, decidedAt = decidedAt),
            installId = null,
            gameId = null,
        )

    fun isConsentAsked(now: Double): Boolean =
        when {
            consent.status == ConsentStatusDto.NOT_ASKED -> true
            consent.noticeVersion != PRIVACY_NOTICE_VERSION -> true
            consent.status == ConsentStatusDto.DECLINED -> now - (consent.decidedAt ?: 0.0) >= CONSENT_ASKED_AGAIN_AFTER_DAYS * MILLISECONDS_PER_DAY
            else -> false
        }

    fun loadGameId(gameId: String?): Telemetry = copy(gameId = gameId?.let(::GameId))

    fun mainMenuOpened(occurredAt: Double): Telemetry = copy(gameId = null).record(Name.MAIN_MENU_OPENED, occurredAt)

    fun startGame(
        gameId: String,
        worldId: String,
        occurredAt: Double,
    ): Telemetry {
        if (!isConsentGiven()) return this
        return copy(gameId = GameId(gameId)).record(Name.GAME_STARTED, occurredAt, Details(worldId = WorldId.of(worldId)?.value))
    }

    fun gameLoaded(
        slot: Int,
        newGameId: String,
        occurredAt: Double,
    ): Telemetry {
        if (!isConsentGiven()) return this
        return copy(gameId = gameId ?: GameId(newGameId)).record(Name.GAME_LOADED, occurredAt, Details(slot = slot))
    }

    fun gameSaved(
        slot: Int,
        occurredAt: Double,
    ): Telemetry = record(Name.GAME_SAVED, occurredAt, Details(slot = slot))

    fun questStarted(
        questId: String,
        occurredAt: Double,
    ): Telemetry = record(Name.QUEST_STARTED, occurredAt, Details(questId = questId))

    fun questCompleted(
        questId: String,
        occurredAt: Double,
    ): Telemetry = record(Name.QUEST_COMPLETED, occurredAt, Details(questId = questId))

    fun questAbandoned(
        questId: String,
        occurredAt: Double,
    ): Telemetry = record(Name.QUEST_ABANDONED, occurredAt, Details(questId = questId))

    fun battleStarted(occurredAt: Double): Telemetry = record(Name.BATTLE_STARTED, occurredAt)

    fun battleFinished(occurredAt: Double): Telemetry = record(Name.BATTLE_FINISHED, occurredAt)

    fun gameOver(occurredAt: Double): Telemetry = record(Name.GAME_OVER, occurredAt)

    fun itemBought(
        lootableItemId: String,
        price: Int,
        occurredAt: Double,
    ): Telemetry = record(Name.ITEM_BOUGHT, occurredAt, Details(lootableItemId = lootableItemId, price = price))

    fun itemSold(
        lootableItemId: String,
        sellPrice: Int,
        occurredAt: Double,
    ): Telemetry = record(Name.ITEM_SOLD, occurredAt, Details(lootableItemId = lootableItemId, price = sellPrice))

    fun memberRecruited(
        unitId: String,
        occurredAt: Double,
    ): Telemetry = record(Name.MEMBER_RECRUITED, occurredAt, Details(unitId = unitId))

    fun pullEvents(): Pair<Set<TelemetryEvent>, Telemetry> = events to copy(events = emptySet())

    private fun record(
        name: Name,
        occurredAt: Double,
        details: Details = Details(),
    ): Telemetry {
        val installId = installId ?: return this
        val sessionId = sessionId ?: return this
        if (!isConsentGiven()) return this
        return copy(
            events =
                events +
                    ProductEventRecorded(
                        name = name,
                        installId = installId.value,
                        sessionId = sessionId.value,
                        gameId = gameId?.value,
                        occurredAt = occurredAt,
                        questId = details.questId,
                        slot = details.slot,
                        lootableItemId = details.lootableItemId,
                        price = details.price,
                        unitId = details.unitId,
                        referral = referral?.value,
                        worldId = details.worldId,
                    ),
        )
    }

    private fun isConsentGiven() = consent.status == ConsentStatusDto.GIVEN && consent.noticeVersion == PRIVACY_NOTICE_VERSION

    private data class Consent(
        val status: ConsentStatusDto,
        val noticeVersion: Int?,
        val decidedAt: Double?,
    )

    @JvmInline private value class InstallId(
        val value: String,
    )

    @JvmInline private value class SessionId(
        val value: String,
    )

    @JvmInline private value class GameId(
        val value: String,
    )

    @JvmInline private value class Referral private constructor(
        val value: String,
    ) {
        companion object {
            private val WORD = Regex("[a-z0-9-]{1,32}")

            fun of(text: String?): Referral? = text?.takeIf(WORD::matches)?.let(::Referral)
        }
    }

    @JvmInline private value class WorldId private constructor(
        val value: String,
    ) {
        companion object {
            private val WORDS = Regex("[a-z0-9-]{1,64}")

            fun of(text: String): WorldId? = text.takeIf(WORDS::matches)?.let(::WorldId)
        }
    }

    private data class Details(
        val questId: String? = null,
        val slot: Int? = null,
        val lootableItemId: String? = null,
        val price: Int? = null,
        val unitId: String? = null,
        val worldId: String? = null,
    )

    data class Dto(
        val consent: ConsentDto,
        val installId: String?,
        val sessionId: String?,
        val referral: String?,
        val gameId: String?,
    ) {
        data class ConsentDto(
            val status: ConsentStatusDto,
            val noticeVersion: Int?,
            val decidedAt: Double?,
        )

        enum class ConsentStatusDto {
            NOT_ASKED,
            GIVEN,
            DECLINED,
        }
    }
}
