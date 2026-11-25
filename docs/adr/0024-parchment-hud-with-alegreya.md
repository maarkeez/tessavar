# The HUD is parchment with dark ink, in Alegreya

The chrome of every screen is light parchment with dark ink: cream panels, brown buttons and a deep red accent, the
colours in `HudStyle`, with every text colour at a contrast of at least 4.5:1 on its panel, checked by `HudStyleTest`.
Titles, names, captions and buttons are in Alegreya SC Bold, and running text (the talk line, the consent and privacy
text, quest descriptions, the effect and ability info) in Alegreya Medium, both under the SIL Open Font License 1.1,
turned into bitmap fonts by `HudFonts` before the first scene. Every text uses the type scale of `HudStyle` (`CAPTION`
12, `BODY` 15, `NAME` 18, `TITLE` 22, `DISPLAY` 40), and none is under 12 px, checked by `TextSizeArchitectureTest`.
There is one look, with no theme setting. We chose this so the HUD looks like a game and reads on a phone, at the cost
of a light HUD around the battlefield's black void, and no dark mode.

## Considered Options

- **Tavern** (warm mid-tone wood and brass, a pixel font) and **Arcade** (saturated teal and violet, a rounded display
  font): compared with Parchment on a prototype, and rejected as less readable and less fitting.
- **A theme setting**, keeping the prototype's `UiTheme` switch: rejected, as a second look doubles every screenshot to
  check, and nobody asked to pick one.
- **Alegreya SC everywhere**: rejected, as all small caps made long text slow to read.
- **Drawing the font bigger than asked** (the prototype's 1.2×), keeping each view's sizes: rejected for a type scale,
  as the layouts stayed at the old sizes and quest lines and long names got cramped.
