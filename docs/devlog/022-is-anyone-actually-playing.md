---
date: 17th November 2026
commit: 7660995852c9ffc9f4f5f11a23eb8daa6e13ae5f
---

# 022 — Is Anyone Actually Playing?

## Hook

The game had a new name, Tessavar, a page on GitHub Pages, and a Wishlist button. It was time to share it.

The plan: a post every week on Bluesky, X, YouTube Shorts and Reddit.

Then a simple question stopped me.

> Is anyone actually playing?

I had no way to know. I could post for months and learn nothing.

**Before I share the game anywhere, I need to know if anyone plays it, and I want to learn that without taking more
than I need.**

## Context

[The architecture redesign](016-architecture-redesign.md) split the game into subdomains: `battle`, `inventory`,
`quest`, `party` and many more. Telemetry became one more of them, the last piece before going public.

The goal was a handful of questions:

- How many players start a new game?
- How many finish Bats in the Meadow, and how many abandon it?
- How many battles end in a game over?
- Which link brought them?

The scope, on purpose:

- **No charts or dashboards.** Plain SQL, once a month.
- **No counting the players who decline.** They said no.
- **No erasing by install id, no game version** on the events. Later, if ever.

## Main Story — Counting Without Taking

### Ask first

Telemetry listens to the domain events the other subdomains already publish. A `QuestCompleted` for
`bats-in-the-meadow` becomes a Quest completed product event. 13 of them in total. The rules of the game never change
for it.

But nothing is recorded until the player says yes.

At the first launch, a panel sits over the dimmed main menu, with Accept and Decline of equal weight. I prototyped three
versions: a strip along the bottom, which most players would ignore, and a screen of its own, which felt like a gate
before the game. The panel won.

No install id is written on the device before Accept, as EU law asks.

### Where do the events live?

This was the real decision. The game is a free pet project, so it had to cost nothing. I looked at five options:

- **PostHog Cloud (EU)**: funnels and SQL for free, but it keeps every event for a fixed year.
- **Aptabase**: made for games, but its free plan stops at 20,000 events a month, about 500 sessions.
- **Supabase**: Postgres, but a free project is paused after 7 days without activity.
- **Workers Analytics Engine**: deletes after 3 months by itself, but no joins, and it can't be kept in the EU.
- **Self-hosting** Umami or ClickHouse: free, but a server to keep running and patched.

**The free tiers decided more than the features did.**

So I wrote my own telemetry collector: a Cloudflare Worker in TypeScript, on the free plan, keeping the events in a D1
database in the EU, for 3 months. A nightly run deletes everything older. It never keeps the IP address, the browser or
the country.

Bonus: if I ever change the collector, the old one empties itself. Nothing to migrate.

### The path of one event

```text
QuestCompleted (domain event)
  → telemetry listener
  → Telemetry, only with consent: stamps install id, session id, game id, referral
  → queued on the device (at most 1,000, oldest dropped first)
  → sent in batches of at most 100
  → telemetry collector → D1, deleted after 90 days
```

The game never waits on the network. If the collector is down, the batch stays queued for later. If the answer is
lost, the rows already kept are ignored.

### Which link?

The last piece was the channel. A link like `?ref=bluesky` gives the session a referral, and every product event of
that session carries it. The game removes it from the address bar once read, so a bookmark doesn't count as Bluesky
forever. Anything that isn't a lowercase word of up to 32 letters, digits and hyphens is dropped.

### The tradeoffs

- **Players who decline are invisible.** Every number undercounts.
- **Anyone can forge events.** The address is public. The collector checks the shape, the origin and a rate limit, and
  that's it. Forging them gains nobody anything.
- **Every change asks everyone again.** The privacy notice reached version 3 in three days. Each new field means every
  player sees the consent panel again.

### Still unfinished

There is no data worth reading yet. No charts either, just SQL.

And a first visitor's Main menu opened is never recorded. They accept after the menu has already opened.

## Key Takeaways

- **Measure before you market.** Without counting, a weekly post is just noise you can't learn from.
- **Ask first, and ask again.** Store nothing until the player accepts, and every new field costs a new consent.
- **Keep the least data for the shortest time.** A 3-month retention also makes changing the collector free.
- **Free tiers hide their real limits.** A fixed year, 20,000 events or a 7-day pause decided more than features did.
- **Listen, don't instrument.** Telemetry reads the events already on the bus, so the game never changes for it.

## What's Next

Now the posting starts. Every week, Bluesky, X, YouTube Shorts and Reddit, each with its own `?ref=`.

Once a month, one SQL query: which channel brings players, and which brings players who finish Bats in the Meadow.

Then change what doesn't work, and post again.

Visibility is a loop, not a launch: post, measure, change, post again.
