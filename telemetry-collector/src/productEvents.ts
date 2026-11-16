// The batch of product events the game sends, as `src/jvmTest/fixtures/telemetry/product-events.json` holds it.

export const MAX_BATCH_SIZE = 100;

const MAX_TEXT_LENGTH = 64;

export const PRODUCT_EVENT_NAMES = [
  "MAIN_MENU_OPENED",
  "GAME_STARTED",
  "GAME_LOADED",
  "GAME_SAVED",
  "QUEST_STARTED",
  "QUEST_COMPLETED",
  "QUEST_ABANDONED",
  "BATTLE_STARTED",
  "BATTLE_FINISHED",
  "GAME_OVER",
  "ITEM_BOUGHT",
  "ITEM_SOLD",
  "MEMBER_RECRUITED",
] as const;

export type ProductEventName = (typeof PRODUCT_EVENT_NAMES)[number];

export interface ProductEvent {
  name: ProductEventName;
  installId: string;
  sessionId: string;
  gameId: string | null;
  occurredAt: number;
  questId: string | null;
  slot: number | null;
  lootableItemId: string | null;
  price: number | null;
  unitId: string | null;
  referral: string | null;
  worldId: string | null;
}

export type ParsedBatch = { productEvents: ProductEvent[] } | { rejected: string };

const FIELDS = [
  "name",
  "installId",
  "sessionId",
  "gameId",
  "occurredAt",
  "questId",
  "slot",
  "lootableItemId",
  "price",
  "unitId",
] as const satisfies readonly (keyof ProductEvent)[];

// Fields a product event may leave out, read as null: the game sent no referral before the privacy notice version 3,
// and no world id before version 4, and sends one only on a game started
const OPTIONAL_FIELDS = ["referral", "worldId"] as const satisfies readonly (keyof ProductEvent)[];

const ID = /^[0-9a-f]{32}$/;

/** The name of the link the session was opened from, as the game's `Referral` checks it. */
export const REFERRAL = /^[a-z0-9-]{1,32}$/;

/** The id of the world a game started is played in, as the game's `WorldId` checks it. */
export const WORLD_ID = /^[a-z0-9-]{1,64}$/;

/** Returns the product events of a batch, or why it is rejected. */
export function parseBatch(body: unknown): ParsedBatch {
  if (!isObject(body) || !hasExactly(body, ["productEvents"])) return { rejected: "the batch is not {\"productEvents\": [...]}" };
  const productEvents = body.productEvents;
  if (!Array.isArray(productEvents)) return { rejected: "productEvents is not a list" };
  if (productEvents.length === 0) return { rejected: "the batch is empty" };
  if (productEvents.length > MAX_BATCH_SIZE) return { rejected: `the batch holds more than ${MAX_BATCH_SIZE} product events` };
  const parsed: ProductEvent[] = [];
  for (const [index, productEvent] of productEvents.entries()) {
    const reason = rejectionOf(productEvent);
    if (reason !== null) return { rejected: `product event ${index}: ${reason}` };
    const { referral, worldId } = productEvent as Partial<ProductEvent>;
    parsed.push({ ...(productEvent as ProductEvent), referral: referral ?? null, worldId: worldId ?? null });
  }
  return { productEvents: parsed };
}

function rejectionOf(productEvent: unknown): string | null {
  if (!isObject(productEvent) || !hasExactly(productEvent, FIELDS, OPTIONAL_FIELDS)) {
    return `its fields are not ${FIELDS.join(", ")}, with ${OPTIONAL_FIELDS.join(", ")} optional`;
  }
  const { name, installId, sessionId, gameId, occurredAt, questId, slot, lootableItemId, price, unitId, referral, worldId } = productEvent;
  if (!PRODUCT_EVENT_NAMES.includes(name as ProductEventName)) return "unknown name";
  if (!isId(installId)) return "installId is not 32 lowercase hex digits";
  if (!isId(sessionId)) return "sessionId is not 32 lowercase hex digits";
  if (gameId !== null && !isId(gameId)) return "gameId is neither null nor 32 lowercase hex digits";
  if (typeof occurredAt !== "number" || !Number.isFinite(occurredAt) || occurredAt < 0) return "occurredAt is not a time";
  if (!isTextOrNull(questId)) return "questId is not a short text or null";
  if (!isIntegerOrNull(slot)) return "slot is not a whole number or null";
  if (!isTextOrNull(lootableItemId)) return "lootableItemId is not a short text or null";
  if (!isIntegerOrNull(price)) return "price is not a whole number or null";
  if (!isTextOrNull(unitId)) return "unitId is not a short text or null";
  if (!isReferralOrAbsent(referral)) return "referral is not a lowercase word of at most 32 characters";
  if (!isWorldIdOrAbsent(worldId)) return "worldId is not lowercase words of at most 64 characters";
  return null;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function hasExactly(value: Record<string, unknown>, fields: readonly string[], optionalFields: readonly string[] = []): boolean {
  const keys = Object.keys(value);
  return (
    fields.every((field) => Object.hasOwn(value, field)) &&
    keys.every((key) => fields.includes(key) || optionalFields.includes(key))
  );
}

function isId(value: unknown): value is string {
  return typeof value === "string" && ID.test(value);
}

function isTextOrNull(value: unknown): boolean {
  return value === null || (typeof value === "string" && value.length > 0 && value.length <= MAX_TEXT_LENGTH);
}

function isReferralOrAbsent(value: unknown): boolean {
  return value === undefined || value === null || (typeof value === "string" && REFERRAL.test(value));
}

function isWorldIdOrAbsent(value: unknown): boolean {
  return value === undefined || value === null || (typeof value === "string" && WORLD_ID.test(value));
}

function isIntegerOrNull(value: unknown): boolean {
  return value === null || Number.isSafeInteger(value);
}
