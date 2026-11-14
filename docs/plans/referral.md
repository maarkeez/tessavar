# Plan: every product event of a session carries the referral of the link it was opened from

> **Status:** to do.
>
> **Summary:** Today no product event says how a player found the game: e.g.: someone taps the game's link on Bluesky,
> opens the main menu on GitHub Pages, accepts the privacy notice and starts a new game, and the telemetry collector
> receives a `GAME_STARTED` row that looks the same as one from a player who typed the address. Their
> `MAIN_MENU_OPENED` is not even recorded, as `Telemetry.mainMenuOpened` only records once consent is given, and a first
> visitor gives it after the menu opened. So the weekly posts on Bluesky, X, YouTube Shorts and Reddit can't be told
> apart. Instead, a link such as `https://maarkeez.github.io/tessavar/?ref=bluesky` gives the session the referral
> `bluesky`, the game removes `?ref=` from the address bar, and every product event of that session carries it, from
> `GAME_STARTED` onwards for that first visitor; the telemetry collector keeps it in a new column, and the privacy notice
> version 3 says so. It builds on the glossary's **Referral** and **Session**, on the [telemetry plan](telemetry.md) and
> the [telemetry collector plan](telemetry-collector.md), and on
> [telemetry listening and recording only with consent](../adr/0017-telemetry-listens-and-records-only-with-consent.md)
> and [product events kept by our own telemetry collector on Cloudflare](../adr/0018-product-events-kept-by-our-own-telemetry-collector-on-cloudflare.md).

## Decisions

| Topic           | Decision                                                                                                                                                                                                                                                                                                                                        |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope           | Reading the referral from the link, keeping it in the session, stamping it on every product event, the telemetry collector keeping it, and the privacy notice version 3. E.g.: `?ref=reddit-playmygame`, then a quest completed, lands in D1 as a `QUEST_COMPLETED` row with `referral` `reddit-playmygame`.                                    |
| What it carries | Every product event of the session. Chosen over `MAIN_MENU_OPENED` only, as a first visitor accepts the consent after the main menu opened, so the players a link brings would never be counted.                                                                                                                                                |
| Valid values    | A word of 1 to 32 lowercase letters, digits and hyphens, `[a-z0-9-]{1,32}`: e.g.: `bluesky`, `x`, `youtube`, `reddit-webgames`. Anything else, e.g. `?ref=Hello%20World`, gives no referral. Chosen over a fixed list in the game, as a new subreddit then needs no release, and no email address or other personal data fits the rule.         |
| Address bar     | The game removes `ref` from the address once read, valid or not, keeping the other parameters and the hash, so a bookmark or a reload starts a session with no referral. Chosen over leaving it, as a bookmarked `?ref=bluesky` would count every later visit as Bluesky.                                                                       |
| No link         | A session opened without `?ref=`, from the bare address, a bookmark or the desktop build, has no referral: `null`, with no `direct` value.                                                                                                                                                                                                      |
| Saves           | The referral belongs to the session, not to a game: it is never written in a save, and loading a game keeps the referral of the session that loads it.                                                                                                                                                                                          |
| Collector       | A nullable `referral` column, checked with the same rule: a product event whose referral breaks it is rejected with 400, so a forged request can't store free text. A product event may leave the field out, so the game of today keeps sending until it is updated.                                                                            |
| Privacy notice  | Its "With it:" paragraph adds "and the name of the link you opened the game from, such as bluesky", in `messages.pot` and `es.po`. `PRIVACY_NOTICE_VERSION` goes from 2 to 3: every player is asked again, and the product events still queued are deleted, as [ADR 0017](../adr/0017-telemetry-listens-and-records-only-with-consent.md) says. |
| Architecture    | `Referral` is a value object in `telemetry.domain`; `Telemetry.startSession` takes it. Reading and removing `?ref=` is `telemetry.adapters.location`, an `expect fun takeLinkReferral(): String?`, read from `window.location` on JS and `null` on the JVM. `startApp` passes it to `TelemetryApi`.                                             |
| Storybook       | **Screen / Privacy notice › Notice** shows the version 3 text; no story is added.                                                                                                                                                                                                                                                               |
| Tests           | Domain tests of `Referral` and `Telemetry`, use case tests of `StartSession`, an acceptance test of the first visit, a common test of the link query, the sample batch with referrals, and Vitest tests of the telemetry collector.                                                                                                             |
| Out of scope    | Counting players who decline the consent, the browser's `Referer` header, a referral kept for an install across sessions, and the clip recorder and GitHub Discussions of the marketing plan.                                                                                                                                                   |
| Delivery        | 2 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                  |

For example, a player taps `https://maarkeez.github.io/tessavar/?ref=bluesky` for the first time:

| Step                                     | Address bar                            | Recorded                                                |
|------------------------------------------|----------------------------------------|---------------------------------------------------------|
| The game launches                        | `https://maarkeez.github.io/tessavar/` | Nothing: the session starts with the referral `bluesky` |
| The main menu opens, the notice is shown | `https://maarkeez.github.io/tessavar/` | Nothing: consent is not given yet                       |
| The player accepts and starts a new game | `https://maarkeez.github.io/tessavar/` | `GAME_STARTED`, `referral` `bluesky`                    |
| They recruit the herbalist               | `https://maarkeez.github.io/tessavar/` | `MEMBER_RECRUITED`, `referral` `bluesky`                |
| A week later, from their bookmark        | `https://maarkeez.github.io/tessavar/` | `MAIN_MENU_OPENED`, `referral` `null`                   |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone              | Done when                                                                                                                                                          | Status | Done in |
|----|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | The collector keeps it | The telemetry collector deployed by `telemetry-collector.yml` keeps a product event's referral, rejects an invalid one, and still keeps product events without it. | ⬜      |         |
| M2 | The game sends it      | The game on GitHub Pages stamps every product event of a session opened with `?ref=` with its referral, and the privacy notice version 3 screenshots are approved. | ⬜      |         |

### M1 commits

1. `0002_add_referral_to_product_events.sql` adds the nullable `referral` column; `parseBatch` accepts a product event with or without `referral`, checks it against `REFERRAL`, and `index.ts` inserts it; the Vitest tests below.
2. `telemetry-collector/README.md` gains the query of the monthly review, counting sessions and installs per referral.

### M2 commits

1. `Referral` and `Telemetry.startSession(sessionId, referral)`, stamping `ProductEventRecorded.referral`; `ProductEventJson` writes it; the sample batch `product-events.json` is written again with referrals.
2. `ReferralQuery` and `takeLinkReferral()` on JS and the JVM, passed by `startApp` to `TelemetryApi`; their tests and the acceptance test.
3. The privacy notice version 3 text in `messages.pot` and `es.po`, and `PRIVACY_NOTICE_VERSION` 3, with screenshots of **Screen / Privacy notice › Notice** in English and Spanish approved.

## Reference

### telemetry-collector

- `migrations/0002_add_referral_to_product_events.sql` runs `ALTER TABLE product_events ADD COLUMN referral TEXT`, with a comment saying it is the referral of the session, never free text.
- `productEvents.ts`: `ProductEvent` gains `referral: string | null`; `REFERRAL` is `/^[a-z0-9-]{1,32}$/`. `parseBatch` accepts the 10 fields of today with `referral` optional, a missing one read as `null`, and `rejectionOf` answers "referral is not a lowercase word of at most 32 characters" when it is neither `null` nor matches `REFERRAL`.
- `index.ts` inserts `referral` with the other fields.
- `README.md` gains the query of the monthly review:

  ```sql
  SELECT referral, COUNT(DISTINCT session_id) AS sessions, COUNT(DISTINCT install_id) AS installs
  FROM product_events
  GROUP BY referral
  ORDER BY sessions DESC;
  ```

### telemetry.domain

- `Referral` is a value object: `Referral.of(text)` returns one when `text` matches `[a-z0-9-]{1,32}`, else `null`. E.g.: `Referral.of("bluesky")` is one, `Referral.of("Bluesky")`, `Referral.of("")` and `Referral.of("a".repeat(33))` are `null`.
- `Telemetry` keeps a `referral: Referral?` next to its `sessionId`, in `Telemetry.Dto` too, so it is stored on the device with the session.
- `Telemetry.startSession(sessionId, referral)` replaces both: a new launch never keeps the referral of the one before.
- `ProductEventRecorded` gains `referral: String?`, which `Telemetry.record` stamps on every product event, as it does the session id.
- `Telemetry.PRIVACY_NOTICE_VERSION` is 3.

### telemetry.usecases

- `StartSession(sessionId, referral)` passes the raw text through `Referral.of`, so an invalid one starts a session with no referral.

### telemetry.adapters

- `location/ReferralQuery.kt`: `ReferralQuery.take(search)` returns the value of `ref` and the query without it, keeping the other parameters in their order, as the Storybook's `LocationQuery` does for its own. E.g.: `?ref=bluesky&lang=es` gives `bluesky` and `?lang=es`; `?lang=es` gives `null` and `?lang=es`.
- `location/PlatformReferral.kt`: `expect fun takeLinkReferral(): String?`.
- `jsMain`: `PlatformReferral.js.kt` applies `ReferralQuery.take` to `window.location.search`, then, when `ref` was there, writes the rest back with `window.history.replaceState`, keeping `window.location.pathname` and `window.location.hash`. E.g.: `/tessavar/?ref=bluesky&lang=es#top` becomes `/tessavar/?lang=es#top`.
- `jvmMain`: `PlatformReferral.jvm.kt` returns `null`.
- `TelemetryApi` takes `referral: String? = null` and starts its session with it, so the tests and the Storybook build it as today.
- `ProductEventJson`: `ProductEventFile` gains `referral: String? = null`, written in the queue and in every batch.

### Main.kt

- `startApp` builds `TelemetryApi` with `referral = takeLinkReferral()`, before the menu scene opens. The Storybook never calls it, so its own query is left alone.

### Localization

- `ui.privacyNotice.body`, "With it:" paragraph:
  - English: "With it: when it happened, random ids of this copy of the game, of this launch and of this game, and the name of the link you opened the game from, such as bluesky. Never your name, nor anything else about you."
  - Spanish: "Con ello: cuándo ocurrió, identificadores aleatorios de esta copia del juego, de esta sesión y de esta partida, y el nombre del enlace desde el que abriste el juego, como bluesky. Nunca tu nombre ni nada más sobre ti."

### Tests

- `ReferralTest`
  - `Referral should be a lowercase word of letters, digits and hyphens when the link names one`
  - `Referral should be absent when the link holds capitals, spaces or more than 32 characters`
- `TelemetryTest`
  - `Telemetry should stamp every product event with the referral of the session when the session was opened from a link`
  - `Telemetry should add product events with no referral when the session was opened without a link`
- `StartSessionTest`
  - `StartSession should start the session with the referral of the link it was opened from`
  - `StartSession should start the session with no referral when the link holds an invalid one`
  - `StartSession should not keep the referral of an earlier launch`
- `TelemetryAcceptanceTest`
  - `A first visit from a Bluesky link should send its game started with the referral when consent is given after the main menu opened`
- `ReferralQueryTest`
  - `ReferralQuery should return the referral and the query without it, keeping the other parameters, when the link has one`
  - `ReferralQuery should return no referral and the same query when the link has none`
  - `ReferralQuery should return no query when ref was its only parameter`
- `ProductEventBatchFixtureTest`
  - `The sample batch should be what the game sends for the 13 product events` now writes them with referrals.
- `collector.test.ts`
  - `the telemetry collector should keep the referral of a product event`
  - `the telemetry collector should keep a product event without a referral`
  - `the telemetry collector should reject a product event whose referral is not a lowercase word`

## Known limitations

- Players who decline the consent are never counted, so the numbers per referral are a floor, not a total.
- A referral counts sessions, not players: a player who opens the game from Bluesky and later from Reddit counts once for each.
- Anyone can open the game with any valid `?ref=`, so a referral says which link was typed, not that the player really came from that site.
- The privacy notice version 3 asks every player again, and deletes the product events still queued when it does.
- The privacy notice's panel was made 640 tall for the version 2 text in Spanish; the longer version 3 text may need it taller, which the screenshots of M2 show.
