package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.localization.adapters.presentation.LocalizationApi
import com.mkz.tessavar.screen.adapters.presentation.HudStyle.panel
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
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size

class WorldBuilderScene(
    private val localizationApi: LocalizationApi,
    private val telemetryApi: TelemetryApi,
    private val storage: IStorage? = null,
) : Scene() {
    companion object {
        const val MENU_MARGIN = 6.0
        const val TOP_STRIP_HEIGHT = 52.0
        const val TOP_STRIP_ALPHA = 0.9
        const val PADDING = 12.0
        const val INFO_Y = 9.0
        const val PANEL_SHEET_HEIGHT = 360.0
        const val PANEL_SHEET_TOP = 26.0
        const val SHEET_RADIUS = 16.0
        const val GRABBER_WIDTH = 40.0
        const val GRABBER_HEIGHT = 5.0
        const val GRABBER_TOP = 8.0

        val SCROLL_MARGIN = Margin(top = TOP_STRIP_HEIGHT, right = 0.0, bottom = WorldBuilderHudView.BOTTOM_CLEARANCE, left = 0.0)
    }

    override val coroutineContext by lazy { super.coroutineContext + views.gameWindow.coroutineDispatcher }

    private val messages = Messages(localizationApi)
    private val resourcesLoader = GameSceneResourcesLoader()
    val battlefieldView = BattlefieldView(resourcesLoader, viewportSize = GAME_WINDOW_SIZE, scrollMargin = SCROLL_MARGIN)
    val infoView = WorldBuilderInfoView(messages)
    val hudView = WorldBuilderHudView(messages)
    val resizeView = WorldBuilderResizeView(messages)
    val detailsView = WorldBuilderDetailsView(messages)
    val stockView = WorldBuilderStockView(messages)
    val formView = WorldBuilderFormView(messages)
    val menuView = WorldBuilderMenuView()

    override suspend fun SContainer.sceneInit() {
        resourcesLoader.load()
        hudView.displayToolIcons(WorldBuilderHudView.toolIcons(resourcesLoader))
        menuView.displayHistoryIcons(WorldBuilderMenuView.historyIcons(resourcesLoader))
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

        addChild(battlefieldView)
        addChild(hudView)
        val panelSheet = panelSheet()
        addUpdater {
            val isPanelOpen = resizeView.isOpen || detailsView.isOpen || stockView.isOpen || formView.isOpen
            panelSheet.visible = isPanelOpen
            hudView.visible = !isPanelOpen
            if (hudView.isCardOpen && (isPanelOpen || menuView.isOpen)) hudView.closeCard()
        }
        topStrip()

        menuView.x = GAME_WINDOW_SIZE.width - WorldBuilderMenuView.BUTTON_WIDTH - MENU_MARGIN
        menuView.y = MENU_MARGIN
        addChild(menuView)

        worldBuilderApi.openWorld()
    }

    private fun Container.topStrip() {
        solidRect(Size(GAME_WINDOW_SIZE.width, TOP_STRIP_HEIGHT), HudStyle.PANEL.withAd(TOP_STRIP_ALPHA))
        addChild(infoView)
        infoView.xy(PADDING, INFO_Y)
    }

    private fun Container.panelSheet(): Container =
        container {
            y = GAME_WINDOW_SIZE.height - PANEL_SHEET_HEIGHT
            visible = false
            panel(Size(GAME_WINDOW_SIZE.width, PANEL_SHEET_HEIGHT + SHEET_RADIUS), radius = SHEET_RADIUS)
            panel(Size(GRABBER_WIDTH, GRABBER_HEIGHT), fill = HudStyle.FAINT, radius = GRABBER_HEIGHT / 2, stroke = HudStyle.FAINT)
                .xy((GAME_WINDOW_SIZE.width - GRABBER_WIDTH) / 2, GRABBER_TOP)
            container {
                xy(PADDING, PANEL_SHEET_TOP)
                addChild(resizeView)
                addChild(detailsView)
                addChild(stockView)
                addChild(formView)
            }
        }
}
