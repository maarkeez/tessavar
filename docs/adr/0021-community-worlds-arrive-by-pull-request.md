# Community worlds arrive by pull request

An author shares a world by opening a pull request that adds its world file to `resources/world/`, which GitHub's web
upload does without git. CI checks that every world file is valid and playable, the maintainer reviews it, and once
merged the world ships in the resources bundle of the next deploy, listed under Community worlds for every player. We
chose this over an online service where players upload and download worlds, and over players passing world files to one
another, so there is no server to run or moderate, nothing reaches players without a review, and the author is credited
in the game and in the history of the repository.

## Consequences

- World files are licensed CC BY-SA 4.0, like the art, and so is the campaign's: the pull request template asks the
  author to agree to it.
- A world reaches players only on a deploy, never the moment its author finishes it.
- Every world version bump updates every world file in the repository in the same commit, as world files are not
  migrated when read.
