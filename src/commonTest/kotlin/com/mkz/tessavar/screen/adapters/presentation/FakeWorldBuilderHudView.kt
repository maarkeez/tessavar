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
