package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.localization.adapters.presentation.LocalizationApi
import com.mkz.tessavar.save.adapters.presentation.SaveApi
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.telemetry.adapters.presentation.TelemetryApi
import com.mkz.tessavar.telemetry.domain.TelemetryEvent
import com.mkz.tessavar.world.adapters.presentation.WorldApi
import com.mkz.tessavar.world.adapters.resources.ResourcesWorldLoader
import com.mkz.tessavar.world.domain.World
import korlibs.io.async.launchImmediately
import korlibs.korge.scene.Scene
import korlibs.korge.service.storage.IStorage
import korlibs.korge.service.storage.storage
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.time.DateTime
import kotlin.random.Random

const val SIGNUP_FORM_URL =
    "https://6ecb7558.sibforms.com/serve/" +
        "MUIFAMiQ4UezjxlhEUvRO3oDcVvD4q-Yj7gRRY5R2RY7chTwnIgZPQpX9Hj2oXNl0A6obLKhgcV6YQO5feVIJNoZhZiAdrHOo52br5CeTwDiZlRmR_tAZbqak97rgvttUAk86vbpd32JmS8CbrKQK_XzfFJsGMkm7UE53M7Nn3IKVuQqmqfazm4oVuUm7TyyOae2nZxsmc0BAqnphA=="

class MenuScene(
    private val localizationApi: LocalizationApi,
    private val telemetryApi: TelemetryApi,
    private val storage: IStorage? = null,
    private val random: Random = Random.Default,
    private val incompatibleSlot: Int? = null,
    private val browser: Browser? = null,
) : Scene() {
    private companion object {
        const val PANEL_HEIGHT = 300.0

        val ROW_Y = (GAME_WINDOW_SIZE.height - PANEL_HEIGHT) / 2 + PANEL_HEIGHT + 15
    }

    override val coroutineContext by lazy { super.coroutineContext + views.gameWindow.coroutineDispatcher }

    override suspend fun SContainer.sceneMain() {
        val messages = Messages(localizationApi)
        val eventBus = InMemoryEventBus()
        addUpdater { eventBus.dispatch() }
        telemetryApi.listen(eventBus)
        val worldApi = WorldApi(eventBus, ResourcesWorldLoader())
        worldApi.init()
        val saveApi =
            SaveApi(
                storage = storage ?: views.storage,
                eventBus = eventBus,
                snapshottables = emptyList(),
                canSave = { false },
                leaderUnitId = { null },
                partySize = { 0 },
                worldId = World.CAMPAIGN_ID,
                now = { DateTime.nowUnixMillis() },
            )
        lateinit var loadGamePresenter: LoadGamePresenter
        lateinit var consentPresenter: ConsentPresenter
        lateinit var communityWorldsPresenter: CommunityWorldsPresenter
        val menuView =
            MenuView(
                messages = messages,
                onNewGameSelected = { launchImmediately { sceneContainer.changeTo { GameScene(localizationApi, telemetryApi, storage, random) } } },
                onLoadGameSelected = { loadGamePresenter.open() },
                onCommunityWorldsSelected = { communityWorldsPresenter.open() },
                onWorldBuilderSelected = { launchImmediately { sceneContainer.changeTo { WorldBuilderScene(localizationApi, telemetryApi, storage) } } },
                onPrivacySelected = { consentPresenter.open() },
                onWishlistSelected = { launchImmediately { (browser ?: GameWindowBrowser(views.gameWindow)).open(SIGNUP_FORM_URL) } },
            )
        val saveSlotsView = SaveSlotsView(messages)
        val communityWorldsView = CommunityWorldsView(messages)
        val confirmDialogView = ConfirmDialogView()
        menuView.x = (GAME_WINDOW_SIZE.width - menuView.width) / 2
        menuView.y = (GAME_WINDOW_SIZE.height - menuView.height) / 2
        val callToActionView = CallToActionView(messages)
        callToActionView.y = ROW_Y
        val consentView = ConsentView(messages)
        val privacyNoticeView = PrivacyNoticeView(messages)
        listOf(menuView, saveSlotsView, communityWorldsView, confirmDialogView, callToActionView, consentView, privacyNoticeView).forEach(::addChild)
        listOf(saveSlotsView, communityWorldsView, confirmDialogView).forEach { view -> view.y = (GAME_WINDOW_SIZE.height - PANEL_HEIGHT) / 2 }
        loadGamePresenter =
            LoadGamePresenter(menuView, saveSlotsView, confirmDialogView, callToActionView, saveApi, messages, { id -> worldApi.searchWorld(id)?.name }, incompatibleSlot) { slot, worldId ->
                launchImmediately { sceneContainer.changeTo { GameScene(localizationApi, telemetryApi, storage, random, slot, worldId) } }
            }
        communityWorldsPresenter =
            CommunityWorldsPresenter(menuView, communityWorldsView, callToActionView, worldApi, messages) { worldId ->
                launchImmediately { sceneContainer.changeTo { GameScene(localizationApi, telemetryApi, storage, random, worldId = worldId) } }
            }
        consentPresenter = ConsentPresenter(consentView, privacyNoticeView, telemetryApi)
        eventBus.publish(TelemetryEvent.RequestRecordMainMenuOpened)
    }
}
