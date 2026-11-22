# Effect interactions are composed from single-outcome effects

Soak, burn, venom and stun interact, and we keep every effect to one outcome and one timing, so effects stay building
blocks: an interaction is an extra effect an ability carries, whose outcome is apply if ongoing (apply another effect
only if the recipient has a given ongoing effect) or resist (lessen the damage of a given effect, while ongoing). The
rules name effect ids, so there is no "poisoned" or "soaked" condition, and a second venom must be added to every rule
that names the first.

## Considered Options

- **Conditions**: a named state, such as poisoned, that several effects give, with interactions written against it.
  Rejected for now: a new concept for a handful of rules.
- **Reactions on the ongoing effect**: soak carrying "when I am about to receive burn, halve it". Rejected as it gives an
  effect more than one part, and venom could not hold a reaction next to its own timing. The unused before-applying-effect
  timing, an early try at this, is deleted.
- **Apply unless ongoing** and **damage conditioned on the recipient**: rejected for "soaked halves burn", as every burn
  source would carry the rule, and soak would not say what it does.
