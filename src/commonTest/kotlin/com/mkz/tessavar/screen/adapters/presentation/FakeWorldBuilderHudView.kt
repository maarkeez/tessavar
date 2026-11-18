package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderHudView : WorldBuilderHudView(MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    var displayedTerrains: List<TerrainOption> = emptyList()
        private set
    var selectedTerrainId: String? = null
        private set
    var displayedTool: Tool? = null
        private set
    var displayedTabs = mutableListOf<Tab>()
        private set
    var displayedBuildings: List<PaletteOption> = emptyList()
        private set
    var displayedUnitRows: List<UnitRow> = emptyList()
        private set
    var displayedNpcs: List<PaletteOption> = emptyList()
        private set
    var selectedBuildingId: String? = null
        private set
    var selectedUnit: Pair<String, String>? = null
        private set
    var selectedNpcId: String? = null
        private set
    var isStockEnabled: Boolean? = null
        private set

    override fun displayTab(tab: Tab) {
        displayedTabs += tab
    }

    override fun displayBuildings(buildings: List<PaletteOption>) {
        displayedBuildings = buildings
    }

    override fun displayUnits(rows: List<UnitRow>) {
        displayedUnitRows = rows
    }

    override fun displayNpcs(npcs: List<PaletteOption>) {
        displayedNpcs = npcs
    }

    override fun selectBuilding(buildingId: String) {
        selectedBuildingId = buildingId
    }

    override fun selectUnit(
        playerId: String,
        unitId: String,
    ) {
        selectedUnit = playerId to unitId
    }

    override fun selectNpc(npcId: String) {
        selectedNpcId = npcId
    }

    override fun displayStock(isEnabled: Boolean) {
        isStockEnabled = isEnabled
    }

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun displayTerrains(
        terrains: List<TerrainOption>,
        selectedTerrainId: String,
    ) {
        displayedTerrains = terrains
        this.selectedTerrainId = selectedTerrainId
    }

    override fun selectTool(tool: Tool) {
        displayedTool = tool
    }

    override fun selectTerrain(terrainId: String) {
        selectedTerrainId = terrainId
    }
}
