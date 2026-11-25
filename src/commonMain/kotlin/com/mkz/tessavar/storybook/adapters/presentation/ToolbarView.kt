package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.localization.domain.Localization.Dto.LanguageDto
import com.mkz.tessavar.shared.adapters.presentation.UiTheme
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.addTo
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.text

open class ToolbarView : Container() {
    companion object {
        const val HEIGHT = StorybookButton.HEIGHT
        private const val LABEL_WIDTH = 96.0
        private const val BUTTON_GAP = 6.0
        const val ROW_GAP = 6.0
        const val TOTAL_HEIGHT = 2 * HEIGHT + ROW_GAP
    }

    interface Delegate {
        fun languageSelected(languageCode: String)

        fun themeSelected(themeId: String) {}
    }

    private var delegate: Delegate? = null

    internal val languageLabel: Text = text("Language", textSize = 16.0, color = StoryInfoView.SECONDARY_COLOR).position(0.0, (HEIGHT - 16.0) / 2)
    internal val languages: Container = container().position(LABEL_WIDTH, 0.0)

    internal val themeLabel: Text = text("Theme", textSize = 16.0, color = StoryInfoView.SECONDARY_COLOR).position(0.0, HEIGHT + ROW_GAP + (HEIGHT - 16.0) / 2)
    internal val themes: Container = container().position(LABEL_WIDTH, HEIGHT + ROW_GAP)

    internal var languageButtons: Map<String, StorybookButton> = emptyMap()
        private set

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun display(
        languages: List<LanguageDto>,
        selectedLanguageCode: String,
    ) {
        this.languages.removeChildren()
        var x = 0.0
        languageButtons =
            languages.associate { language ->
                val size = StorybookButton.sizeFor(language.name)
                val button = StorybookButton(language.name, size) { delegate?.languageSelected(language.code) }.addTo(this.languages).position(x, 0.0)
                button.isSelected = language.code == selectedLanguageCode
                x += size.width + BUTTON_GAP
                language.code to button
            }
    }

    fun displayThemes(
        themes: List<UiTheme>,
        selectedThemeId: String,
    ) {
        this.themes.removeChildren()
        var x = 0.0
        themes.forEach { theme ->
            val size = StorybookButton.sizeFor(theme.name)
            val button = StorybookButton(theme.name, size) { delegate?.themeSelected(theme.id) }.addTo(this.themes).position(x, 0.0)
            button.isSelected = theme.id == selectedThemeId
            x += size.width + BUTTON_GAP
        }
    }
}
