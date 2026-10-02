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

**Appearance**
- The pumpkin head is visible on the model (carved face).
- Jack o'lantern head: the Sentry gives off light. Carved pumpkin: it doesn't.
- The heart is **visible in the center of the chest**, and it is the weak point.

**Activity rule**
- The Sentry is active when its pedestal gets no daylight: **at night, or anywhere without sky
  access** (underground, enclosed). This rule applies to **player-built Sentries only**.
- **Ruin Sentries are afraid of sunlight** instead (see Pale Ruin): they're always active in the
  dark, day or night, and only freeze when direct sunlight reaches them.
- The check uses **sky light / time of day only**. Artificial light (torches, candles) doesn't put
  a Sentry to sleep.
- In daylight it is a **statue**: a solid, decorative block that can't be damaged.
- The pumpkin head makes it **unaffected by gaze**: players and mobs looking at it don't freeze it.
  The only thing that slows it is a player wearing the **Pale Mask** (they look like a Creaking).

**Behavior (player-built)**
- Attacks hostile mobs within a leash range around its pedestal (~16–24 blocks).
- Melee with long arms and an iron-golem-style upward launch.
- **Lower damage than an iron golem**: a night/dark deterrent, not a tank.
- Turns hostile when the player attacks it, like an iron golem.

**Damage model: heart cracks**
- **Only players can damage the heart.** Mob attacks, mob projectiles and explosions do nothing
  (knockback at most). Precedent: the vanilla Creaking only takes player damage.
- The body can't be damaged; only hits on the heart count.
- Heart damage shows as **visible crack stages** (like anvils, turtle/sniffer eggs).
- One crack per:
  - fully charged arrow on the heart (partly charged arrows bounce off);
  - thrown trident on the heart;
  - sword hit on the heart (riskier because of the Sentry's reach and launch).
- Crack count:

  | Difficulty | Ruin Sentry | Player's Sentry |
  |---|---|---|
  | Easy   | 2 | 3 |
  | Normal | 3 | 3 |
  | Hard   | 4 | 3 |

- Last crack: the heart is destroyed and the Sentry collapses into resin and pale oak.
- Possible later addition, if fights feel slow: a fully charged arrow from a Power IV+ bow cracks 2.

**Healing**
- **Turning into a statue** (dawn, for a Sentry with sky access) clears all cracks.
- **Out of combat**: if no player is within 16 blocks, it regains **1 crack per minute**. This is
  what heals Sentries that never see daylight (underground, enclosed, Ruin Sentries). Within a
  fight it doesn't heal, so a player can step back, drink a potion and return; a long break resets
  progress.

**Moving it**
- While it's a statue, breaking it with an axe returns the **heart and the head**. Killing it with
  hits destroys the heart.
- **Sneak + axe** on the heart takes it apart at any time (including Sentries that never see
  daylight), returning the heart and the head. Only works on a non-hostile Sentry the player
  built; a Sentry that's angry at the player, or a Ruin Sentry, must be killed.

**Technical**
- Heart hitbox: **multi-part entity, like the Ender Dragon**. The body is one part (ignores damage),
  the heart is a small separate part in the chest that receives player hits.

### 3. Pale Mask (tank helmet)

- Head slot, modeled on the turtle shell: a unique effect, **weak armor**. Wearing it means giving
  up a netherite helmet.
- **Watcher's Gaze**: the mob in the crosshair within ~16 blocks gets Slowness while the gaze is
  held. Costs durability per second of gazing.
- Bosses get a weaker effect (Slowness I instead of II). Main target: Wither phase 2 (armored,
  melee), so a melee tank can fight back.
- **The only thing that slows a Sentry** (any Sentry, player-built or Ruin). Ruin Sentries still
  see and attack the wearer; the gaze only slows them.
- **Passive: hidden from Creakings.** Vanilla Creakings don't target the wearer (the mask makes
  you look like one of them). Attacking a Creaking reveals you to it, like piglins and gold armor.
- Recipe idea: carved pale oak + Creaking Heart + resin + an item found in the Pale Ruin (ties the
  trophy to the power-up). The exact ingredient is open.

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
- Drops when killed: TODO.
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
  - the Pale Mask ingredient (TODO: working idea is a "Resin Eye").
- **Lighting**: a consistent low level of artificial light (candles, glow lichen, soul lanterns,
  amethyst clusters). It's for atmosphere, not spawn-proofing: in Java Edition hostile mobs only
  spawn at block light 0, so lit floors stay clear while unlit corners can still spawn vanilla
  hostiles. That's intended; Ruin Sentries ignore other mobs, so they coexist.
- Mostly data-driven: structure NBT, loot tables, worldgen JSON.

## Parked for later (warrior content and extras)

- **Resin Tappers**: illager-type faction that farms resin from captive hearts, uses resin traps.
- **Elder Creaking**: mini-boss with several hearts hidden around a structure; each destroyed
  heart weakens it but makes it more aggressive.
- Ambient threat: moths drawn to open eyeblossoms at night.
- Watcher block for redstoners: outputs a signal while a player looks at it.
- Other resin uses: slowing resin-coated blocks, amber preservation displays, tool handle upgrade.

## Open questions / TODO

1. **Ruin Sentry drops**: TODO.
2. **Pale Mask Ruin ingredient**: TODO. Working idea: "Resin Eye".
3. **Ruin Sentry count** per structure: TODO, decided when the structure is built.
