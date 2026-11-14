-- The referral of the session a product event belongs to: the name of the link the game was opened from, such as
-- bluesky, a lowercase word of at most 32 characters, never free text. Null when the session was opened without one.
ALTER TABLE product_events ADD COLUMN referral TEXT;
