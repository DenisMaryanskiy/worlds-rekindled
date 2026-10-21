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
  | Easy   | 3 | 5 |
  | Normal | 4 | 5 |
  | Hard   | 5 | 5 |

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

**The lensed Sentry** (built)

The Elderwood's lens is what gave the Ruin's guards their gaze. Set in a player's own Sentry, it
gives it the same gaze, turned on the mobs it fights: a second use for a lens, and a reason to
fell the tree again once the mask is made.

- **Setting it**: use an **Elderwood Lens** on a Heartwood Sentry, awake or a statue, that isn't
  hunting you. The lens is used up and sits high on its chest beside the heart (the heart fills
  the chest up to the shoulders, so there's no room above it), drawn as the lens item at full
  brightness, so a lensed Sentry is told apart at a glance, statue or not. One lens per Sentry;
  using a second does nothing.
- **Getting it back**: taking the Sentry apart (sneak + axe) returns the lens with the heart and
  the head, and a Sentry that dies drops it. Nothing else takes it out.
- **The gaze**: while it's awake, its gaze holds **the mob it's fighting**, within **12 blocks**,
  60 degrees to each side of where its head points, with a clear line, as a Ruin Sentry's does.
  The same thin trail of amber motes as the Elderwood's shows where it looks.
  - a mob it holds is **slowed** (Slowness II, 0.5 s after the gaze breaks), like under the Pale
    Mask's gaze;
  - held for **5 s** without a break, a mob is **petrified for 3 s**: Slowness VII, which takes
    all of a mob's speed (each level takes 15%), so it's rooted to the spot. Its next
    petrification takes another full 5 s hold, counted once this one is over. A status effect,
    so it wears off by itself and is saved with the mob; flying mobs move by their flying speed,
    which Slowness doesn't touch, so a phantom is only slowed;
  - bosses (`#vanillareforged:gaze_resistant`) are only slowed, with Slowness I and the drag, as
    under the mask, and never petrified.
- **Turned on a player**, as any Sentry is once that player hits it, the gaze petrifies them at
  a Ruin Sentry's rate (5 s), and the mask and a raised shield block it as they block the Ruin
  Sentries'.
- Its melee, its range and its heart are unchanged: the lens holds mobs for it, it doesn't make
  it tougher.
- Code: `SentryLens` (the gaze, also what a Ruin Sentry's gaze checks it sees with), the
  `lensed` flag on the Sentry, synced and saved; the slowing is the Pale Mask's own
  (`ElderwoodGaze.slow`) and the trail the Elderwood's (`ElderwoodLens.showTrail`).

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
- **The line**: a pavilion on the surface with a stairway 20, 40 or 60 down (see "It stays
  underground" below), four guard rooms, a second stairway 16 down and the hall, so the hall's
  floor lies about 37 below the surface at the least. Each room's exit is straight ahead, left
  or right, which makes the line wind.
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
  longer for it, the passages between rooms got shorter. Each of the four is one of eleven kinds,
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
  Sentries, a heart and a chest), and with its five chambers and three entrances the Ruin is 67 templates in all.
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
- **It stays underground.** Jigsaw sets the whole Ruin's height by the ground at its entrance
  and never looks at the ground over its rooms, which lie up to a hundred blocks away at a
  fixed depth below that entrance. On a hillside they came out of the slope, roofs and all.
  - The Ruin is its own structure type (`vanillareforged:pale_ruin`, `PaleRuinStructure`) that
    lays the Ruin out first and only takes a site where at least 3 blocks of ground lie over
    every piece but the entrance, by the sea floor's height at the piece's corners and middle
    (water over a room is no roof), and where the Ruin comes out whole.
  - Refusing alone would have left almost no Ruins: measured on real terrain, the ground around
    a Pale Garden falls about 15 blocks below the rooms' roofs somewhere along a typical Ruin,
    and 40 at the worst, and only 1 site in 20 passed.
  - So the Ruin comes **at three depths**, each with its own entrance: a stairway 20, 40 or 60
    down. A site gets the shallowest that stays buried. A deeper stairway is more flights
    doubling back under one another, ten steps each, not longer ones: the entrance keeps its
    footprint, and its middle stays under the pavilion.
  - Measured on one seed's real terrain, of 169 Pale Garden sites 59 took the stairway of 20,
    58 that of 40, 31 that of 60, and 21 were refused: 88% kept. Other seeds will differ.
  - It doesn't look for caves: a room can still open into one.

#### Side chambers and more rooms

> Built: the side doors, four plain chambers, the Sentry workshop, three more kinds of room,
> the hall's mural and the loot pass. Both playthroughs said the same thing: the Ruin is one
> corridor that is walked from end to end, with nowhere to step aside.

**Side chambers**

- **One side door in each of the four rooms**, in a wall its way on doesn't use, behind a short
  passage: four dead ends hang off the line. The line itself stays as it is.
- **Small**: about 9 x 9 inside and 5 high, lit like the rest.
- **No Sentries in them.** A Sentry only notices a player in its own room, so a chamber is a
  pocket to heal in or to break a gaze. One that is already hunting follows in.
- **What a chamber is depends on which room it hangs off**, so the two that teach something are
  always there and always in the same order:

  | Off room | Chamber |
  |---|---|
  | 1 | a store room, or a collapsed passage |
  | 2 | the study, or the tomb |
  | 3 | **the Sentry workshop**, always |
  | 4 | a store room, the tomb, or a collapsed passage |

  - **store room**: stacks of the builders' materials, pots, and a chest of supplies: bread,
    arrows, candles, ingots;
  - **collapsed passage**: a corridor that ends in fallen stone and roots after a few blocks,
    with one heap to brush, as good as the tomb's. A dead end that is only that;
  - **study**: a lectern, a few shelves, a desk with candles, and a chest that always holds an
    enchanted book;
  - **tomb**: the builders' own. Pots in niches and three heaps richer than the rooms', so
    archaeology has a place of its own.
- **How it's built**: the chamber is one more jigsaw piece on the room, 9 x 9 inside and 5 high
  behind a passage three blocks long (the collapsed passage is only the passage, nine long).
  A room's way on is tried before its side door, but its chamber still goes up before the rooms
  two on, and a chamber in their way would cut the line short. So which wall the door is in
  depends on the room's place in the line, the line's bearing there and the room's way on.
  There are 41 ways four rooms can wind without the line curling back; the walls were found by
  trying them all, and with them **every room gets its chamber and none is ever in the line's
  way**. Rooms 1 and 2 take the wall away from where the line still has to go. For rooms 3 and
  4 that rule let a few chambers collide with earlier ones, so theirs are set case by case.
- The GameTest that lays the Ruin out 300 times checks it with the game's own placement: all 41
  windings come up, the line is whole in each, and four chambers hang off it.
- The chambers are furnished as first versions. What they hold is under Rewards.

**The Sentry workshop**

Nothing in the game says a player can build a Sentry of their own from a pale oak log, a
Creaking Heart and a carved pumpkin. This chamber shows it, the way the workshop's unfinished
stump shows the Elderwood's.

- **Three plinths in a row**, read from the door:
  1. a pale oak log;
  2. a pale oak log with a Creaking Heart on it;
  3. a finished **Heartwood Sentry** standing on its log, pumpkin head and all: the builders'
     own, still at its post. It's a player's kind of Sentry, so it leaves the player alone, and
     it goes for the Creakings that walk the Ruin at night.
- **A carved pumpkin** lies on the bench, beside a chest that always holds **shears and a carved
  pumpkin** (with seeds, resin and a log or two), and pumpkins grow on along the other wall:
  the one part of the recipe the Ruin hasn't shown yet, and where to get it.
- **The second plinth is one pumpkin short of a Sentry**: putting the bench's pumpkin on its
  heart builds one, there and then.
- **A mural** on the wall behind the plinths, in blocks on a calcite panel: a log, the heart on
  it, a carved pumpkin on top. Its heart is chiseled resin brick. A real heart under a real
  pumpkin is the pattern itself, and the wall would build a Sentry.
- **The Guardian sherd**, a third pottery sherd: a figure with a pumpkin head and a heart in its
  chest. The chamber's two pots show it and give it when broken; placeholder texture like the
  other two. It isn't among the finds in the gravel.
- The Sentry can be taken apart with sneak and an axe like any player's, which gives back its
  heart and pumpkin: a second heart for whoever understood the room.
- Built. A Sentry that a structure or a command sets down takes the log under it for its
  pedestal on its first tick; before, only one built by hand or from an egg had one. A GameTest
  places the chamber and checks there is exactly one Sentry in it, on its log.
- Awake underground, the Sentry doesn't stay on its plinth: it strolls the chamber and comes
  back to its log. To see in play whether that reads as "the third step" or needs it kept still.

**More kinds of room**

Three more for the four rooms of the line, eleven in all:

- **shrine**: this was a temple. In the middle stands what they prayed to, the tree in small:
  a trunk of pale oak on a stepped base with a heart of resin in it and lit candles at its
  foot. Benches face it from both sides; they are low cover, the aisles are open. (A relief on
  the far wall, as first planned, would have stood in whichever wall the way on needs.);
- **foundry**: where the copper for the bands, cages and lens frames was worked. Furnaces along
  one wall, anvils, stacks of copper in every stage of weathering, two columns, and a band left
  half cast on the floor: three sides of a square, the fourth never poured;
- **collapsed room**: part of the ceiling is down. Two ridges of fallen stone cross the room
  from opposite walls, each with a gap at its far end, so the way across winds and nothing
  looks from one end to the other. All four doors stay reachable.

Built, as first versions.

**The hall's mural**: on the wall behind the stump, the tree on a calcite panel with its lens,
which glows, and its seven hearts: four in the trunk, one on each branch, the core under the
lens. The hearts are resin blocks: seven real ones in a wall would be seven to carry off. Built.

**Build order**: (1) the side doors and the four plain chambers, with the layout test, done;
(2) the Sentry workshop and the Guardian sherd, done; (3) the three kinds of room and the hall's
mural, done; (4) the loot pass (see Rewards), done. A playthrough after all four.

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
  lured away from it.
- Implemented, as its own entity (`ruin_sentry`, with a spawn egg) sharing the Heartwood Sentry's
  heart, cracks, healing and melee. Notes:
  - **the gaze** fills the meter by **1 a tick** (the Elderwood: 2, or 3 with its core open), so
    5 s of being watched petrifies. It reaches **12 blocks**, 60 degrees to each side of where
    its head points, and needs a clear line: a column breaks it. It only holds its target.
    **Gazes don't add up**: of several that hold a player, only the strongest counts, so two
    Sentries in a room petrify no faster than one;
  - **the leash** is **its Ruin**: every piece of the structure its post lies in, the stairway
    and the pavilion included, and 2 blocks into the rock around them, so a niche dug into a
    wall is no way out. Anywhere in there it picks, keeps and follows a target; outside it
    doesn't. A Sentry that stands in no Ruin (spawn egg, command) guards **16 blocks** around
    its **post**, the spot where it first stands. Its first leash was that circle in the Ruin
    too, which ended a few steps into the corridor of its own room: from there a player could
    kill it while it stood and watched;
  - **a hit from beyond its leash bounces off** its heart, as one from sunlight does: either it
    comes for whoever hits it, or the hit does nothing;
  - **it only notices a player in its own room**. Once it hunts, it follows through the whole
    Ruin, and it comes for whoever hits it from anywhere in it. With the whole Ruin as its
    leash it first noticed players there too, and the Sentries of several rooms came down one
    corridor as a pack of three;
  - **with nobody to hunt it walks back to its post**, wherever in the Ruin the hunt took it;
  - **its blow** deals **4 damage** (the player's Sentry: 6), about a heart through iron armor.
    A blow that lands on a **petrified** player **breaks the stone**: it hurts 50% more, and
    then the player is free, with the 3 s in which no gaze takes hold. Before, three blows
    landed in the 3 s of being stuck. It **doesn't throw the player up** as the player's own
    Sentry throws a mob: thrown, they took the next blow in the air, from it or from the Sentry
    beside it;
  - **cracks**: 3 on Easy and Peaceful, 4 on Normal, 5 on Hard, as in the table above (one
    fewer since the third playthrough, in iron armor with a shield, still found them tough). A
    **critical hit** (a sword swung while falling) **cracks it twice**, on the player's own
    Sentry too. The Elderwood's hearts take one crack a hit either way;
  - **drops**: 1-3 resin clumps, 1-2 Elderwood Lens Shards and 10 XP;
  - it isn't a monster by vanilla's rules (a golem, like the player's Sentry), so it stays on
    Peaceful, where it stands guard without attacking, and a player's Sentry or an iron golem
    doesn't go for it. The Pale Mask's gaze slows it whether or not it's hunting the wearer;
  - **look**: the Heartwood Sentry's model with a bark head and one eye, a lens shard that always
    glows. Placeholder textures.
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
    placeholder textures; vanilla already has a "heart" sherd, hence the names. A third, the
    **Guardian** (a pumpkin-headed figure with a heart), is on the pots of the Sentry workshop.
- The **Buried Garden** advancement for entering it.

Placed so (loot tables under `chests/pale_ruin/`, `pots/` and `archaeology/`):

- **The hall's chest** stands before the stump, between two pots that show the Creaking sherd
  (the tree with its heart) to the door. It always holds **one Creaking Heart** and **two lens
  shards**, with 4-6 stacks of resin bricks and clumps, copper blocks, emeralds and pale oak
  saplings, **2-3 valuables**, and two times in three **1-3 diamonds** (see Richer chests).
- **The tool chest**, in the second room, whose root carries a heart: always a **worn iron axe
  with Silk Touch** and a **brush**, and supplies (resin, copper and iron ingots, candles,
  bread, now and then a diamond). It is the only chest in the line: the line is for fighting, the chambers for looting.
- **The chambers' chests**: the store room's holds supplies (bread, arrows, candles, ingots,
  resin, a few logs), the study's always an **enchanted book** with books and paper, the Sentry
  workshop's the shears and the pumpkin.
- **Pots**: two in every room, more in the chambers, each with one stack: resin, copper, or
  emeralds. A pot broken with a tool gives the sherds on its sides, so only some show one: one
  of the two in every room (the **Gaze** sherd in the guard room, the cells, the lens room, the
  archive and the foundry; the **Creaking** sherd in the others), two of the store room's four,
  one of the study's two, two of the tomb's six, both of the hall's, and the Sentry workshop's
  two, the only ones with the **Guardian**. The rest are plain.
- **Heaps to brush**: suspicious gravel lying on the floor with rubble beside it (in the floor
  it could fall through a cave under the Ruin).
  - One in every room: 2 of 12 finds are a sherd, the rest resin, copper, emeralds and odds
    and ends. The pots are where the rooms' sherds are.
  - Three in the tomb and one in the collapsed passage: 6 of 14 are a sherd, the rest
    emeralds, bones, resin and copper. One in the study, like the rooms'.
  - Four in the hall's corners. **The one by the mural, right of it as one faces it, always
    holds the Elderwood armor trim.** The other three: 6 of 10 a sherd, 1 a lens shard, the
    rest emeralds and a diamond.
- **The brush** is in the tool chest because nothing else in the Ruin gives one, and gravel
  broken without it gives nothing.

**What the loot pass changed**, after two playthroughs:

- **Fewer pale oak logs.** There's a forest of them overhead. They left the chests but for a
  few in the store room and the Sentry workshop, where a log is a part of the pattern.
- **No amethyst.** It was meant as what a lens is ground from, but a lens is made of shards and
  nothing in the game says otherwise. Copper and resin took its place.
- **The fourth room's chest is gone**, and one of every room's two heaps: what they held is in
  the chambers.
- **The trim is certain.** It was 2 of 12 finds in each of the hall's four heaps, so about half
  of all Ruins had none, and two playthroughs never saw it.
- **The chambers are random, so what they hold is too**: one Ruin in three has no store room,
  one in three no tomb, one in two no study. Nothing a player needs is in them: the axe, the
  brush, the heart, the shards, the trim and the shears are where every Ruin has them.

**Richer chests** (built). For the hardest structure in the mod the chests held little worth
taking home: one diamond in the whole Ruin now and then, iron by the one to three. The hall's
chest became a treasure chest worth the fight, and the supply chests carry real metal:

- **The hall's chest** keeps its heart, its shards and its resin and copper, and gains:
  - **2-3 valuables**: iron ingots (4-10), gold ingots (3-8), emeralds (3-8), a golden apple, a
    diamond pickaxe or sword enchanted at levels 20-39, and rarely an enchanted golden apple
    (weights 6, 5, 4, 2, 1, 1, 1);
  - **1-3 diamonds two times in three**, from a pool of their own. Among the valuables at the
    weight first planned (4 of 24, rolled 2-3 times), only one chest in three had a diamond,
    not two as intended.
- **The tool chest**: iron ingots 2-5 (was 1-3), and a diamond now and then.
- **The store room**: iron ingots 2-6 (was 1-3), gold ingots now and then, and rarely a diamond.
- The study, the workshop and the gravel stay as they are.

**Things only the Ruin has** (planned), so a second Ruin is worth clearing:
- **A banner pattern**, the lens's eye, like the Trial Chambers' Flow and Guster: in the study's
  chest one time in three and the hall's one time in four.
- **A painting**, the tree with its seven hearts as the mural shows it, in the hall's chest one
  time in three. Paintings are data in 26.3; this one isn't placeable at random, so it's only
  found. Placeholder art for both.

**A map to it** (built). The Pale Garden is the hardest biome to find. The **Cartographer**
sells a **Pale Ruin Map** at Journeyman (level 3), for **14 emeralds and a compass**, beside its
Ocean Monument and Trial Chambers maps, as an explorer map of its own:
- in 26.3 trades are data: the trade is `vanillareforged:cartographer/3/emerald_and_compass_pale_ruin_map`,
  added to vanilla's `#minecraft:cartographer/level_3` trade tag, and leads to
  `#vanillareforged:on_pale_ruin_maps`;
- like the other explorer maps it searches **100 chunks** around the villager, and the trade is
  dropped when no Ruin lies within reach, so in a world without a Pale Garden near the village
  it never shows up. The reach counts the structure's placement regions, not plain chunks: on
  one real-terrain seed, the sixth Journeyman tried offered it, and the map led to a Ruin about
  4,300 blocks from spawn;
- the map is an item of its own (`pale_ruin_map`, as vanilla's maps are now) with its own mark,
  the `vanillareforged:pale_ruin` map decoration. Placeholder art for both: vanilla's Trial
  Chambers icon recoloured pale, with resin for its copper.

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
  keep it for the whole fight, and the stage sets its tier (see Tiers below). A tree that never
  had a collar, from a spawn egg or a command, is the plain copper tier.
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
  its lens and 2–4 resin clumps (kept), and its core as a trophy (see the craft chain).

**Petrification** (kept; shared with Ruin Sentries)
- A **meter**, like vanilla freezing in powder snow, not a status effect: milk clears all effects,
  which would turn every fight into "drink milk".
- It **fills while the lens has line of sight** to the player and drains once sight is broken.
  The player slows down more and more, and when it's full they're **petrified**: stuck in place.
- **Brittle**: damage taken rises with the meter, up to about **+50%** when petrified (you're
  slowly turning into a statue). Not fire: stone burns no better, and burning already goes
  through armor on a player who can't move to put it out.
- A grey, cracked stone overlay creeps in from the screen edges as it fills, like powder snow's
  frost overlay.
- Implemented (players only; creative and spectators are immune):
  - the meter runs from 0 to 100; each source decides how fast it fills it per tick;
  - movement only slows once the meter is half full, then by up to **60%**; when full the player
    is petrified for **3 s** (no movement, no jumping) even if the gaze breaks;
  - several gazes at once don't add up: only the strongest fills the meter;
  - cracking free (or being shattered by a slam or a Ruin Sentry's blow) empties the meter and
    gives **3 s in which no gaze takes hold**. Before, the player came out half full and still in the gaze, so in the
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
- **Root eruption** (new): within 24 blocks, as far as the lens sees, **every 4 s**, against a
  player its branches don't deal with: one it can't see (behind, to the side or behind cover),
  and one it sees beyond the slam's reach of 6. The ground under the player cracks for 1 s, then
  roots burst out: 6 damage (the brittle bonus applies), a toss into the air, and a petrified
  player's stone breaks, as under the slam. Moving out of the cracked patch dodges it, like
  evoker fangs. It keeps circling, camping and standing back from being free.
  - **After the third playthrough**: roots first reached 12 blocks and only came for a player
    the lens couldn't see. The hall's corners, about 15 from the trunk, were out of their reach,
    and where the colonnade stood in the lens's line nothing reached a player there at all. And
    a player the lens saw at range had only the gaze and the resin to face: one stood by a
    column, petrified, hitting every glob back, and the fight stalled. Up close, meanwhile,
    slam, gaze and roots all came at once, so playing aggressively was what got punished.
    Roots now come at range too, and their stone-breaking hit frees the statue, which then has
    the 3 s in which no gaze takes hold to move before the next.
  - **Armor takes its share**: the roots have their own damage type
    (`vanillareforged:roots`). They were magic at first, which goes straight through armor: 3
    hearts a strike whatever the player wore, 4.5 on a petrified one, every 2.5 s. Through iron
    armor it's now about 1.5 and 2.5. A shield doesn't stop them: they come from below.
- **Burning resin** (kept): at a player in the gaze cone beyond the slam's reach it throws a glob
  of burning resin from the lens, the first after 1 s and then every 3 s. A hit deals **5 damage
  and 5 s of fire**; blocks never catch fire. Its own look: a molten amber glob with a white-hot
  core, without vanilla's fire overlay.
- **Hitting the resin back** (kept): a melee hit sends it where the player looks, like a ghast's
  fireball. Hit back into the Elderwood it does no damage but **stuns it for 3 s**; only resin a
  player hit back does. For 5 s afterwards it can't be stunned again. Coming out of the stun it
  waits a full 3 s before its next glob: thrown at once, that glob punished the player who had
  just earned the stun. Resin a player hit back **never hits that player**: vanilla lets it
  strike anyone at once, so one hit back as it passed their head flew into it and burned them
  as their own.
- **Stunned**: it doesn't turn, gaze, slam, throw or raise roots, its lens goes dark and its
  branches droop, so **both branch hearts open** to arrows and tridents (still too high for a
  sword). This is how a ranged player opens them without baiting a slam.
- **Falling dormant**: with no player within 32 blocks for **90 s** it stops, and its hearts heal
  one crack every few seconds, destroyed ones included. Walking away resets the fight; it doesn't
  let the player whittle it down from safety. Its lens is dark while it's dormant. At first it
  was 30 s, which is less than dying and running back from the Ruin's entrance takes: every
  death undid the fight.
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

A lens can also go into a player's own Sentry instead, which gives it a gaze of its own (planned,
see [The lensed Sentry](#2-heartwood-sentry-golem)).

**The Elderwood Core** (built): the trophy. The heart that broke last, under the lens, dropped
on **every kill**, as the Wither always drops its star and the Elder Guardian its sponge.
- A block: the core's resin cracked through and dull, with a glow that pulses slowly in its
  cracks (an animated texture, a 2 s pulse) and a faint light (level 5). Placed and mined like a
  Creaking Heart, best with an axe, and it drops itself with any tool, or none; an explosion
  destroys it. Rare (aqua name), next to the Creaking Heart in the Natural Blocks tab.
- Decorative only: it does nothing and goes into no recipe, so it stays a trophy. Placeholder
  texture: vanilla's Creaking Heart, dulled, fading between its dormant and awake looks.
- It's the mod's first block (`ModBlocks`).
- **One Core per tier** (built): the Elderwood Core, and the Exposed, Weathered and Oxidized
  Elderwood Core, its face rimmed in that stage's copper (placeholder: a one-pixel border of
  vanilla's copper block texture). Four blocks that never weather; a full set takes a kill at
  every tier.

**Tiers: the collar sets the fight** (built). The copper on the stump used to only show on the
tree. It's the difficulty dial now, so the same stump gives four fights, each harder and better
paid than the last, as Ominous trials are to plain ones:

| Collar | The fight | Drops, besides its Core and the resin |
|---|---|---|
| Copper | as playtested | 1 lens |
| Exposed | roots every 3.5 s, turns 15% faster | 1 lens, 2-4 lens shards |
| Weathered | roots every 3 s, resin every 2.5 s, turns 30% faster, one more crack on every heart | 2 lenses |
| Oxidized | as weathered, and its gaze fills 50% faster (3 a tick, 4 with its core open) | 2 lenses, 4-6 lens shards |

- Waxed copper counts as its stage. An axe scrapes a stage off before the heart goes on, so the
  player picks the tier.
- **The Ruin's own stump has waxed plain copper**, so the first fight, the one the Ruin builds
  up to, is the lowest tier, as playtested, however long the Ruin waits. Its cap was oxidized
  copper, which would have made it the hardest.
- The numbers are first values, for the playthroughs to tune.
- **Verdigris**, a challenge advancement, for felling it with an oxidized collar (feature 6).
- Code: `ElderwoodTier`, one per stage, holds the tier's numbers and its loot table
  (`entities/elderwood`, `elderwood_exposed`, `elderwood_weathered`, `elderwood_oxidized`). The
  constants on `Elderwood` and `ElderwoodLens` are the plain copper tier's. The boss bar counts
  the tier's cracks, so it still empties exactly.

**More players, more tree** (planned). With more players it's an easy fight shared out and a
single lens for the group. The players within 24 blocks when it wakes set it, up to four:
- every heart takes **one more crack per player past the first**;
- every player who cracked a heart during the fight gets **a lens of their own**, dropped where
  the trunk stood with the rest. Shards, Core and resin stay one set.

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

Eight advancements in vanilla's **Adventure** tab (no tab of our own, like a Mojang update would).
Their descriptions double as the hints the mod otherwise lacks.

| Advancement | Earned by | Frame | Trigger |
|---|---|---|---|
| **Buried Garden** | Entering a Pale Ruin | task | vanilla `location`, inside the structure |
| **Old Growth** | Waking the Elderwood | goal | vanilla `summoned_entity`, which the summon fires for players within 8 blocks |
| **Heartwood Guardian** | Building a Heartwood Sentry | task | vanilla `summoned_entity`, which the build fires for players within 5 blocks, as an Iron Golem's does |
| **Set in Stone** | Being fully petrified and living through it | task | `vanillareforged:survived_petrification` |
| **Deadwood** | Killing the Elderwood | goal | vanilla `player_killed_entity`: the core's last crack is credited to whoever dealt it |
| **One of Them** | Wearing a Pale Mask next to a Creaking that ignores you | task | `vanillareforged:ignored_by_creaking` |
| **Don't Blink** | Killing the Elderwood without being petrified once | challenge, 100 XP | `vanillareforged:killed_elderwood_unpetrified` |
| **Verdigris** | Killing the Elderwood woken with an oxidized collar | challenge, 100 XP | `vanillareforged:killed_oxidized_elderwood` |

- **Tree**: Old Growth hangs off vanilla's **Heart Transplanter** (placing a Creaking Heart), so
  its description, which spells out the stump, shows up for anyone who has started the Adventure
  tab. It leads to Set in Stone and Deadwood, and Deadwood to One of Them, Don't Blink and
  Verdigris.
  Buried Garden hangs off Heart Transplanter too, beside it, and so does Heartwood Guardian,
  whose description spells out the Sentry's pattern for anyone who has placed a heart, as the
  Sentry workshop shows it in the Ruin.
- **Triggers**: the four of our own are plain vanilla `PlayerTrigger`s registered under our ids
  (`ModCriteria`), like vanilla's `avoid_vibration`: the code decides when they fire.
- **Set in Stone** is granted when the stone cracks open, by time or by a hit, if the player is
  still alive then. A meter that never filled doesn't count.
- **One of Them** needs the Creaking to be one that would otherwise attack (so not in Creative)
  and within **4 blocks**.
- **Don't Blink**: the tree remembers whom its gaze has petrified, saved with it so relogging
  doesn't clear it, and forgets once it has gone dormant and mended every crack: that's a new
  fight. Only the player who lands the last crack is checked and rewarded, like Deadwood. A Pale
  Mask makes it easy, which fits: that's the second kill.

**Vanilla's own advancements** count what the mod adds where vanilla would, through overrides
of their files in `data/minecraft/advancement/adventure/`. Each is vanilla's file with criteria
added, so another mod that overrides the same file wins or loses as a whole.

| Advancement | Now also counts |
|---|---|
| **Monster Hunter** (kill any monster) | a Ruin Sentry, the Elderwood |
| **Monsters Hunted** (kill one of every monster) | a Ruin Sentry and the Elderwood, both required, as vanilla requires its bosses and the Creaking |
| **Crafting a New Look** (trim any armor) | the Elderwood trim |

- Left alone: **Hired Help** names the Iron Golem and counts nothing else, not even the Snow
  Golem, so a Heartwood Sentry doesn't grant it. **Smithing with Style** asks for eight of vanilla's rarest trims, not all of them;
  adding ours would only make it harder. **How Did We Get Here** counts status effects, and petrification is a
  meter like powder snow's freezing, which isn't on it either.

### 7. More for resin (planned)

Resin is the Pale Garden's own resource, and besides the bricks the mod only asks for it to
summon the tree and repair the mask. Two steady uses, so a resin farm under a Pale Mask among
the Creakings has something to feed:

- **Resin glass**: 8 glass around a block of resin give 8 resin glass, as dye makes stained
  glass; 6 of it give 16 panes. Translucent amber, the lens's own glass. Placeholder texture.
- **Healing a Sentry by hand**: a resin clump used on a Heartwood Sentry mends one crack, as iron
  ingots heal an Iron Golem. Any time but while it's hunting that player; not on Ruin Sentries.

## Parked for later (warrior content and extras)

- **Resin Tappers**: illager-type faction that farms resin from captive hearts, uses resin traps.
- Ambient threat: moths drawn to open eyeblossoms at night.
- Gaze block for redstoners: outputs a signal while a player looks at it. Dropped by decision,
  as the Watcher (a lens block that sees mobs) was.
- Other resin uses: slowing resin-coated blocks, amber preservation displays, tool handle upgrade.

## What's left

Nothing in this section is implemented or agreed in detail. It's the backlog: ideas to pick from,
roughly in the order they'd pay off.

### A. Advancements

Seven are in (feature 6), and three of vanilla's count the mod's mobs and trim. Nothing left
planned.

### B. Finish what's on hold

- **The Pale Ruin** (feature 4): it generates, with its Sentries and its loot. Left: how often
  it generates; a third playthrough from the pavilion to the Elderwood. The
  first, in iron armor without a shield, ended in three deaths to the Sentries and four to the
  Elderwood; the Sentries were eased after it (gazes that don't add up, a blow that breaks the
  stone, 4 damage, the leash to the whole Ruin), and so was the Elderwood (its drawn trunk no
  longer trails the real one, roots that armor reduces and that come every 4 s, 90 s before it
  mends, a pause before its next resin after a stun). The second, with the same gear, took
  two deaths to the Sentries and three to the Elderwood, in half the time at the boss; after it
  the Sentries stopped throwing the player up and noticing them from other rooms, a critical
  hit came to crack a Sentry's heart twice, and fire lost its bonus on a petrified player.
- **Left as it is after the second playthrough**, to look at again if it still hurts: the
  Elderwood's resin burns for 5 s and comes every 3 s, so in its gaze the fire never goes out;
  its gaze covers 60 degrees to each side.
- **Next for the Ruin**: a third playthrough. Its side chambers, the Sentry workshop, three
  more kinds of room, the hall's mural and the loot pass are in. They answer what both
  playthroughs found: one long corridor with nowhere to step aside, too many pale oak logs in
  the chests, pots that all look alike, amethyst that doesn't explain itself.
- **Left as vanilla has it**, by decision: at night the rooms' Creakings come on top of the
  Elderwood, and dying deep in the Ruin leaves the gear under its Sentries. Both are the game's
  own rules at work.
- **A third playthrough** once the Ruin's new rooms are in: the changes after the second (no
  throw, no pack, critical hits, hit-back resin) haven't been played yet. The demo video
  predates all of it and should be re-recorded once the numbers settle.
- **Elderwood numbers**: cracks per heart, turn speed, root damage, stun length, gaze fill rate,
  resin damage and the summon's resin cost are first guesses.
- **Difficulty**: the Sentry's cracks scale with difficulty, the Elderwood's nothing does.
  Candidates: gaze fill rate, slam damage, root interval. Also what Peaceful means for it (it's
  a monster, so it would vanish; refusing to wake may read better).
- **Multiplayer**: whom it targets and turns to with several players around, and the gaze
  holding only one of them. More cracks and a lens each are planned (feature 5).
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
- **The sherds and the Ruin tell it**: the Creaking sherd already shows a tree with a heart. The
  Sentry's pattern has a chamber of its own, the Sentry workshop, and the tree with its seven
  hearts a mural in the hall (feature 4).
- **In-fight hints**: a hit on bark or copper that does nothing gives a dull thud and no chips, so
  the contrast with a heart is heard at once (to check what a bark hit does today).
- A written book in Ruin loot from the builders, if the rest isn't enough. Least vanilla-like.

### D. More reasons to come back

The loop's last step, "return for more resin and hearts", is thin once the player has a mask.

**Planned**, a reason to return for every kind of player, each its own PR, in this order:

| # | What | For | Where |
|---|---|---|---|
| 1 | The lensed Sentry (built) | fighters, base guards | feature 2 |
| 2 | The Elderwood Core (built) | collectors | feature 5 |
| 3 | Richer chests (built) | everyone | feature 4, Rewards |
| 4 | Tiers, a Core per tier, Verdigris (built) | fighters, collectors | features 5 and 6 |
| 5 | The Pale Ruin explorer map (built) | explorers | feature 4, Rewards |
| 6 | Resin glass, healing a Sentry by hand | builders, farmers | feature 7 |
| 7 | The banner pattern and the painting | collectors, explorers | feature 4, Rewards |
| 8 | More players, more tree | multiplayer | feature 5 |

What makes each a reason to come back: a lens per Sentry, four tiers and a set of four Cores,
a map to every next Ruin, and resin that keeps being used.

- **Dropped by decision**: a spyglass set with a lens that outlines mobs; the felled tree leaving
  its stump and collar behind; a lens block for redstone that sees mobs (the Watcher).
- **Other lens uses**, not planned: repairing a mask with a lens shard for a big chunk of
  durability.
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
