package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.localization.adapters.presentation.LocalizationApi
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.telemetry.adapters.presentation.TelemetryApi
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import com.mkz.tessavar.worldBuilder.adapters.resources.ResourcesCatalog
import korlibs.io.async.launchImmediately
import korlibs.korge.scene.Scene
import korlibs.korge.service.storage.IStorage
import korlibs.korge.service.storage.storage
import korlibs.korge.ui.uiSpacing
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.math.geom.Size

class WorldBuilderScene(
    private val localizationApi: LocalizationApi,
    private val telemetryApi: TelemetryApi,
    private val storage: IStorage? = null,
) : Scene() {
    private companion object {
        const val MENU_MARGIN = 6.0
    }

    override val coroutineContext by lazy { super.coroutineContext + views.gameWindow.coroutineDispatcher }

    private val messages = Messages(localizationApi)
    private val resourcesLoader = GameSceneResourcesLoader()
    val battlefieldView = BattlefieldView(resourcesLoader)
    val infoView = WorldBuilderInfoView(messages)
    val hudView = WorldBuilderHudView(messages)
    val resizeView = WorldBuilderResizeView(messages)
    val detailsView = WorldBuilderDetailsView(messages)
    val stockView = WorldBuilderStockView(messages)
    val formView = WorldBuilderFormView(messages)
    val menuView = WorldBuilderMenuView()

    override suspend fun SContainer.sceneInit() {
        resourcesLoader.load()
    }

    override suspend fun SContainer.sceneMain() {
        val eventBus = InMemoryEventBus()
        addUpdater {
            eventBus.dispatch()
        }

        val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
        val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
        val worldBuilderApi = WorldBuilderApi(terrainApi, battlefieldApi, eventBus, storage ?: views.storage, ResourcesCatalog())
        terrainApi.init()
        terrainApi.initialiseTerrains()
        worldBuilderApi.init()

        WorldBuilderPresenter(
            battlefieldView = battlefieldView,
            infoView = infoView,
            hudView = hudView,
            resizeView = resizeView,
            detailsView = detailsView,
            stockView = stockView,
            formView = formView,
            menuView = menuView,
            worldBuilderApi = worldBuilderApi,
            battlefieldApi = battlefieldApi,
            terrainApi = terrainApi,
            messages = messages,
            worldFileSaver = GameWindowWorldFileSaver(views.gameWindow, messages),
            worldFilePicker = GameWindowWorldFilePicker(views.gameWindow, messages),
            textPrompt = if (needsTextPrompt()) GameWindowTextPrompt(views.gameWindow) else null,
            browser = GameWindowBrowser(views.gameWindow),
            coroutineScope = this@WorldBuilderScene,
            onLeft = { launchImmediately { sceneContainer.changeTo { MenuScene(localizationApi, telemetryApi, storage) } } },
            onPlaytest = { worldFile -> launchImmediately { sceneContainer.changeTo { GameScene(localizationApi, telemetryApi, storage, playtestWorldFile = worldFile) } } },
            eventBus = eventBus,
        )

        uiVerticalStack(padding = 2.0) {
            uiSpacing(Size(0, 10))
            addChild(infoView)
            addChild(battlefieldView)
            uiVerticalStack(padding = 5.0) {
                uiSpacing(Size(0, 10))
                container {
                    addChild(hudView)
                    addChild(resizeView)
                    addChild(detailsView)
                    addChild(stockView)
                    addChild(formView)
                }
            }
        }

        menuView.x = GAME_WINDOW_SIZE.width - WorldBuilderMenuView.BUTTON_WIDTH - MENU_MARGIN
        menuView.y = MENU_MARGIN
        addChild(menuView)

        worldBuilderApi.openWorld()
    }
}
