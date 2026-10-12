# Pale Garden: design

Status: features 1, 2, 3, 5 and 6 are implemented with placeholder art and sound; the Pale Ruin (4)
generates with its guards and its loot. Everything still to do is collected under [What's left](#whats-left); what to
judge in a first playthrough is in [the playtest checklist](pale-garden-playtest.md).

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
- **Players, and mob projectiles, can damage the heart.** Mob melee and explosions do nothing
  (knockback at most): zombies always hit from the front, so with melee counting a Sentry wouldn't
  survive a night. A skeleton or pillager arrow (or a drowned's trident) on the heart cracks it
  like a charged player arrow, so a Sentry isn't near-immortal against mobs. A monster that shoots
  it becomes its target.
- The body can't be damaged; only hits on the heart count, and the heart can only be hit **from
  the front**. Any player hit still turns the Sentry against them.
- Heart damage shows as **visible crack stages** (like the iron golem's cracks): dark cracks spread
  from the core through the bark, and the core's glow shrinks and dims, one stage per crack.
- One crack per:
  - fully charged arrow on the heart (crossbow bolts count; partly charged arrows bounce off);
  - thrown trident on the heart;
  - sword hit on the heart, at any charge (riskier because of the Sentry's reach and launch).
    Swords are the item tag `vanillareforged:cracks_sentry_hearts`, so packs can add weapons.
- At most one crack per 0.5 s, so multishot volleys and quick double hits count once.
- Crack count:

  | Difficulty | Ruin Sentry | Player's Sentry |
  |---|---|---|
  | Easy   | 4 | 5 |
  | Normal | 5 | 5 |
  | Hard   | 6 | 5 |

- Last crack: the heart is destroyed and the Sentry collapses into 1–3 resin clumps and a pale oak
  log (loot table `vanillareforged:entities/heartwood_sentry`).
- Possible later addition, if fights feel slow: a fully charged arrow from a Power IV+ bow cracks 2.

**Healing**
- **Turning into a statue** (dawn, for a Sentry with sky access) clears all cracks.
- **Out of combat**: if no player is within 16 blocks and it has no target, it regains **1 crack
  per minute**. This is what heals Sentries that never see daylight (underground, enclosed, Ruin Sentries). Within a
  fight it doesn't heal, so a player can step back, drink a potion and return; a long break resets
  progress.

**Moving it**
- While it's a statue, hitting it with an axe returns the **heart and the head**. Killing it with
  hits destroys the heart.
- **Sneak + use an axe** on it takes it apart at any time (including Sentries that never see
  daylight), returning the heart and the head. Costs 1 axe durability; the pedestal log stays.
  Doesn't work while the Sentry is hunting that player: an angry Sentry must be killed. Ruin
  Sentries don't allow it at all.

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

- **Creaking Eye** (rare ingredient): crafted from an **Elderwood Lens** set in resin, see
  [The Elderwood](#5-the-elderwood-boss). The mask's durability and resin repair stay as they are:
  the mask is strong, so each one costs a boss fight.
- **Elderwood's Gaze**: the **hostile** mob in the crosshair within ~16 blocks gets Slowness while the
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
- **Immune to petrification**: the wearer looks through the same lens,
  so the Elderwood's and Ruin Sentries' petrifying gaze doesn't take hold. Blocking it costs
  **1 durability per second** of being watched, like the mask's own gaze, so the mask makes boss
  farming easier without making it free.
- **Passive: hidden from Creakings.** Vanilla Creakings don't target the wearer (the mask makes
  you look like one of them). Attacking a Creaking reveals you to it, like piglins and gold armor.
  Implemented with a Mixin on `Creaking#canAttack` (no Fabric hook exists). Side effects: the
  wearer's own gaze no longer freezes Creakings that ignore them (vanilla's freeze check uses the
  same `canAttack`), and a reveal is per Creaking and isn't saved, so a reloaded Creaking forgets.
- **Build order**: (1) Creaking Eye + wearable mask with recipe, done; (2) Elderwood's Gaze, done;
  (3) hidden from Creakings, done.

### 4. Pale Ruin (structure)

> Implemented: the Ruin Sentry, the structure (the entrance, eight kinds of room and the hall) and
> its loot.

The ancient builders' temple, where they bound Creakings to guard and woke the Elderwood. The
woods have grown over it and their roots through it.

#### Layout

- Spawns only in Pale Gardens, **underground, with no natural light**. On the surface only a
  collapsed stairway shows, overgrown by the woods.
- **Small**: the entrance, **4 rooms** and the hall. The rooms' order and choice are
  **randomized** (jigsaw pieces), the entrance and the hall are fixed ends.
- **Columns** hold up every ceiling and are the cover from the Sentries' gaze, and in the hall
  from the Elderwood's. Built from slow-to-mine stone (deepslate or tuff bricks), which the
  Elderwood's slam doesn't break.
- **Pale roots** run through the walls and rooms: thick pale oak wood, with moss and hanging moss,
  as the sign of the woods taking the temple back. Some carry a **natural Creaking Heart**, so
  vanilla Creakings walk the corridors. They're the dungeon's filler instead of new mobs: they
  already drop resin when hit, and a heart can be taken home. Vanilla wakes a heart by the time
  of day alone (its `creaking_active` attribute follows the day's timeline, dusk to dawn), so
  underground too they're awake at night and dormant by day. A heart in a template has to be
  written as dormant, not uprooted: vanilla doesn't tick an uprooted heart, and only works its
  state out when one is placed by hand, so an uprooted one would never wake.
- **Lit throughout** by the builders' lights (oxidized copper lanterns): in Java Edition
  hostile mobs only spawn at block light 0, so with no dark floor left nothing but the Ruin's own
  guards is down there.
- **The hall** is the last and biggest room, the builders' main research hall: about 25 x 25 and
  at least 12 high, since the Elderwood needs a 5 x 5 x 10 column to grow and its gaze reaches 24
  blocks. No Sentries in it. In its middle stands the **felled stump**, whole but for its heart:
  the pale oak log, the four resin roots and the copper cap, with nothing on top. Placing a
  Creaking Heart there wakes the Elderwood, as on a stump the player builds.
- The pieces are generated by script, like the Elderwood's model, and corrected after walking
  through them in game.

Built so far (`tools/pale_ruin.py` writes the templates and the worldgen files):

- **Materials**: tuff bricks for the walls, deepslate bricks and tiles for footings and floors,
  **calcite** for the columns and a pale band along the walls, oxidized copper for the columns'
  bands in the hall, the lanterns and their chains, resin only in the stump. Under lantern light
  calcite reads as cream rather than white.
- **The line**: a pavilion on the surface with a stairway 20 down, four guard rooms, a
  second stairway 16 down and the hall, so the hall's floor lies about 37 below the surface. Each
  room's exit is straight ahead, left or right, which makes the line wind.
- Jigsaw placement doesn't backtrack: a piece that doesn't fit is dropped with everything after
  it. Two rules keep the line whole. A room only gets exits that keep the line within one turn of
  the stairway's direction, so it never curls back on itself; that takes one copy of each room
  per bearing, 24 in all. And the hall, twice as wide as a room, lies **a level below**
  the rooms, where nothing of the Ruin is in its way.
- **The pavilion**'s floor is level with the ground: on a slope the uphill side is cut back (the
  air above the floor is cleared) and the downhill side stands on a rubble footing. Pale oaks
  grow over and into it. Its stairway doubles back: one flight north out from under the
  pavilion, a landing, a second flight south beneath it. That isn't for looks. Jigsaw sets the
  start piece's height by the ground at the middle of its bounding box; with one straight
  flight that middle was 12 blocks down the stairs, so on sloping ground the pavilion came out
  buried or on stilts. Doubled back, the middle lies under the pavilion.
- **Rooms** are 19 x 13 inside and 7 high, lit by lanterns hanging from the ceiling. The first
  ones were 13 x 11 and felt cramped with two Sentries between the columns; the line didn't get
  longer for it, the passages between rooms got shorter. Each of the four is one of eight kinds,
  picked at random. Three are the temple's own:
  - **guard room**: four 2 x 2 columns set wide apart, a root down one corner, moss;
  - **overgrown room**: the woods have it. A great root comes through the ceiling, slants down
    across the room and runs out along the floor; two of the columns are down, with moss,
    eyeblossoms and rubble where the floor was;
  - **workshop**: benches and pots along the walls, two columns, and a stump the builders never
    finished: the log and two of its four resin roots on a plinth, the copper for its cap
    standing beside it. It's the hint at what the hall's stump is.

  Five show what the builders did here, which was study the Creakings and their freeze and turn
  it into the Sentries' eyes and the Elderwood's lens:
  - **archive**: rows of bookshelves, which are the cover here instead of columns, a lectern
    between them, cobwebs;
  - **cells**: copper-barred cells along the far wall, their gates open now. One still holds a
    pale oak log; in a room with a heart, the heart is in it, between two logs as a living tree
    holds one. A skull lies in another;
  - **lens room**: a lens in its copper frame, five blocks square (amber glass), aimed at three
    posts of pale oak with resin on them; grindstones, and a bench with crystals;
  - **resin works**: copper vats full of resin under their hoist chains, a third one split with
    resin spilled around it, cauldrons, and resin bricks stacked by the wall;
  - **nursery**: four beds of earth with pale oak saplings that never grew, and eyeblossoms.
- **Furnishing is a piece of its own**: a room's walls, lanterns and way out are one template,
  and what stands in it another, set into the room through a jigsaw in its floor. Without that
  every kind needed a copy for each of the 24 ways a room can lie in the line; now a kind is
  three templates, one per variant (two Sentries; one Sentry, a heart and the tool chest; two
  Sentries, a heart and a chest), and the Ruin is 51 templates in all.
- **7 Sentries**: 2, 1, 2 and 2 in the four rooms, whatever their kind. The second and the
  fourth room have a Creaking Heart, in the root down their corner; in the overgrown room it
  lies in plain sight in the root on the floor, and in the cells it's the captive.
- **The hall** is 25 x 25 inside and 13 high: two colonnades of four columns, the stump on a
  stepped dais between them with a pale square in the floor around it, lanterns on posts along
  the walls and on chains over the nave, roots in two corners.
- **Light**: the generator checks that no spot a mob could stand on, in any piece, is left at
  block light 0. On top of that the structure overrides monster spawns to none inside its pieces,
  so nothing but its own guards lives there even if a lantern is broken.
- **Water**: underground the Ruin cuts through water pockets. Vanilla waterlogs whatever a
  structure places in water, and a waterlogged lantern floods the room around it, so the
  structure is set to ignore waterlogging, like the Trial Chambers. A GameTest builds the hall
  into water to check.
- **Where**: only in Pale Gardens, on a grid of 12 chunks with at least 6 between two Ruins. A
  garden is small, so many have none; to be tuned once it's been looked for in play.
- A GameTest lays the Ruin out 300 times without building it and checks every layout has all
  its pieces, the straight line (the longest) among them.
- Not yet: the hall's mural and the Sentries' leash tied to the structure's bounds. On steep ground a deep piece can still come out
  of a hillside.

#### Ruin Sentries

- **7 per Ruin**, placed by the pieces. **Always active in the dark**; they guard it until killed
  and don't respawn.
- **Look**: a plain Creaking body (no pumpkin) with its heart visible in the chest. The pumpkin
  marks a player's own, peaceful Sentry; a bare Creaking with a glowing heart is a Ruin guard.
- **Story**: the builders' simpler work on the same idea as the Elderwood: Creakings with an
  isolated heart and a **shard of the Elderwood's lens** in the eye, bound to guard. The Elderwood's
  gaze doesn't affect them (they share its heart); the Pale Mask still slows them.
- **A weaker petrifying gaze** (see [Petrification](#5-the-elderwood-boss)): it fills the meter more
  slowly than the Elderwood's.
- Hostile to players and ignoring other mobs, same as the vanilla Creaking (which only targets
  players). Two ways to beat them:
  - aim: bow or trident at the heart;
  - melee: Pale Mask to slow them, then a sword on the heart.
- **Leashed to the Ruin**: it never chases the player outside the structure, so it can't be
  lured to the surface.
- Implemented, as its own entity (`ruin_sentry`, with a spawn egg) sharing the Heartwood Sentry's
  heart, cracks, healing and melee. Notes:
  - **the gaze** fills the meter by **1 a tick** (the Elderwood: 2, or 3 with its core open), so
    5 s of being watched petrifies. It reaches **12 blocks**, 60 degrees to each side of where
    its head points, and needs a clear line: a column breaks it. It only holds its target;
  - **the leash** is to its **post**, the spot where it first stands, and **16 blocks** long. It
    doesn't pick, keep or follow a target beyond that. Tying it to the structure's bounds
    instead comes with the structure;
  - **cracks**: 4 on Easy and Peaceful, 5 on Normal, 6 on Hard, as in the table above;
  - **drops**: 1-3 resin clumps, 1-2 Elderwood Lens Shards and 10 XP;
  - it isn't a monster by vanilla's rules (a golem, like the player's Sentry), so it stays on
    Peaceful, where it stands guard without attacking, and a player's Sentry or an iron golem
    doesn't go for it. The Pale Mask's gaze slows it whether or not it's hunting the wearer;
  - **look**: the Heartwood Sentry's model with a bark head and one eye, a lens shard that always
    glows. Placeholder textures;
  - not yet: a player shooting it from beyond its leash is left alone.
- **Fear of sunlight** (precedent: undead burning in the sun). Underground it only matters if the
  player digs the roof open, which is allowed but deliberately expensive: the Ruin is deep and
  its stone slow to mine, and it only buys safety during the day.
  - A Ruin Sentry standing in **direct sunlight** (daytime + sky visible at its position) turns
    into a statue on the spot and wakes up once it's shaded again or night falls.
  - Statues can't be damaged and turning into one **clears all cracks**, so sunlight makes a
    Sentry harmless for a while but never easier to kill. Luring one into the sun resets your
    progress.
  - Its pathfinding **avoids sunlit blocks**. At the edge of a light patch it stops, **shields its
    heart with its arms** (hits on the heart bounce off) and backs away out of line of sight. This
    stops the player from killing it risk-free from inside a sunlit spot.
  - Only sunlight counts. Torches and other artificial light do nothing.
  - Implemented. Notes:
    - direct sunlight is daytime with open sky above, read from skylight like the player's
      Sentry: glass lets it through;
    - it shields its heart while **its target** stands in sunlight, with its arms crossed over
      its chest, and backs away through the shade, never past its leash, until that player has
      no line of sight to it. In a room with nowhere to hide it ends up against the far wall;
    - a hit bounces off (with a dull knock on wood) whenever **the attacker** stands in sunlight,
      target or not, so a second player can't snipe from a light patch either;
    - its gaze still works from the shade: standing in the sun is safe from its arms, not from
      being petrified;
    - at night an open roof changes nothing.

#### Rewards

- **Lens shards**: each Ruin Sentry drops resin and **1-2 Elderwood Lens Shards**, and the hall's
  loot holds **2 more**, so a cleared Ruin gives 9 to 16. **9 shards craft an Elderwood Lens**:
  one Ruin always gives one lens. The Ruin is a one-time source found by exploring, the Elderwood
  is the repeatable farm.
- That lens makes the Pale Mask, which blocks the Elderwood's gaze: the dungeon hands the player
  the counter to the boss at its end.
- **Archaeology**: decorated pots, and suspicious gravel or mud holding:
  - **Elderwood armor trim** smithing template (rare; duplicated with 7 diamonds + a block of resin).
    Its textures are placeholders to be replaced with final art;
  - pottery sherds: **Gaze** (eye) and **Creaking** (tree figure with a heart). Implemented with
    placeholder textures; vanilla already has a "heart" sherd, hence the names.
- The **Buried Garden** advancement for entering it.

Placed so (loot tables under `chests/pale_ruin/`, `pots/` and `archaeology/`):

- **The hall's chest** stands before the stump, between two pots that show the Creaking sherd
  (the tree with its heart) to the door. It always holds **one Creaking Heart** and **two lens
  shards**, with 4-6 stacks of treasure: resin bricks and clumps, copper blocks, amethyst,
  emeralds, pale oak saplings, rarely diamonds or a golden apple.
- **The tool chest**, in the second room, whose root carries a heart: always a **worn iron axe
  with Silk Touch**, and supplies (resin, copper and iron ingots, pale oak logs, candles, bread).
  The fourth room, the other one with a heart, has a chest of supplies with now and then
  emeralds or an enchanted book.
- **Pots**: two in every room and the workshop's own five, each with one stack: resin, copper,
  or emeralds.
- **Heaps to brush**: suspicious gravel lying on the floor with rubble beside it (in the floor
  it could fall through a cave under the Ruin), two in every room and four in the hall's corners.
  In the rooms 4 of 14 finds are a sherd, the rest resin, copper, emeralds and odds and ends. In
  the hall 6 of 12 are a sherd, 2 the **Elderwood armor trim**, 1 a lens shard: about half of
  all Ruins give a trim.

#### The missing heart

The stump shows that something goes on top; nothing says it's a Creaking Heart, or how to get one
whole. Facts: a heart drops itself only with **Silk Touch** (otherwise 1-3 resin clumps), and it
can also be **crafted** from two pale oak logs and a block of resin, a recipe vanilla unlocks on
picking up a resin block. So Silk Touch is a shortcut, not a requirement. The Ruin says it twice:

1. **A heart is given**: one whole Creaking Heart kept in the hall or the room before it (on a
   pedestal, or in a pot or chest), so the first fight needs no outside knowledge.
2. **The builders' tool**: a worn iron axe with Silk Touch in the loot of a room where a root's
   heart is in plain sight, for every fight after the first. Precedent: Swift Sneak books in
   Ancient Cities.

On top of that, as decoration: the Creaking sherd (a tree with a heart) guaranteed in the hall's
pots and a mural of the tree with its hearts on the hall's wall, and Old Growth's description,
which already names the heart and the stump.

Build order: the Ruin Sentry, the structure, then the loot, the archaeology and the advancement.
All done.

### 5. The Elderwood (boss)

The source of the Creaking Eye, and so of the Pale Mask. A full boss with a boss bar, for
iron/diamond gear: a huge, rooted tree fought by moving around it.

> **Reworked.** The first version, the Watcher, was a 4.6-block walking relative of the Creaking
> with five hearts on its front. In play it read as a reskinned Warden (the names are close too)
> and as a mini-boss, so the Elderwood replaced it. What carried over unchanged is marked
> "kept"; the rework's steps are under Build order. All of this section is implemented.

**Story**
- Creakings are natural creatures with a prey instinct: under a gaze, they freeze.
- **The Elderwood** is the oldest pale oak, the tree the first Creaking Heart grew in. Over ages
  the heart woke the whole tree. An ancient civilization couldn't destroy it, so they **improved
  it to control it**: they banded its trunk with copper, riveted plates over its wounds and set a
  **lens** in a hollow, which turns the Creakings' freeze outward. Its gaze now freezes others.
- To keep it under control they **cut out its hearts and felled it** to a stump. A stump with its
  copper collar still on it is all that's left, and the builders' method still works on any pale
  oak: the player wakes one by **putting a Creaking Heart back**, the same craft as the Heartwood
  Sentry, done long ago and on a much larger scale.
- The Ruin Sentries are the builders' simpler copies (see Pale Ruin). The names connect: the
  **Elderwood** armor trim, **Elderwood's Gaze** on the mask (the reversed gaze, inherited through
  its lens) and Creakings ignoring the mask's wearer (you look like one of them).

**Summoning** (farmable anywhere, since Pale Gardens are hard to find)
- The player builds a **stump**, and the **Creaking Heart is placed last**, on top. It wakes only
  at night or in darkness, like any Creaking. The heart is consumed.
- **Pattern**: 7 blocks. A humanoid frame that turns into a 10-block tree made no sense, so the
  build is only the stump and the tree **grows out of it**:

  ```
  top view, ground level     side view
  .  R  .                    .  ♥  .     ♥  Creaking Heart, placed last
  R  L  R                    .  C  .     C  any copper block: the collar
  .  R  .                    R  L  R     L  pale oak log or wood; R  resin blocks: the roots
  ```

- Materials per summon: 1 pale oak log, 1 copper block, 4 resin blocks and the Creaking Heart.
  With the 4 resin clumps for the eye that's about 49 resin per Creaking Eye (the Watcher's
  pattern took about 31): a real goal, renewable once a Pale Garden is known.
- **Any copper block works, and it shows on the Elderwood** (kept): copper, exposed, weathered or
  oxidized, waxed or not. The woken tree's bands and plates take the collar's oxidation stage and
  keep it for the whole fight.
- **It grows in three bursts** over 3 s, not smoothly: the stump's blocks break and a sprout a
  third of its size stands in their place, then twice a second later it snaps to the next size
  in a burst of wood splinters with a crack and a creak, reaching full size on the third. While
  growing it can't be hurt and doesn't attack. It faces the player who placed the heart.
- **Room to grow**: about 5 × 5 blocks around the stump and 10 up. Anything that isn't a full
  block (grass, flowers, snow, moss carpets) may stay and is left alone, as in the Watcher's
  pattern. If full blocks are in the way nothing is destroyed; the heart stays a block and the
  player sees "The Elderwood needs room to grow" above the hotbar.
- Placing the heart in daylight shows "The Elderwood only wakes in darkness" (kept). "Darkness"
  means not daytime with open sky above.
- The heart sits on copper, so the pattern never matches vanilla's Creaking Heart setup (a heart
  between two logs) or the Sentry (log + heart + pumpkin).

**The woken Elderwood**
- **An old pale oak about 10 blocks tall**: a thick trunk about 3 blocks across, spreading roots,
  and a crown of bare branches and twigs with hanging moss, about 7 blocks wide at the top.
  Placeholder model and textures built from boxes, generated from one spec by
  `tools/elderwood_model.py` so the geometry (`geometry/elderwood.json`) and the UVs agree; the
  part names are a contract so an artist can replace the look later without touching the fight.
- **Metal everywhere**, so it reads as the builders' work: copper bands around the trunk, riveted
  plates over old wounds, a collar holding the lens in a hollow about 4 blocks up, braces on the
  two main branches and a plate over the core. Four texture variants, one per oxidation stage.
- **Only weak points glow** (kept): the lens and the hearts that can be hit right now, full-bright
  so they read in the dark.
- **Rooted**: it never leaves the spot it grew on. It can't be pushed, knocked back or moved by
  pistons, and its trunk is solid, so players walk around it, not through it.
- **It turns slowly in place** to face its target, slower than a player can circle it (about 30°
  a second; a sprinting player 4 blocks away circles at about 80°).
- It hunts players only and never despawns. It counts as gaze-resistant for the Pale Mask. Drops
  its lens and 2–4 resin clumps (kept).

**Petrification** (kept; shared with Ruin Sentries)
- A **meter**, like vanilla freezing in powder snow, not a status effect: milk clears all effects,
  which would turn every fight into "drink milk".
- It **fills while the lens has line of sight** to the player and drains once sight is broken.
  The player slows down more and more, and when it's full they're **petrified**: stuck in place.
- **Brittle**: damage taken rises with the meter, up to about **+50%** when petrified (you're
  slowly turning into a statue).
- A grey, cracked stone overlay creeps in from the screen edges as it fills, like powder snow's
  frost overlay.
- Implemented (players only; creative and spectators are immune):
  - the meter runs from 0 to 100; each source decides how fast it fills it per tick;
  - movement only slows once the meter is half full, then by up to **60%**; when full the player
    is petrified for **3 s** (no movement, no jumping) even if the gaze breaks;
  - cracking free (or being shattered by a slam) empties the meter and gives **3 s in which no
    gaze takes hold**. Before, the player came out half full and still in the gaze, so in the
    open one petrification led straight into the next;
  - a **shield raised towards the gaze blocks it**: the meter drains behind it, at 1 shield
    durability per second. The price is the shield's own: slow walking and no attacking;
  - without a gaze it drains **2 per tick** (a full meter clears in 2.5 s);
  - stored as a synced Fabric data attachment, not saved: relogging clears it. The brittleness
    needs a Mixin on `LivingEntity#hurtServer` (Fabric has no event that changes damage); it
    applies after difficulty scaling.

**The fight** (single player first; numbers to be tuned)

The Elderwood has a front and a back, and each is dangerous in its own way. In front the lens
petrifies and the branches slam; behind and to the sides the roots strike. Its hearts are spread
all around the trunk, so the player has to keep moving between the two.

- **The gaze** covers a cone in front of the lens (about 60° to each side) out to 24 blocks and
  fills the meter by 2 per tick (petrified in about 2.5 s), 3 once the core is open. Four ways
  out of it:
  - **a raised shield**, the one that needs nothing built and no mask: the first kill has to be
    possible without the mask, on open ground;
  - **circle**: the trunk turns slower than a player runs;
  - **cover**: any block between the player and the lens stops it, including pillars the player
    builds before or during the fight;
  - **the Pale Mask** (see Pale Mask).
- **Hearts**: it can only be hurt through its hearts, each with crack stages like the Sentry's and
  traced the same way (kept: a sword, a charged arrow or a trident, at most one
  crack per 0.5 s, players only). A glowing heart is one that can be hit right now; a destroyed
  heart is a dark husk. Any sword swing counts, however charged: the first version asked for a
  full-strength swing, which in a fight read as misses with the crosshair on the heart. The
  half-second between cracks is what paces it, on the Sentry too.
  **Seven hearts, 23 cracks**; the boss bar shows how many are left:
  - **4 trunk hearts** (3 cracks each) at sword height: front, back, left and right. **Always
    open**, each from its own side. The danger is where you have to stand: the front one is in
    the gaze, the others are where the roots strike;
  - **2 branch hearts** (3 each), one on each main branch, about 6 blocks up: out of a sword's
    reach and closed until that branch is stuck after a slam, or the tree is stunned;
  - the **core** (5), under the lens behind a copper plate that opens once the other six are
    destroyed. Destroying it kills the Elderwood.
- **Branch slam** (reworked from the Watcher's): against a player in front within about 6 blocks
  it raises a main branch for 0.75 s, then brings it down where the player stood when it started,
  so stepping aside dodges it. Players near the strike take **10 damage** (15 when petrified,
  before armor), their petrification **shatters** and they're thrown back. The loop is gaze,
  petrify, slam, back to cover.
  - **Stuck branch**: the branch stays planted for 1.5 s, which brings its heart down to sword
    height, glowing and open from any side. While a branch is planted the tree doesn't gaze or
    turn. Branches alternate, with 2 s between slams.
  - **It smashes soft cover**: blocks under the strike that are in the
    `#vanillareforged:elderwood_breakable` tag (logs, planks, leaves, dirt and the like) break
    and drop. Stone and harder cover holds, so a quick wooden hut doesn't trivialize the fight.
    Follows the `mobGriefing` game rule.
- **Root eruption** (new): against a player it can't see in front of it (behind, to the side or
  behind cover) within about 12 blocks. The ground under the player cracks for 1 s, then roots
  burst out: about 6 damage (the brittle bonus applies) and a toss into the air. Moving out of the
  cracked patch dodges it, like evoker fangs. It keeps circling and camping from being free.
- **Burning resin** (kept): at a player in the gaze cone beyond the slam's reach it throws a glob
  of burning resin from the lens, the first after 1 s and then every 3 s. A hit deals **5 damage
  and 5 s of fire**; blocks never catch fire. Its own look: a molten amber glob with a white-hot
  core, without vanilla's fire overlay.
- **Hitting the resin back** (kept): a melee hit sends it where the player looks, like a ghast's
  fireball. Hit back into the Elderwood it does no damage but **stuns it for 3 s**; only resin a
  player hit back does. For 5 s afterwards it can't be stunned again.
- **Stunned**: it doesn't turn, gaze, slam, throw or raise roots, its lens goes dark and its
  branches droop, so **both branch hearts open** to arrows and tridents (still too high for a
  sword). This is how a ranged player opens them without baiting a slam.
- **Falling dormant**: with no player within 32 blocks for 30 s it stops, and its hearts heal one
  crack every few seconds, destroyed ones included. Walking away resets the fight; it doesn't let
  the player whittle it down from safety. Its lens is dark while it's dormant.
- **Reading the fight** (polish):
  - **the gaze shows**: a thin trail of amber motes runs from the lens towards whoever it holds,
    so the cone can be seen before the stone creeps in, and by other players;
  - **a crack shows on its heart**: resin chips fly from the heart that took it, and the heart's
    own shards when it breaks. The tree doesn't flash red: on a model this size that read as
    nothing in particular;
  - **its own sounds**, with their own subtitles ("Elderwood raises a branch", "Ground cracks",
    "Elderwood is stunned"…). They still play vanilla audio; real audio is for a sound designer.
- **Death**: it doesn't tip over like other mobs. Its lens and hearts go dark and it shakes itself
  apart over about 4 s in three loud breaks: the crown and branches come off, then the upper
  trunk, and then the stump bursts (an explosion that harms nothing). Its lens and resin are
  left where the trunk stood. The model is built in those three layers for it.
- The hit tracing is shared with the Sentry (`HeartHit`, kept); each heart is a box at a fixed
  spot on the tree, and a planted branch's box sits where its heart ends up.

**Drops: the craft chain** (as deep as netherite; each step has a meaning)
1. **Elderwood Lens**: one per kill. A copper-rimmed disc of resin glass, clearly made by the
   builders. Also crafted from **9 Elderwood Lens Shards** (Ruin Sentry drops).
2. **Creaking Eye**: the lens in the middle, 4 resin clumps around it (plus shape). Setting the
   lens in living resin turns it back into an eye. Shaped recipe, 1 eye.
3. **Pale Mask**: unchanged (`L E L` / `L _ L` with pale oak logs).

The lens, shard and both recipes are implemented (recipe book unlocks on picking up a lens or a
shard). Placeholder textures: amber resin glass in a weathered
copper rim.

**Build order**

The Watcher, done: (1) sturdier Sentry hearts and mob projectiles; (2) the lens, lens shard and
the Creaking Eye recipe; (3) petrification meter, its overlay and the mask's immunity; (4) summon
pattern and the entity with its gaze; (5) body hearts, branch slam and the stuck-arm window;
(6) burning resin and the reflect stun.

The rework into the Elderwood:
1. **Rename**: Watcher to Elderwood everywhere, items included (Elderwood Lens, Elderwood Lens
   Shard, Elderwood armor trim) with their ids, tags and translations. Nothing is released yet,
   so old ids aren't migrated: Watcher items in existing test worlds disappear. Done: the code
   now calls the old walking body the Elderwood until step 2 replaces it.
2. **The tree**: the rooted, solid, slowly turning entity at its new size with the placeholder
   tree model, the stump pattern, growing, and the room and darkness messages. Done. Notes:
   - its collision box is the trunk (3 × 3 × 10); the crown and branches are only drawn;
   - branch hearts hang outside the collision box, where neither a crosshair nor an arrow finds
     the tree, so each has an invisible entity of its own (`elderwood_branch_heart`) that follows
     it, can only be targeted while its heart is open and passes hits on to the tree. It isn't
     saved; the tree makes its own again. (The first attempt widened the tree's pick radius,
     which arrows ignore: a stunned tree's branch hearts couldn't be hit at all.)
3. **Hearts and gaze**: the seven hearts around the trunk, the gaze cone, and resin and stun
   adjusted to the new body. Done. A trunk heart only opens to a hit that lands on its own side
   of the trunk. The resin leaves from the lens, not the crown: it's only thrown at a player the
   lens sees, so it always starts on the trunk's front.
4. **Branch slam**: the longer slam, the planted branch bringing its heart down, and smashing
   soft cover. Done. Notes:
   - it slams at a player within 75° of straight ahead and between 2.5 and 6 blocks from the
     trunk's center (a closer or farther spot is pulled into that ring);
   - the planted branch is drawn stretched or shortened so its tip is in the ground where the
     slam landed, and its heart sits 70% of the way from the branch's base to that spot;
   - soft blocks break within 1.5 blocks of the landing spot, from its level to 3 blocks above
     it, so a wooden roof goes too but the ground it lands on stays. The tag holds logs, planks,
     leaves, dirt, sand, wool and wooden slabs, stairs, fences, gates, doors and trapdoors.
5. **Roots and dormancy**: the root eruption and falling dormant. Done. Notes:
   - it no longer needs to see a player to target one: it senses anyone within 32 blocks, turns
     towards them and raises roots, while the gaze and the resin still need the lens to see them;
   - roots first come 1 s after the lens loses the player and then every 2.5 s, on the spot where
     the player stands at that moment. They're their own short-lived entity with a placeholder
     model of four spikes, and deal **6 damage** with a toss of about 2 blocks (low enough that
     landing doesn't hurt);
   - dormant after 30 s without a target; it then mends one crack every 5 s.

Ruin Sentries petrify too, more slowly (feature 4).

### 6. Advancements

Six advancements in vanilla's **Adventure** tab (no tab of our own, like a Mojang update would).
Their descriptions double as the hints the mod otherwise lacks.

| Advancement | Earned by | Frame | Trigger |
|---|---|---|---|
| **Buried Garden** | Entering a Pale Ruin | task | vanilla `location`, inside the structure |
| **Old Growth** | Waking the Elderwood | goal | vanilla `summoned_entity`, which the summon fires for players within 8 blocks |
| **Set in Stone** | Being fully petrified and living through it | task | `vanillareforged:survived_petrification` |
| **Deadwood** | Killing the Elderwood | goal | vanilla `player_killed_entity`: the core's last crack is credited to whoever dealt it |
| **One of Them** | Wearing a Pale Mask next to a Creaking that ignores you | task | `vanillareforged:ignored_by_creaking` |
| **Don't Blink** | Killing the Elderwood without being petrified once | challenge, 100 XP | `vanillareforged:killed_elderwood_unpetrified` |

- **Tree**: Old Growth hangs off vanilla's **Heart Transplanter** (placing a Creaking Heart), so
  its description, which spells out the stump, shows up for anyone who has started the Adventure
  tab. It leads to Set in Stone and Deadwood, and Deadwood to One of Them and Don't Blink.
  Buried Garden hangs off Heart Transplanter too, beside it.
- **Triggers**: the three of our own are plain vanilla `PlayerTrigger`s registered under our ids
  (`ModCriteria`), like vanilla's `avoid_vibration`: the code decides when they fire.
- **Set in Stone** is granted when the stone cracks open, by time or by a hit, if the player is
  still alive then. A meter that never filled doesn't count.
- **One of Them** needs the Creaking to be one that would otherwise attack (so not in Creative)
  and within **4 blocks**.
- **Don't Blink**: the tree remembers whom its gaze has petrified, saved with it so relogging
  doesn't clear it, and forgets once it has gone dormant and mended every crack: that's a new
  fight. Only the player who lands the last crack is checked and rewarded, like Deadwood. A Pale
  Mask makes it easy, which fits: that's the second kill.

## Parked for later (warrior content and extras)

- **Resin Tappers**: illager-type faction that farms resin from captive hearts, uses resin traps.
- Ambient threat: moths drawn to open eyeblossoms at night.
- Gaze block for redstoners: outputs a signal while a player looks at it.
- Other resin uses: slowing resin-coated blocks, amber preservation displays, tool handle upgrade.

## What's left

Nothing in this section is implemented or agreed in detail. It's the backlog: ideas to pick from,
roughly in the order they'd pay off.

### A. Advancements

Six are in (feature 6). Left:

- Vanilla's "Crafting a New Look" and "Smithing with Style" list trim recipes and templates by
  id, so the Elderwood trim doesn't count towards them. Adding it means overriding vanilla's
  advancement files; probably not worth it for the second one, which would become harder.

### B. Finish what's on hold

- **The Pale Ruin** (feature 4): it generates, with its Sentries and its loot. Left: the hall's
  mural; the Sentries' leash tied to the structure's bounds; how often it generates; a playthrough
  from the pavilion to the Elderwood to see whether the loot and the difficulty hold up.
- **Second playtest of the Elderwood** after the easing in PRs #27 to #30 (breather after
  petrification, shield blocking the gaze, any sword swing, staged death). The demo video
  predates all of it and should be re-recorded once the numbers settle.
- **Elderwood numbers**: cracks per heart, turn speed, root damage, stun length, gaze fill rate,
  resin damage and the summon's resin cost are first guesses.
- **Difficulty**: the Sentry's cracks scale with difficulty, the Elderwood's nothing does.
  Candidates: gaze fill rate, slam damage, root interval. Also what Peaceful means for it (it's
  a monster, so it would vanish; refusing to wake may read better).
- **Multiplayer**: whom it targets and turns to with several players around, the gaze holding
  only one of them, more cracks per heart with more players, and loot (one lens for the group).
  Petrification isn't saved, so relogging mid-fight clears it; fine alone, an exploit in a group.
- **Dedicated server run**: everything so far was tested through GameTests and a single-player
  client.

### C. Discoverability

Nothing in the game tells the player any of this exists: the Sentry and stump patterns, that only
hearts can be hurt, that resin can be hit back, that a shield stops the gaze. Ideas, cheapest
first:

- **Advancement descriptions** as hints: done for the stump (Old Growth), the hearts (Deadwood)
  and the mask (One of Them). Nothing yet says that resin can be hit back or that a shield stops
  the gaze.
- **The felled stump** stands in the Ruin's hall, whole but for its heart (feature 4): it shows
  the pattern and is the first place to summon.
- **The sherds and the Ruin tell it**: the Creaking sherd already shows a tree with a heart; the
  Ruin could hold a broken Sentry frame on its pedestal and a mural of the tree with seven hearts.
  See the missing heart in feature 4.
- **In-fight hints**: a hit on bark or copper that does nothing gives a dull thud and no chips, so
  the contrast with a heart is heard at once (to check what a bark hit does today).
- A written book in Ruin loot from the builders, if the rest isn't enough. Least vanilla-like.

### D. More reasons to come back

The loop's last step, "return for more resin and hearts", is thin once the player has a mask.

- **More uses for the lens**: after the first mask a second kill gives nothing new. Candidates:
  the gaze block for redstone (parked below) made from a lens; a spyglass upgrade that outlines
  mobs in view; repairing a mask with a lens shard for a big chunk of durability.
- **A trophy**: a decorative drop from the tree with a low chance or from the challenge
  advancement, e.g. its dead heart as a block that pulses faintly, or a copper-banded pale log.
- **Something left behind**: after the death burst, a dead stump remains (decorative, or a
  one-time source of pale oak and the copper collar back).
- **Healing a Sentry by hand** with a resin clump, like an iron golem with ingots: resin gets a
  repeat use and guarding a base gets less fiddly than waiting for dawn.
- **Sentry quality of life**: name tags showing on the statue, a visible range marker when
  placing the pedestal, and whether a Sentry should follow orders at all (stay, or guard a wider
  radius). Decide what's vanilla-like before adding any.
- The power bow rule (a Power IV+ arrow cracks 2) is still open for both the Sentry and the tree.

### E. Art and audio

All of it is placeholder made in code or by script, and the user reviews but doesn't draw. To
replace:

- **Models**: the Elderwood (the part names in `ElderwoodModel` are the contract), its roots,
  the burning resin glob, the Pale Mask's 3D mask.
- **Textures**: the tree in four oxidation stages with its lens and heart overlays, the Sentry
  (awake, statue, crack stages, glowing face), the mask, every item (lens, shard, eye, both
  sherds, the trim template and its armor overlays), the petrification screen overlay, the mod
  icon.
- **Animation**: the tree only has code-driven branch poses and a shake. A real model would want
  an idle sway, a wind-up worth reading, and growth and decay made as animations.
- **Audio**: 15 Elderwood sound events have their own subtitles and play vanilla sounds; the
  Sentry and the mask use vanilla events directly. Possibly boss music while the bar is up.
- When the real tree model lands, delete `tools/elderwood_model.py` and `tools/png.py` (the
  placeholder generator) and their README note. `tools/pale_ruin.py` and `tools/nbt.py` stay: the
  Ruin's pieces are meant to be generated.

### F. Release

- **License**: still "All rights reserved"; needed before publishing anywhere.
- **Publishing**: Modrinth and CurseForge pages, versioning and a changelog, a release workflow
  next to the CI build, screenshots and the fight video.
- **Ids freeze at the first release**: until then renames cost nothing (as with Watcher to
  Elderwood); after it they need migration.
- **Translations**: only `en_us` exists.
- **Compatibility pass**: shaders and Sodium with the full-bright heart and lens layers and the
  JSON-loaded model; recipe viewers (EMI, REI, JEI) with the tag-based sticky piston recipe; the
  overridden vanilla recipe and tag files next to other mods that touch them.
- **Config**: none. The fight's numbers are constants; if packs should tune them, the natural
  place is data (like the tags are now), not a config screen.
- **Mixins**: four server-side ones today. Re-check each against Fabric API on every Minecraft
  update, since a new event would let one go.

### G. Housekeeping

- The Elderwood section above still carries the history of the Watcher rework ("kept", the
  build-order notes). Once the fight is stable, trim it to what the tree is now.
