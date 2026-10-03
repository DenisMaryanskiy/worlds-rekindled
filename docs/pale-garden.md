# Pale Garden: design

Status: agreed in brainstorming, not yet implemented.

## Problem

The vanilla Pale Garden (rare biome, the Creaking, resin) gives players a strong first impression
and no reason to come back. It is the hardest biome to find and the smallest payoff: only builders
benefit (pale oak wood, resin bricks). Explorers get no trophy or progression, warriors face a
puzzle with no preparation, miners get a decorative surface resource.

Goal: make it fit the gameplay loop with additions that feel like Mojang could have made them.
Rewards reuse the biome's own mechanic, **gaze**, and the Creaking Heart is the key ingredient.

Target loop:

> find biome → find the Pale Ruin (trophies, mask ingredient) → hunt Creakings (resin, Silk Touch
> heart) → craft Sentry, Pale Mask, sticky pistons → return for more resin and hearts

Priorities: single player first, multiplayer second. Progression tier: mid-game. Hearts come from
the natural spawn rate; nothing here needs to be crafted dozens of times.

### Vanilla facts this builds on

- A Creaking Heart drops itself only with Silk Touch; otherwise it drops 1–3 resin clumps.
- Hearts can also be crafted, but resin only comes from hitting a Creaking spawned by a *natural*
  heart, so the scarce input is natural hearts.
- A heart placed between two pale oak logs on the same axis spawns a hostile Creaking at night.
  The Sentry must be clearly distinct from this.

## Features

### 1. Resin sticky piston (first to implement)

- One recipe for both: vanilla's `sticky_piston` recipe is overridden so its top ingredient is the
  item tag `vanillareforged:sticky_piston_adhesives` (slime ball, resin clump), like planks accept
  any log. The recipe book shows a single entry cycling both ingredients and unlocks it when the
  player picks up either one.
- Data-only, no balance risk, gives resin a functional use.

### 2. Heartwood Sentry (golem)

**Construction** (follows the iron/snow golem pattern; the head is placed last):

```
[carved pumpkin | jack o'lantern]   ← head, placed last, triggers the build
[Creaking Heart]                    ← body / life
[pale oak log]                      ← base
```

- Any Creaking Heart works (crafted ones need resin, which only natural Creakings give).
- The heart and head are consumed; **the log stays as the pedestal** and the Sentry stands on it.

**Build order**: (1) entity, building, night guard AI, day statue, done; (2) heart cracks with a
heart hitbox, healing, done; (3) sneak + axe to take it apart, jack o'lantern glow, Pale Mask
slowing it, done.

**Appearance**
- The Creaking's body (tall, long arms, its animations) with a pumpkin head and its own texture.
  Placeholder textures.
- Jack o'lantern head: the Sentry gives off light (level 10) and its face glows. Carved pumpkin: it
  doesn't. Vanilla has no light from entities, so an invisible light block follows the head
  through the air and is cleared when the Sentry moves on, is taken apart or dies.
- The heart is **visible in the center of the chest**, and it is the weak point.

**Activity rule**
- The Sentry is active when its pedestal gets no daylight: **at night, or anywhere without sky
  access** (underground, enclosed). This rule applies to **player-built Sentries only**.
- **Ruin Sentries are afraid of sunlight** instead (see Pale Ruin): they're always active in the
  dark, day or night, and only freeze when direct sunlight reaches them.
- The check uses **sky light / time of day only**. Artificial light (torches, candles) doesn't put
  a Sentry to sleep.
- In daylight it walks back onto its pedestal and freezes as a **statue**: no AI, can't be
  damaged or pushed, drawn in stone grey. It's a frozen mob, not a real block (agreed; turning a
  mob into a block and back would be fragile). It wakes up when the pedestal loses daylight.
- Daylight means bright outside and open sky above the standing spot; glass and barriers let
  skylight through, so a glass roof still counts as daylight.
- The pumpkin head makes it **unaffected by gaze**: players and mobs looking at it don't freeze it.
  The only thing that slows it is a player wearing the **Pale Mask** (they look like a Creaking).

**Behavior (player-built)**
- Attacks hostile mobs (not creepers, like the iron golem) within **20 blocks** of its pedestal.
- Melee with long arms and an iron-golem-style upward launch.
- **Lower damage than an iron golem**: a flat **6**, a night/dark deterrent, not a tank.
- Turns hostile when the player attacks it, like an iron golem.

**Damage model: heart cracks**
- **Only players can damage the heart.** Mob attacks, mob projectiles and explosions do nothing
  (knockback at most). Precedent: the vanilla Creaking only takes player damage.
- The body can't be damaged; only hits on the heart count, and the heart can only be hit **from
  the front**. Any player hit still turns the Sentry against them.
- Heart damage shows as **visible crack stages** (like the iron golem's cracks): dark cracks spread
  from the core through the bark, and the core's glow shrinks and dims.
- One crack per:
  - fully charged arrow on the heart (crossbow bolts count; partly charged arrows bounce off);
  - thrown trident on the heart;
  - full-strength sword hit on the heart (riskier because of the Sentry's reach and launch).
    Swords are the item tag `vanillareforged:cracks_sentry_hearts`, so packs can add weapons.
- At most one crack per 0.5 s, so multishot volleys and quick double hits count once.
- Crack count:

  | Difficulty | Ruin Sentry | Player's Sentry |
  |---|---|---|
  | Easy   | 2 | 3 |
  | Normal | 3 | 3 |
  | Hard   | 4 | 3 |

- Last crack: the heart is destroyed and the Sentry collapses into 1–3 resin clumps and a pale oak
  log (loot table `vanillareforged:entities/heartwood_sentry`).
- Possible later addition, if fights feel slow: a fully charged arrow from a Power IV+ bow cracks 2.

**Healing**
- **Turning into a statue** (dawn, for a Sentry with sky access) clears all cracks.
- **Out of combat**: if no player is within 16 blocks, it regains **1 crack per minute**. This is
  what heals Sentries that never see daylight (underground, enclosed, Ruin Sentries). Within a
  fight it doesn't heal, so a player can step back, drink a potion and return; a long break resets
  progress.

**Moving it**
- While it's a statue, hitting it with an axe returns the **heart and the head**. Killing it with
  hits destroys the heart.
- **Sneak + use an axe** on it takes it apart at any time (including Sentries that never see
  daylight), returning the heart and the head. Costs 1 axe durability; the pedestal log stays.
  Doesn't work while the Sentry is hunting that player: an angry Sentry must be killed. Ruin
  Sentries won't allow it either, once they exist.

**Technical**
- Heart hitbox: a small box (0.5 blocks) in the chest, front side. Hits on the Sentry are traced
  through it: the attacker's look ray for melee, the projectile's flight path for arrows and
  tridents. A separate part entity like the Ender Dragon's was the first idea, but vanilla
  hard-codes part entities to the dragon in several places (level lookups, networking, player
  attacks), so it would take a handful of Mixins for the same result.

### 3. Pale Mask (tank helmet)

- Head slot, modeled on the turtle shell: a unique effect, **weak armor**. Wearing it means giving
  up a netherite helmet. **2 armor, 275 durability**, repaired with **resin clumps**, takes helmet
  enchantments. No armor trims: it uses its own model, so a trim wouldn't render.
- A real mask, not a full helmet: a custom 3D model (Fabric `ArmorRenderer`) with a bark plate over
  the face only, a few short pale twigs sticking out and a resin eye in the upper middle (the eye's
  slot in the recipe) that glows in the dark like the Creaking's eyes. Placeholder textures.
- **Recipe**: the helmet shape with a **Creaking Eye** in the top middle and any pale oak log or
  wood (`#minecraft:pale_oak_logs`) around it:

  ```
  L E L
  L   L
  ```

- **Creaking Eye** (rare ingredient): crafted from a **Watcher's Lens** set in resin, see
  [The Watcher](#5-the-watcher-boss). The mask's durability and resin repair stay as they are: the
  mask is strong, so each one costs a boss fight.
- **Watcher's Gaze**: the **hostile** mob in the crosshair within ~16 blocks gets Slowness while the
  gaze is held. Costs **1 durability per second** of slowing.
- Bosses (`#vanillareforged:gaze_resistant`: Wither, Warden, Elder Guardian) get a weaker effect:
  Slowness I plus a drag on their horizontal velocity (x0.85 per tick). Main target: Wither phase 2
  (armored, melee), so a melee tank can fight back. The Wither ignores all status effects and
  chases with hard-coded acceleration, so for it the drag is the whole effect (about 20-25% slower
  chase). The Ender Dragon is immune to effects and moves by phases, so the gaze doesn't affect it.
- Walls block the gaze. Slowness lasts 0.5 s after looking away.
- **The only thing that slows a Sentry** (any Sentry, player-built or Ruin), with the normal
  Slowness II. Sentries aren't monsters, so the gaze only takes a Sentry that's awake and hunting
  the wearer; looking at your own guard costs no durability. Ruin Sentries still see and attack
  the wearer; the gaze only slows them.
- **Passive: hidden from Creakings.** Vanilla Creakings don't target the wearer (the mask makes
  you look like one of them). Attacking a Creaking reveals you to it, like piglins and gold armor.
  Implemented with a Mixin on `Creaking#canAttack` (no Fabric hook exists). Side effects: the
  wearer's own gaze no longer freezes Creakings that ignore them (vanilla's freeze check uses the
  same `canAttack`), and a reveal is per Creaking and isn't saved, so a reloaded Creaking forgets.
- **Build order**: (1) Creaking Eye + wearable mask with recipe, done; (2) Watcher's Gaze, done;
  (3) hidden from Creakings, done.

### 4. Pale Ruin (structure)

> On hold: the structure's layout isn't decided yet. Its loot items (trim, sherds) are implemented
> and creative-only until then.

- Spawns only in Pale Gardens. **Underground or fully enclosed, with no natural light**, reached
  through a collapsed stairway or sinkhole.
- **Ruin Sentries are always active in the dark** and guard it until killed. They don't respawn.
- **Fear of sunlight** (precedent: undead burning in the sun):
  - A Ruin Sentry standing in **direct sunlight** (daytime + sky visible at its position) turns
    into a statue on the spot and wakes up once it's shaded again or night falls.
  - Statues can't be damaged and turning into one **clears all cracks**, so sunlight makes a
    Sentry harmless for a while but never easier to kill. Luring one into the sun resets your
    progress.
  - Its pathfinding **avoids sunlit blocks**. At the edge of a light patch it stops, **shields its
    heart with its arms** (hits on the heart bounce off) and backs away out of line of sight. This
    stops the player from killing it risk-free from inside a sunlit spot.
  - Only sunlight counts. Torches and other artificial light do nothing.
  - **Leashed to the Ruin**: it never chases the player outside the structure, so it can't be
    lured to the surface.
  - Removing the roof to let daylight in is allowed but deliberately expensive: the Ruin is deep
    and built from slow-to-mine blocks (deepslate/tuff bricks). Even then it only buys safety
    during the day, since at night the Sentries are active again. Number per structure: TODO, decided when the structure is built.
- **Look**: a plain Creaking body (no pumpkin) with its heart visible in the chest. The pumpkin
  marks a player's own, peaceful Sentry; a bare Creaking with a glowing heart is a Ruin guard.
- **Story**: the ancient builders' simpler work on the same idea as the Watcher: Creakings with an
  isolated heart, bound to guard. The Watcher's gaze doesn't affect them (they share its heart);
  the Pale Mask still slows them.
- Drops when killed: TODO (resin; the Creaking Eye now comes from the Watcher).
- Ruin Sentries are hostile to players and ignore other mobs, same as the vanilla Creaking (which
  only targets players). Two ways to beat them:
  - aim: bow or trident at the heart;
  - melee: Pale Mask to slow them, then a sword on the heart.
- **Archaeology**: suspicious gravel or mud holds:
  - **Watcher armor trim** smithing template (rare; duplicated with 7 diamonds + a block of resin).
    Implemented, but only obtainable in creative until the Ruin's loot exists. Its textures are
    placeholders to be replaced with final art;
  - pottery sherds: **Gaze** (eye) and **Creaking** (tree figure with a heart). Implemented with
    placeholder textures; vanilla already has a "heart" sherd, hence the names;
- **Lighting**: a consistent low level of artificial light (candles, glow lichen, soul lanterns,
  amethyst clusters). It's for atmosphere, not spawn-proofing: in Java Edition hostile mobs only
  spawn at block light 0, so lit floors stay clear while unlit corners can still spawn vanilla
  hostiles. That's intended; Ruin Sentries ignore other mobs, so they coexist.
- Mostly data-driven: structure NBT, loot tables, worldgen JSON.

### 5. The Watcher (boss)

The source of the Creaking Eye, and so of the Pale Mask. A mid-game boss (iron/diamond gear,
roughly Elder Guardian tier) with a boss bar.

**Story**
- Creakings are natural creatures with a prey instinct: under a gaze, they freeze.
- **The Watcher** is the oldest of them, a natural Creaking grown huge over ages. An ancient
  civilization couldn't destroy it, so they **improved it to control it**: they fitted it with
  copper braces and resin joints and set a **lens** in its eye socket that turns the freeze
  outward. Its gaze now freezes others.
- To keep it under control they **cut out its heart**. Without one it sleeps, and over the ages it
  has come to look like a dead pale oak held together by old copper.
- The player wakes it by **putting a Creaking Heart back in**: the same craft as the Heartwood
  Sentry, done long ago and on a much larger scale.
- The Ruin Sentries are the builders' simpler copies (see Pale Ruin). The names connect: the
  **Watcher** armor trim, **Watcher's Gaze** on the mask (the reversed gaze, inherited through its
  lens) and Creakings ignoring the mask's wearer (you look like one of them).

**Summoning** (farmable anywhere, since Pale Gardens are hard to find)
- Like the Wither and the golems, the dormant body is a **block pattern** the player builds, and
  the **Creaking Heart is placed last** to wake it. It wakes only at night or in darkness, like
  any Creaking. The heart is consumed.
- Materials, all renewable once a Pale Garden is known: **pale oak logs** (saplings grow
  anywhere), **copper blocks** for the builders' braces (any oxidation stage; weathered fits the
  look), **resin blocks** for the joints, and the Creaking Heart.
- The shape is TODO: candidates are compared in-game. Requirements: it reads as a sleeping
  creature (or a dead tree with a hollow for the heart), it's clearly different from the Sentry
  (log + heart + pumpkin) and from vanilla's two-log Creaking Heart setup, and it costs enough to
  make each eye a goal (on the order of a few copper blocks and a few resin blocks).

**The woken Watcher**
- A Creaking several blocks tall, with weathered copper plates, resin joints and a glowing lens
  for an eye. Placeholder textures.
- Fight mechanics: TODO, ideas are being collected. Starting points from the discussion: a gaze
  that slows and then freezes whoever it sees; a shield-reflected gaze that briefly freezes the
  Watcher itself; things it looks at turning into temporary statues; a single-lens blind spot.

**Drops: the craft chain** (as deep as netherite; each step has a meaning)
1. **Watcher's Lens**: one per kill. A copper-rimmed disc of resin glass, clearly made by the
   builders.
2. **Creaking Eye**: the lens in the middle, 4 resin clumps around it (plus shape). Setting the
   lens in living resin turns it back into an eye. Shaped recipe, 1 eye.
3. **Pale Mask**: unchanged (`L E L` / `L _ L` with pale oak logs).

## Parked for later (warrior content and extras)

- **Resin Tappers**: illager-type faction that farms resin from captive hearts, uses resin traps.
- Ambient threat: moths drawn to open eyeblossoms at night.
- Watcher block for redstoners: outputs a signal while a player looks at it.
- Other resin uses: slowing resin-coated blocks, amber preservation displays, tool handle upgrade.

## Open questions / TODO

1. **Ruin Sentry drops**: TODO.
2. **Watcher summon shape**: TODO, chosen from in-game mockups.
3. **Watcher fight mechanics**: TODO.
4. **Ruin Sentry count** per structure: TODO, decided when the structure is built.
