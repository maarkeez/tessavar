# How to record a clip of the game for a post

> **Summary:** Record a GIF and an MP4 of the game played by a script, e.g.: for a post on Reddit, Bluesky, X or
> YouTube Shorts. A clip is a test under `src/jvmTest/kotlin/com/mkz/tessavar/shared/adapters/clip`, left out of
> `jvmTest` and recorded by `./gradlew recordClips` into `build/clips`. The clips are committed, so they can be recorded
> again after the game changes.

## Prerequisites

- A machine with OpenGL, e.g.: a macOS or desktop development machine. The headless CI runners are not supported.
- An end-to-end test flow to start from, see [End-to-end test](how-to-add-a-new-test.md#test-example---end-to-end-test).
  The clip plays the game through the same page objects, e.g.: `clickTile` or `clickArrow`.

## Steps

### 1. Create the clip class

Create a class named `<What it shows>Clip` under `src/jvmTest/kotlin/com/mkz/tessavar/shared/adapters/clip`, extending
`OpenGlViewsForTesting`, which renders the frames with OpenGL at the game's window size, 390x844:

```kotlin
class ExploreClip : OpenGlViewsForTesting() {
    @Test
    fun `should record the leader walking`() =
        sharedViewsTest {
            views.clearColor = Colors["#2b2b2b"]
            startNewGame()
        }
}
```

The class name must end with `Clip`: `jvmTest` leaves out the classes of that package ending with `Clip`, and
`recordClips` runs only them.

Set `views.clearColor` to the game's background, or the clip is recorded on black.

### 2. Play the clip inside `recordClip`

Set up what the clip starts from, then play what it shows inside `views.recordClip`, named after the files it writes:

```kotlin
startNewGame()
views.recordClip("explore", frameTime) {
    waitFor(400.milliseconds)
    clickArrow(requireNotNull(virtualPadView()), Direction.EAST)
    waitUntil { battlefieldView()!!.hasBattleUnitAt(KNIGHT_START_ROW, KNIGHT_START_COLUMN + 1) }
    waitFor(1.seconds)
}
```

- Only what happens inside `recordClip` is recorded, so the consent and the menu stay out of the clip.
- The clip lasts as long as the game time the script plays, at 10 frames per second. Hold what the viewer must read
  with `waitFor`, e.g.: 400 ms after each tap, and 1 s at the end before the clip loops.
- Every tap is marked with a ring growing for 400 ms where it lands, drawn on the frames only.
- To start from a world of your own in the world builder, keep its world file under
  `NativeStorageWorldBuilderRepository.KEY` in an `InmemoryStorage`, and pass it to `startWorldBuilder`, as
  `WorldBuilderClip` does.

### 3. Choose the formats

By default, `recordClip` writes a GIF at x1, 390x844, and an MP4 at x2, 780x1688, in the middle of a 1080x1920 canvas
filled with `views.clearColor`, as YouTube Shorts and Buffer take videos between 9:16 and 1:1 only. Pass `formats` to
choose others, e.g.: only a GIF at x2:

```kotlin
views.recordClip("explore", frameTime, formats = listOf(ClipFormat.Gif(scale = 2))) { ... }
```

Or an MP4 at x2 without the canvas, at the game's 9:19.5:

```kotlin
views.recordClip("explore", frameTime, formats = listOf(ClipFormat.Mp4(canvas = null))) { ... }
```

### 4. Save a still (optional)

Call `views.saveStill` at the step to capture, to write a PNG at x2, e.g.: the world builder with its menu open:

```kotlin
views.saveStill("world-builder-menu")
```

### 5. Record the clips

```shell
./gradlew recordClips
```

To record one clip only:

```shell
./gradlew recordClips --tests '*ExploreClip*'
```

## Verify

Open `build/clips`. It holds, per clip, its `<name>.gif` and `<name>.mp4`, and every still saved, e.g.:
`world-builder.gif`, `world-builder.mp4` and `world-builder-menu.png` for `WorldBuilderClip`. The GIF loops forever, the
taps are ringed, and the frames that do not change are merged into one, so a hold costs no size.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| The clip is recorded on black | `views.clearColor` is not set | Set it to `Colors["#2b2b2b"]` before starting the game, as in step 1 |
| `jvmTest` runs the clip and fails on CI | The class name does not end with `Clip` | Rename it, e.g.: `ExploreClip` |
| A single tile of terrain does not show | The terrain is drawn from the corners of the tiles, so a tile alone in another terrain draws nothing | Paint at least 2x2 tiles of it |
| A tap misses its tile | The tile is out of the battlefield's 6x6 tiles on screen | Scroll to it first with `dragBattlefield` |
| Buffer refuses the MP4: "Aspect ratio must be between 9:16 (vertical) and 1:1 (square)" | The MP4 was recorded without its canvas, at the game's 9:19.5 | Keep the default `ClipFormat.Mp4()`, which pads it to 1080x1920 |
| The GIF is too big for a post | The clip is long, or scaled up | Shorten the holds, or record the GIF at x1 |

## See also

- [How to take a snapshot of the game during an end-to-end test](how-to-take-a-game-snapshot.md), for a single PNG of a
  step.
- `WorldBuilderClip`, the clip of the world builder's post.
