-- The world a game started product event's game is played in, such as tessavar for the campaign, a community world
-- such as the-drowned-mill, or playtest from the world builder: lowercase words joined by hyphens, at most 64
-- characters, never free text. Null on every other product event, which reaches it through its game id.
ALTER TABLE product_events ADD COLUMN world_id TEXT;
