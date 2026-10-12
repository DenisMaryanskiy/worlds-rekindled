"""Generates the Pale Ruin: its structure templates and the worldgen files that chain them.

The Ruin is a line of jigsaw pieces: the entrance on the surface with its stairway down, ROOM_SLOTS
rooms, a second stairway down and the hall. Every slot has its own pools, so the hall always ends
the line. A slot's room is one of several kinds, and its exit is straight ahead, to the left or to
the right, so every Ruin is furnished and winds differently. Off every room hangs a side chamber,
a dead end with no guards in it.

Jigsaw placement doesn't backtrack: a piece that doesn't fit is left out, with everything after it.
So the line must never curl back on itself. Each room is made once per bearing the line can have
by then (still ahead, or turned once to the left or right of the stairway), and only with the exits
that keep it within one turn of ahead. The hall is twice as wide as a room and would still clip
the rooms it comes back alongside, so it lies a level below them all.

Run from the repository root:  python3 tools/pale_ruin.py
"""
import collections
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402

DATA_VERSION = 5023
NAMESPACE = "vanillareforged"
DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", NAMESPACE)

# What a room holds besides its furniture: how many Ruin Sentries, whether it has a natural Creaking
# Heart, and which chest if any. The tool chest has the builders' Silk Touch axe, for taking such a
# heart whole.
VARIANTS = {"plain": (2, False, None), "tools": (1, True, "tool_chest"), "heart": (2, True, "room")}
# The four rooms in order: 7 Sentries in all.
ROOM_VARIANTS = ["plain", "tools", "plain", "heart"]
ROOM_SLOTS = len(ROOM_VARIANTS)
# The stairway comes at these depths: a site gets the shallowest that keeps the Ruin under the
# ground, and around a Pale Garden the ground often falls a long way (see PaleRuinStructure).
STAIR_DEPTHS = [20, 40, 60]
FLIGHT = 10
# How far below the first free block above the ground the pavilion's floor lies: 1 is level with it.
SUNK = 1
# The hall's ceiling stays below the rooms' floors, whichever way the line turned.
DESCENT_DEPTH = 16
# For each bearing of the line so far, the exits a room may have and the bearing each leaves it on.
TURNS = {
    "ahead": {"straight": "ahead", "left": "left", "right": "right"},
    "left": {"straight": "left", "right": "ahead"},
    "right": {"straight": "right", "left": "ahead"},
}

# The wall of a room its side chamber hangs off, by the room's slot, the line's bearing there and
# the room's exit. A room's chamber is placed before the rooms two on, so a chamber in their way
# would cut the line short. These walls were found by trying every way the line can wind (41):
# with them every chamber fits and none is ever in the way. Rooms 1 and 2 take the wall away from
# where the line still has to go; for 3 and 4 that rule let a few chambers collide with earlier ones.
def side_door(slot, bearing, exit_side):
    if slot >= 3 and bearing != "ahead":
        return {(3, "left", "straight"): "west", (3, "left", "right"): "south", (3, "right", "straight"): "east",
                (3, "right", "left"): "west", (4, "left", "straight"): "west", (4, "left", "right"): "south",
                (4, "right", "straight"): "east", (4, "right", "left"): "south"}[(slot, bearing, exit_side)]
    if slot < 3 and bearing != "ahead":
        return "east" if bearing == "left" else "west"
    if exit_side == "straight":
        return "east" if slot != 2 else "west"
    return "west" if exit_side == "left" else "east"


# Which chambers a room's side door may lead to, by the room's slot.
CHAMBERS = {1: ["store_room", "collapsed_passage"], 2: ["study", "tomb"], 3: ["sentry_workshop"],
            4: ["store_room", "tomb", "collapsed_passage"]}

AIR = "minecraft:air"
TUFF_BRICKS = "minecraft:tuff_bricks"
CHISELED_TUFF = "minecraft:chiseled_tuff_bricks"
POLISHED_TUFF = "minecraft:polished_tuff"
DEEPSLATE_BRICKS = "minecraft:deepslate_bricks"
DEEPSLATE_TILES = "minecraft:deepslate_tiles"
CALCITE = "minecraft:calcite"
COPPER_BAND = "minecraft:oxidized_chiseled_copper"
LANTERN = "minecraft:waxed_oxidized_copper_lantern"
CHAIN = "minecraft:waxed_oxidized_copper_chain"
ROOT = "minecraft:pale_oak_wood"
MOSS = "minecraft:pale_moss_block"
MOSS_CARPET = "minecraft:pale_moss_carpet"
HANGING_MOSS = "minecraft:pale_hanging_moss"

# Vanilla only ticks a Creaking Heart that isn't "uprooted", and works that out when one is placed
# by hand. A structure's heart skips that, so it has to start in a state that ticks: from there
# the game wakes it by night and puts it to sleep by day.
ASLEEP = "dormant"

WALLS = [(TUFF_BRICKS, 1)]
DARK_WALLS = [(DEEPSLATE_BRICKS, 5), ("minecraft:cracked_deepslate_bricks", 1)]
FLOOR = [(DEEPSLATE_TILES, 7), ("minecraft:cracked_deepslate_tiles", 2), ("minecraft:cobbled_deepslate", 1)]
RUBBLE = [("minecraft:cobbled_deepslate", 3), (DEEPSLATE_BRICKS, 1), ("minecraft:tuff", 2)]

LIGHT = {LANTERN: 15}
# Blocks light passes through; everything else a piece places stops it.
BARS = "minecraft:waxed_oxidized_copper_bars"
CUT_COPPER = "minecraft:waxed_oxidized_cut_copper"
CLEAR = {AIR, LANTERN, CHAIN, MOSS_CARPET, HANGING_MOSS, "minecraft:candle", "minecraft:damaged_anvil",
         "minecraft:chipped_anvil", "minecraft:pumpkin_stem",
         "minecraft:attached_pumpkin_stem", "minecraft:tuff_brick_wall", "minecraft:tuff_brick_slab",
         "minecraft:tuff_brick_stairs", "minecraft:closed_eyeblossom", "minecraft:decorated_pot", "minecraft:chest",
         BARS, "minecraft:cobweb", "minecraft:orange_stained_glass", "minecraft:grindstone", "minecraft:amethyst_cluster",
         "minecraft:resin_clump", "minecraft:cauldron", "minecraft:pale_oak_sapling", "minecraft:skeleton_skull",
         "minecraft:lectern", "minecraft:jigsaw"}
# What can't be set on air or on a half slab.
NEEDS_A_FLOOR = {"minecraft:candle", MOSS_CARPET, "minecraft:pale_oak_sapling", "minecraft:closed_eyeblossom",
                 "minecraft:pumpkin_stem", "minecraft:attached_pumpkin_stem", "minecraft:decorated_pot", "minecraft:skeleton_skull"}
# What loot may be put down on: nothing that holds anything up.
SOFT = {AIR, MOSS_CARPET, HANGING_MOSS, "minecraft:closed_eyeblossom"}
# Blocks a mob can't stand on although they're in the way of light too little to matter.
NOT_A_FLOOR = CLEAR | {"minecraft:jigsaw"}

ENTRY = f"{NAMESPACE}:pale_ruin/entry"
ANCHOR = f"{NAMESPACE}:pale_ruin/anchor"
EMPTY = "minecraft:empty"


class Piece:
    def __init__(self, name, size):
        self.name = name
        self.size = size
        self.blocks = {}
        self.entities = []
        self.random = random.Random(name)

    def set(self, x, y, z, block, nbt_data=None, **properties):
        assert all(0 <= c < s for c, s in zip((x, y, z), self.size)), (self.name, x, y, z)
        self.blocks[(x, y, z)] = (block, tuple(sorted(properties.items())), nbt_data)

    def fill(self, x1, y1, z1, x2, y2, z2, block, **properties):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.set(x, y, z, self.pick(block), **properties)

    def pick(self, block):
        """A block id, or a weighted choice from a list of (id, weight)."""
        if isinstance(block, str):
            return block
        return self.random.choices([b for b, _ in block], [w for _, w in block])[0]

    def block(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state, priority=0):
        """A higher priority is tried earlier among a piece's jigsaws, and what it places goes on first."""
        self.set(x, y, z, "minecraft:jigsaw", {
            "id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final_state,
            "joint": "aligned", "selection_priority": priority, "placement_priority": priority,
        }, orientation=orientation)

    def entry(self, x, z, y=0, final_state=DEEPSLATE_TILES):
        """Where the piece before this one attaches: in the floor of the doorway on its north side."""
        self.jigsaw(x, y, z, "north_up", ENTRY, EMPTY, EMPTY, final_state)

    def exit(self, x, z, facing, pool, final_state=DEEPSLATE_TILES):
        # The line goes on before anything hangs off it.
        self.jigsaw(x, 0, z, facing + "_up", f"{NAMESPACE}:pale_ruin/exit", ENTRY, pool, final_state, priority=2)

    def side(self, x, z, facing, pool):
        self.jigsaw(x, 0, z, facing + "_up", f"{NAMESPACE}:pale_ruin/side", ENTRY, pool, DEEPSLATE_TILES)

    def entity(self, x, y, z, entity_id):
        self.entities.append({"pos": [x + 0.5, float(y), z + 0.5], "blockPos": [x, y, z], "nbt": {"id": entity_id}})

    def chest(self, x, z, facing, loot):
        assert self.block(x, 1, z) in SOFT, (self.name, x, z)
        self.set(x, 1, z, "minecraft:chest", {"id": "minecraft:chest", "LootTable": f"{NAMESPACE}:chests/pale_ruin/{loot}"},
                 facing=facing, type="single", waterlogged="false")

    def pot(self, x, y, z, facing, front="minecraft:brick"):
        """A decorated pot with something in it; {@code front} is the sherd on the side it faces."""
        assert self.block(x, y, z) in SOFT, (self.name, x, z)
        sherds = {side: {"id": front if side == "front" else "minecraft:brick"} for side in ("back", "front", "left", "right")}
        self.set(x, y, z, "minecraft:decorated_pot",
                 {"id": "minecraft:decorated_pot", "LootTable": f"{NAMESPACE}:pots/pale_ruin", "sherds": sherds},
                 facing=facing, cracked="false", waterlogged="false")

    def heap(self, x, z, beside, loot):
        """Suspicious gravel to brush, lying on the floor (it would fall through a gap under it) with rubble beside it."""
        for hx, hz in ((x, z), beside):
            assert self.block(hx, 1, hz) in SOFT, (self.name, hx, hz)
        self.set(x, 1, z, "minecraft:suspicious_gravel",
                 {"id": "minecraft:brushable_block", "LootTable": f"{NAMESPACE}:archaeology/{loot}"}, dusted="0")
        self.set(beside[0], 1, beside[1], self.pick(RUBBLE))

    def lantern(self, x, y, z, hanging):
        self.set(x, y, z, LANTERN, hanging="true" if hanging else "false", waterlogged="false")

    def darkest_floor(self):
        """The lowest block light on any spot a mob could stand, lit by this piece's own lanterns."""
        light = {}
        queue = collections.deque()
        for pos, (block, _, _) in self.blocks.items():
            if block in LIGHT:
                light[pos] = LIGHT[block]
                queue.append(pos)
        while queue:
            x, y, z = queue.popleft()
            level = light[(x, y, z)] - 1
            for nx, ny, nz in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if self.block(nx, ny, nz) in CLEAR and level > light.get((nx, ny, nz), 0):
                    light[(nx, ny, nz)] = level
                    queue.append((nx, ny, nz))
        darkest = 15
        for (x, y, z), (block, _, _) in self.blocks.items():
            below = self.block(x, y - 1, z)
            if block in CLEAR and below is not None and below not in NOT_A_FLOOR:
                darkest = min(darkest, light.get((x, y, z), 0))
        return darkest

    def unsupported(self):
        """Things that need a block under them and have air or a half slab there: in the game they'd hang."""
        hanging = []
        for (x, y, z), (block, _, _) in self.blocks.items():
            if block not in NEEDS_A_FLOOR:
                continue
            below = self.blocks.get((x, y - 1, z))
            if below is None or (below[0] in CLEAR and dict(below[1]).get("type") != "top"):
                hanging.append((block, x, y, z))
        return hanging

    def save(self):
        palette = {}
        blocks = []
        for (x, y, z), (block, properties, nbt_data) in sorted(self.blocks.items()):
            key = (block, properties)
            state = palette.setdefault(key, len(palette))
            entry = {"pos": [x, y, z], "state": state}
            if nbt_data:
                entry["nbt"] = nbt_data
            blocks.append(entry)
        states = []
        for block, properties in palette:
            state = {"id": block}
            if properties:
                state["properties"] = dict(properties)
            states.append(state)
        path = os.path.join(DATA, "structure", "pale_ruin", self.name + ".nbt")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbt.write(path, {"DataVersion": DATA_VERSION, "size": list(self.size), "palette": states, "blocks": blocks,
                         "entities": self.entities})


def wall_block(piece, height, bands):
    """The wall's layers: a dark footing, pale bands of calcite, grey brick between."""
    if height == 1:
        return piece.pick(DARK_WALLS)
    return CALCITE if height in bands else piece.pick(WALLS)


def shell(piece, x1, z1, x2, z2, top, bands=()):
    """A closed box from floor level 0 to its ceiling at {@code top}, hollow inside."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            edge = x in (x1, x2) or z in (z1, z2)
            near_wall = x in (x1 + 1, x2 - 1) or z in (z1 + 1, z2 - 1)
            piece.set(x, 0, z, piece.pick(WALLS if edge or near_wall else FLOOR))
            piece.set(x, top, z, piece.pick(WALLS if piece.random.random() < 0.85 else DARK_WALLS))
            for y in range(1, top):
                piece.set(x, y, z, wall_block(piece, y, bands) if edge else AIR)


def corridor(piece, x, z1, z2, top=4):
    """A passage three wide along z, centered on x, open at both ends."""
    for z in range(z1, z2 + 1):
        for dx in range(-2, 3):
            piece.set(x + dx, 0, z, piece.pick(WALLS if abs(dx) == 2 else FLOOR))
            piece.set(x + dx, top, z, piece.pick(WALLS))
            for y in range(1, top):
                piece.set(x + dx, y, z, wall_block(piece, y, ()) if abs(dx) == 2 else AIR)


def doorway(piece, x, z, along_x, height=3):
    """An opening three wide through the wall at (x, z), with a chiseled lintel."""
    for d in range(-1, 2):
        dx, dz = (d, 0) if along_x else (0, d)
        for y in range(1, height + 1):
            piece.set(x + dx, y, z + dz, AIR)
        piece.set(x + dx, height + 1, z + dz, CHISELED_TUFF)
        piece.set(x + dx, 0, z + dz, piece.pick(FLOOR))


def column(piece, x, z, top, band=None):
    """A 2 x 2 column of calcite on a tuff footing, from the floor to the ceiling at {@code top}."""
    for dx in range(2):
        for dz in range(2):
            for y in range(1, top):
                if y == 1 or y == top - 1:
                    block = CHISELED_TUFF
                elif y == band:
                    block = COPPER_BAND
                else:
                    block = CALCITE
                piece.set(x + dx, y, z + dz, block)


def overgrow(piece, x1, z1, x2, z2, top, carpets, strands):
    """Pale moss on the floor and hanging from the ceiling, wherever there's room for it."""
    for _ in range(carpets):
        x, z = piece.random.randint(x1, x2), piece.random.randint(z1, z2)
        if piece.block(x, 1, z) == AIR:
            piece.set(x, 1, z, MOSS_CARPET)
    for _ in range(strands):
        x, z = piece.random.randint(x1, x2), piece.random.randint(z1, z2)
        length = piece.random.randint(1, 3)
        if all(piece.block(x, top - 1 - i, z) == AIR for i in range(length)):
            for i in range(length):
                piece.set(x, top - 1 - i, z, HANGING_MOSS, tip="true" if i == length - 1 else "false")


def root(piece, x, z, top, heart, reach=(-1, 1)):
    """A pale root down a corner from ceiling to floor, moss at its foot, maybe with a Creaking Heart in it."""
    for y in range(1, top):
        piece.set(x, y, z, ROOT, axis="y")
    if heart:
        piece.set(x, 2, z, "minecraft:creaking_heart", axis="y", creaking_heart_state=ASLEEP, natural="true")
    dx, dz = reach
    piece.set(x + dx, top - 1, z, ROOT, axis="x")
    piece.set(x, top - 1, z + dz, ROOT, axis="z")
    piece.set(x + dx, 1, z + dz, ROOT, axis="x")
    for mx, mz in ((x + dx, z), (x, z + dz), (x + 2 * dx, z + dz), (x + dx, z + 2 * dz)):
        piece.set(mx, 0, mz, MOSS)


def stairway(piece, x, z, top, steps, direction=1, open_above=None):
    """
    Stairs three wide down from floor level {@code top}, running south (or north for direction -1):
    step i lies i blocks on and i blocks down, with four blocks of headroom.
    """
    for i in range(steps):
        y, sz = top - i, z + direction * i
        for sx in range(x - 2, x + 3):
            inside = abs(sx - x) <= 1
            piece.set(sx, y - 1, sz, piece.pick(DARK_WALLS))
            for dy in range(0, 6):
                if open_above is not None and y + dy > open_above and (not inside or dy == 5):
                    continue  # above ground the stairwell is open to the sky
                if dy == 5 or not inside:
                    piece.set(sx, y + dy, sz, piece.pick(WALLS))
                elif dy == 0:
                    piece.set(sx, y, sz, "minecraft:tuff_brick_stairs", facing="north" if direction > 0 else "south",
                              half="bottom", shape="straight", waterlogged="false")
                else:
                    piece.set(sx, y + dy, sz, AIR)
        if i % 6 == 5 and (open_above is None or y + 5 <= open_above):
            piece.lantern(x, y + 4, sz, hanging=True)


def entrance_name(depth):
    return "entrance" if depth == STAIR_DEPTHS[0] else f"entrance_{depth}"


def entrance(depth):
    """
    A ruined pavilion level with the ground, and the stairway {@code depth} down from it. The stairs
    double back flight under flight: north out from under the pavilion, a landing, south beneath
    it, a landing, and so on down.

    Jigsaw sets the start piece's height by the ground at the middle of its bounding box, so that
    middle has to be under the pavilion: with one straight flight it was 12 blocks down the
    stairs, and on a slope the pavilion ended up buried or on stilts. That's also why a deeper
    stairway is more flights and not longer ones.
    """
    assert depth % (2 * FLIGHT) == 0, "the stairs have to come out at the foot facing south"
    north = FLIGHT - 2  # how far a flight and its landing reach north of the pavilion
    end = north + 12
    piece = Piece(entrance_name(depth), (9, depth + 6, end + 1))
    assert north <= (end // 2) <= north + 8, "the piece's middle should lie under the pavilion"

    # The pavilion: a floor on a footing, and what's left of its columns. On a slope the footing
    # shows on the downhill side, so it reaches well down and ends raggedly, like rubble.
    for x in range(9):
        for z in range(north, north + 9):
            piece.set(x, depth, z, piece.pick(WALLS if piece.random.random() < 0.8 else [(MOSS, 1)]))
            for down in range(1, piece.random.randint(4, 8)):
                piece.set(x, depth - down, z, piece.pick(DARK_WALLS if down < 3 else RUBBLE))
    # Open air above the floor, so the uphill side of a slope is cut back instead of burying it.
    piece.fill(0, depth + 1, north, 8, depth + 5, north + 8, AIR)
    for (x, z), height in zip(((7, north + 1), (1, north + 7), (7, north + 7)), (5, 4, 2)):
        piece.set(x, depth + 1, z, CHISELED_TUFF)
        for y in range(depth + 2, depth + 1 + height):
            piece.set(x, y, z, CALCITE)
    for x, z in ((6, north + 6), (7, north + 2), (5, north + 4), (2, north + 8), (4, north)):
        piece.set(x, depth + 1, z, MOSS_CARPET)
    piece.set(5, depth + 1, north + 1, CALCITE)
    piece.jigsaw(4, depth, north + 7, "north_up", ANCHOR, EMPTY, EMPTY, TUFF_BRICKS)

    def landing(level, rows, closed):
        """Where the stairs turn, across both flights; {@code closed} is the row of its end wall."""
        for z in rows:
            for x in range(9):
                wall = x in (0, 8) or z == closed
                piece.set(x, level - 1, z, piece.pick(DARK_WALLS))
                piece.set(x, level, z, piece.pick(WALLS if wall else FLOOR))
                piece.set(x, level + 5, z, piece.pick(WALLS))
                for y in range(level + 1, level + 5):
                    piece.set(x, y, z, piece.pick(WALLS) if wall else AIR)
        piece.lantern(4, level + 4, rows[len(rows) // 2], hanging=True)

    for flight in range(depth // FLIGHT):
        top = depth - flight * FLIGHT
        if flight % 2 == 0:
            # Down the west side and out to the north.
            stairway(piece, 2, north + 5, top, FLIGHT, direction=-1, open_above=depth if flight == 0 else None)
            landing(top - FLIGHT, range(0, north - 4), closed=0)
        else:
            # Back south down the east side, beneath the pavilion.
            stairway(piece, 6, north - 4, top, FLIGHT)
            if top - FLIGHT > 0:
                landing(top - FLIGHT, range(north + 6, north + 10), closed=north + 9)

    # The passage at the foot of the stairs, and the way on.
    corridor(piece, 6, north + 6, end, top=5)
    piece.lantern(6, 4, north + 8, hanging=True)
    piece.exit(6, end, "south", pool(1, "ahead"))
    return piece


def guard_room(piece, top, heart, sentries):
    """Four columns set wide apart for cover, a root down one corner."""
    for x in (5, 14):
        for z in (4, 11):
            column(piece, x, z, top)
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=18, strands=14)
    return {1: [(10, 10)], 2: [(4, 12), (16, 12)]}[sentries]


def overgrown_room(piece, top, heart, sentries):
    """The woods have this one: a great root through it, two columns down, moss and eyeblossoms where the floor was."""
    for x in (5, 14):
        column(piece, x, 4, top)
    # The fallen pair: stumps of different heights, and what's left of them on the floor.
    for x, height in ((5, 3), (14, 2)):
        for dx in range(2):
            for dz in range(2):
                piece.set(x + dx, 1, 11 + dz, CHISELED_TUFF)
                for y in range(2, height + (dx + dz) % 2):
                    piece.set(x + dx, y, 11 + dz, CALCITE)
    for x, z in ((7, 13), (8, 12), (12, 13), (16, 10), (3, 9)):
        piece.set(x, 1, z, piece.pick(RUBBLE + [(CALCITE, 3)]))
    piece.set(8, 2, 12, CALCITE)

    # The root comes through the ceiling in the south-east corner, slants down across the room and
    # runs out along the floor.
    for step, (x, z) in enumerate(((18, 13), (17, 13), (16, 12), (15, 12), (14, 11), (13, 10), (12, 10), (11, 9))):
        y = top - 1 - step * (top - 3) // 7
        for dy in (0, -1):
            piece.set(x, y + dy, z, ROOT, axis="x")
        piece.set(x, y, z - 1, ROOT, axis="x")
    for x in range(6, 11):
        piece.set(x, 1, 8, ROOT, axis="x")
    piece.set(10, 2, 8, ROOT, axis="x")
    piece.set(10, 2, 9, ROOT, axis="x")
    if heart:
        piece.set(8, 1, 8, "minecraft:creaking_heart", axis="x", creaking_heart_state=ASLEEP, natural="true")

    for x in range(2, 19):
        for z in range(3, 14):
            if piece.block(x, 1, z) == AIR and piece.random.random() < 0.45 - 0.03 * abs(x - 10):
                piece.set(x, 0, z, MOSS)
                if piece.random.random() < 0.12:
                    piece.set(x, 1, z, "minecraft:closed_eyeblossom")
    overgrow(piece, 1, 2, 19, 14, top, carpets=30, strands=26)
    return {1: [(4, 9)], 2: [(4, 9), (16, 6)]}[sentries]


def workshop(piece, top, heart, sentries):
    """Where the builders worked: benches and pots along the walls, and a stump they never finished."""
    for x in (5, 14):
        column(piece, x, 11, top)
    bench(piece, 1, (3, 4, 5, 6, 10, 11, 12, 13))
    for x, y, z, facing in ((1, 2, 4, "east"), (1, 2, 11, "east"), (18, 1, 13, "north"), (19, 1, 12, "west"), (19, 1, 6, "west")):
        piece.pot(x, y, z, facing)

    # The stump as far as they got with it: the log and two of its four roots on a plinth, the
    # copper for its cap still standing beside it.
    # It stands to one side, its bare log towards the door.
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            piece.set(15 + dx, 1, 6 + dz, POLISHED_TUFF if (dx, dz) != (0, 0) else CHISELED_TUFF)
    piece.set(15, 2, 6, "minecraft:pale_oak_log", axis="y")
    piece.set(16, 2, 6, "minecraft:resin_block")
    piece.set(15, 2, 7, "minecraft:resin_block")
    piece.set(13, 1, 4, "minecraft:oxidized_copper")

    root(piece, 19, 2, top, heart)
    overgrow(piece, 2, 2, 19, 14, top, carpets=10, strands=10)
    return {1: [(10, 11)], 2: [(8, 12), (12, 12)]}[sentries]


def bench(piece, x, zs):
    for z in zs:
        piece.set(x, 1, z, "minecraft:tuff_brick_slab", type="top", waterlogged="false")


def archive(piece, top, heart, sentries):
    """Their records: rows of bookshelves, which make the cover here, and a lectern between them."""
    for x, facing in ((5, "west"), (14, "east")):
        for z in (4, 5, 6, 7, 10, 11, 12, 13):
            for y in range(1, 4 if piece.random.random() < 0.7 else 3):
                if piece.random.random() < 0.2:
                    piece.set(x, y, z, "minecraft:chiseled_bookshelf", facing=facing,
                              **{f"slot_{slot}_occupied": "false" for slot in range(6)})
                else:
                    piece.set(x, y, z, "minecraft:bookshelf")
    for x in range(13, 17):
        for y in range(1, 4):
            piece.set(x, y, 2, "minecraft:bookshelf")
    piece.set(10, 1, 6, "minecraft:lectern", facing="north", has_book="false", powered="false")
    for x, y, z in ((5, 4, 7), (14, 4, 10), (13, 4, 2), (1, 6, 14), (19, 6, 14), (6, 1, 13)):
        if piece.block(x, y, z) == AIR:
            piece.set(x, y, z, "minecraft:cobweb")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=8, strands=8)
    return {1: [(10, 10)], 2: [(3, 12), (16, 12)]}[sentries]


def cells(piece, top, heart, sentries):
    """Where they kept what they studied: barred cells along the far wall, one still holding a heart."""
    for x in (5, 14):
        column(piece, x, 4, top)
    for x in list(range(1, 9)) + list(range(12, 20)):
        wall = x in (4, 8, 12, 16)
        for z in ((11, 12, 13, 14) if wall else (11,)):
            for y in range(1, top):
                gate = z == 11 and y <= 2 and x in (2, 6, 14, 18)
                if wall:
                    piece.set(x, y, z, wall_block(piece, y, ()))
                elif not gate:
                    piece.set(x, y, z, BARS, east="true", west="true", north="false", south="false", waterlogged="false")
    # One cell's captive: a heart between two logs, as a living tree holds it.
    for y in (1, 2, 3):
        piece.set(6, y, 13, "minecraft:pale_oak_log", axis="y")
    if heart:
        piece.set(6, 2, 13, "minecraft:creaking_heart", axis="y", creaking_heart_state=ASLEEP, natural="true")
    piece.set(6, 1, 12, "minecraft:resin_clump", south="true", north="false", east="false", west="false", up="false", down="false",
              waterlogged="false")
    for y in range(4, top):
        piece.set(2, y, 13, CHAIN, axis="y", waterlogged="false")
        piece.set(18, y, 12, CHAIN, axis="y", waterlogged="false")
    piece.set(15, 1, 12, "minecraft:skeleton_skull", rotation="5", powered="false")
    # The two middle cells are walled off from the room's lanterns.
    for x in (6, 14):
        piece.lantern(x, top - 1, 12, hanging=True)
    overgrow(piece, 1, 2, 19, 10, top, carpets=10, strands=8)
    return {1: [(10, 8)], 2: [(6, 7), (14, 7)]}[sentries]


def lens_room(piece, top, heart, sentries):
    """Where the lens was ground and tried: one in its copper frame, aimed at posts of pale oak."""
    for x in range(8, 13):
        for y in range(1, 6):
            rim = x in (8, 12) or y in (1, 5)
            piece.set(x, y, 10, CUT_COPPER if rim else "minecraft:orange_stained_glass")
    for x, height in ((8, 2), (10, 2), (12, 3)):
        for y in range(1, height + 1):
            piece.set(x, y, 6, "minecraft:pale_oak_log", axis="y")
        piece.set(x, 2, 7, "minecraft:resin_clump", north="true", south="false", east="false", west="false", up="false",
                  down="false", waterlogged="false")
    bench(piece, 1, (3, 4, 5, 6))
    for z in (4, 5):
        piece.set(1, 2, z, "minecraft:amethyst_cluster", facing="up", waterlogged="false")
    for x, z in ((17, 11), (17, 13)):
        piece.set(x, 1, z, "minecraft:grindstone", face="floor", facing="east")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=10, strands=8)
    return {1: [(10, 13)], 2: [(5, 12), (15, 12)]}[sentries]


def resin_works(piece, top, heart, sentries):
    """Where resin was rendered: copper vats of it under their hoists, one split open, and bricks of it stacked."""
    for vx, vz, whole in ((5, 5, True), (15, 5, True), (5, 11, False)):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if (dx, dz) == (0, 0):
                    piece.set(vx, 1, vz, "minecraft:resin_block")
                elif whole or (dx, dz) not in ((1, 0), (1, 1), (0, 1)):
                    piece.set(vx + dx, 1, vz + dz, CUT_COPPER)
                else:
                    piece.set(vx + dx, 1, vz + dz, "minecraft:resin_clump", down="true", up="false", north="false",
                              south="false", east="false", west="false", waterlogged="false")
        if whole:
            for y in range(3, top):
                piece.set(vx, y, vz, CHAIN, axis="y", waterlogged="false")
    for x, y, z in ((15, 1, 11), (16, 1, 11), (15, 1, 12), (16, 1, 12), (15, 2, 11)):
        piece.set(x, y, z, "minecraft:resin_bricks")
    piece.set(16, 2, 12, "minecraft:chiseled_resin_bricks")
    for x in (9, 11):
        piece.set(x, 1, 6, "minecraft:cauldron")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=8, strands=8)
    return {1: [(10, 10)], 2: [(8, 12), (12, 12)]}[sentries]


def nursery(piece, top, heart, sentries):
    """Where they raised pale oaks under lantern light: beds of earth, saplings that never grew, eyeblossoms."""
    for bx, bz in ((4, 5), (16, 5), (4, 11), (16, 11)):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                piece.set(bx + dx, 1, bz + dz, "minecraft:coarse_dirt")
                if (dx, dz) == (0, 0):
                    piece.set(bx, 2, bz, "minecraft:pale_oak_sapling", stage="0")
                elif piece.random.random() < 0.3:
                    piece.set(bx + dx, 2, bz + dz, "minecraft:closed_eyeblossom")
    piece.set(10, 1, 6, "minecraft:cauldron")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=20, strands=16)
    return {1: [(10, 9)], 2: [(8, 9), (12, 9)]}[sentries]


def shrine(piece, top, heart, sentries):
    """
    This was a temple. In the middle stands what they prayed to, the tree in small: a trunk of pale
    oak on a stepped base with a heart of resin in it, candles at its foot. Benches face it from
    both sides and are the room's low cover; the aisles between them stay open.
    """
    cx, cz = ROOM_CENTER
    # A cross of half slabs, with the candles on the floor in its corners: on a half slab a candle
    # has nothing under it and hangs in the air.
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        piece.set(cx + dx, 1, cz + dz, "minecraft:tuff_brick_slab", type="bottom", waterlogged="false")
    piece.set(cx, 1, cz, CHISELED_TUFF)
    piece.set(cx, 2, cz, ROOT, axis="y")
    piece.set(cx, 3, cz, "minecraft:chiseled_resin_bricks")
    piece.set(cx, 4, cz, ROOT, axis="y")
    for (dx, dz), candles in (((-1, -1), 3), ((1, -1), 2), ((-1, 1), 1), ((1, 1), 4)):
        piece.set(cx + dx, 1, cz + dz, "minecraft:candle", candles=str(candles), lit="true", waterlogged="false")
    # A bench's back is the way its stairs face: these sit towards the middle.
    for x, back in ((4, "west"), (6, "west"), (14, "east"), (16, "east")):
        for z in (4, 5, 6, 10, 11, 12):
            if piece.random.random() < 0.85:
                piece.set(x, 1, z, "minecraft:tuff_brick_stairs", facing=back, half="bottom", shape="straight", waterlogged="false")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=8, strands=10)
    return {1: [(10, 11)], 2: [(8, 12), (12, 12)]}[sentries]


def foundry(piece, top, heart, sentries):
    """
    Where the copper was worked for the bands, the cages and the lens frames: furnaces along one wall,
    anvils, copper stacked in every stage of its weathering, and a band left half cast on the floor.
    """
    for z in (4, 5, 6, 10, 11, 12):
        piece.set(1, 1, z, "minecraft:blast_furnace", facing="east", lit="false")
        if z in (5, 11):
            piece.set(1, 2, z, "minecraft:blast_furnace", facing="east", lit="false")
    piece.set(4, 1, 5, "minecraft:damaged_anvil", facing="north")
    piece.set(4, 1, 11, "minecraft:chipped_anvil", facing="north")
    piece.set(3, 1, 12, "minecraft:cauldron")
    stages = ["minecraft:waxed_copper_block", "minecraft:waxed_exposed_copper", "minecraft:waxed_weathered_copper",
              "minecraft:waxed_oxidized_copper"]
    for i, block in enumerate(stages):
        piece.set(14 + i, 1, 4, block)
        piece.set(14 + i, 1, 5, block)
        if i % 2 == 0:
            piece.set(14 + i, 2, 4, block)
    # The band: three sides of a square of the columns' copper, the fourth never poured.
    cx, cz = ROOM_CENTER
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            if (dx, dz) != (0, 0) and not (dz == 1 and dx >= 0):
                piece.set(cx + dx, 1, cz + dz, COPPER_BAND)
    column(piece, 13, 9, top)
    column(piece, 6, 9, top)
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=6, strands=8)
    return {1: [(10, 11)], 2: [(8, 12), (12, 12)]}[sentries]


def collapsed_room(piece, top, heart, sentries):
    """
    Part of the ceiling is down. Two ridges of fallen stone cross the room from opposite walls, each
    with a gap at its far end, so the way across winds and nothing looks from one end to the other.
    """
    for z, xs in ((6, range(1, 13)), (10, range(8, 20))):
        for x in xs:
            height = piece.random.choice((2, 2, 3, 3, 4))
            for y in range(1, height + 1):
                piece.set(x, y, z, piece.pick(RUBBLE))
            # Stone that rolled off the ridge.
            for side in (-1, 1):
                # Not by the walls: the side doors open there.
                if 1 < x < 19 and piece.random.random() < 0.35 and piece.block(x, 1, z + side) == AIR:
                    piece.set(x, 1, z + side, piece.pick(RUBBLE))
    # What's left of a column, and the root that brought it down.
    for x, y, z in ((15, 1, 4), (16, 1, 4), (15, 2, 4), (4, 1, 12), (5, 1, 12), (4, 1, 13)):
        piece.set(x, y, z, CALCITE)
    for x in range(3, 9):
        piece.set(x, top - 1, 6, ROOT, axis="x")
    piece.set(8, top - 2, 6, ROOT, axis="y")
    piece.set(8, top - 3, 6, ROOT, axis="y")
    root(piece, 19, 2, top, heart)
    overgrow(piece, 1, 2, 19, 14, top, carpets=14, strands=14)
    return {1: [(10, 8)], 2: [(4, 8), (16, 12)]}[sentries]


ROOM_KINDS = {"shrine": shrine, "foundry": foundry, "collapsed": collapsed_room, "guard": guard_room, "overgrown": overgrown_room, "workshop": workshop, "archive": archive, "cells": cells,
              "lens_room": lens_room, "resin_works": resin_works, "nursery": nursery}
ROOM_TOP = 8
ROOM_CENTER = (10, 8)
# The furnishing of a room is a piece of its own, set into the room through a jigsaw in its floor:
# with the walls and the way out in one piece and the furniture in another, a new kind of room is
# three templates (one per variant) instead of one for every way a room can lie in the line.
INTERIOR = f"{NAMESPACE}:pale_ruin/interior"
INTERIOR_ORIGIN = (1, 0, 2)
INTERIOR_SIZE = (19, ROOM_TOP, 13)
FURNISHED_FROM = (10, 2)


def bare_room(name):
    """A room 19 wide, 13 deep and 7 high inside with its lanterns, entered from the north, and nothing in it."""
    cx, _ = ROOM_CENTER
    piece = Piece(name, (21, ROOM_TOP + 1, 16))
    piece.random = random.Random("room")
    corridor(piece, cx, 0, 0)
    shell(piece, 0, 1, 20, 15, ROOM_TOP, bands={4})
    doorway(piece, cx, 1, along_x=True)
    for x, z in ((3, 3), (17, 3), (3, 13), (17, 13), (10, 3), (10, 13), (10, 8), (3, 8), (17, 8)):
        piece.lantern(x, ROOM_TOP - 1, z, hanging=True)
    return piece


def room(slot, bearing, exit_side):
    """The room of one slot for one bearing of the line, with its way out and the jigsaw that furnishes it."""
    cx, cz = ROOM_CENTER
    piece = bare_room(room_name(slot, bearing, exit_side))
    piece.entry(cx, 0)
    fx, fz = FURNISHED_FROM
    piece.jigsaw(fx, 0, fz, "up_north", f"{NAMESPACE}:pale_ruin/floor", INTERIOR, pool(f"interior_{ROOM_VARIANTS[slot - 1]}"),
                 DEEPSLATE_TILES)
    next_pool = pool(slot + 1, TURNS[bearing][exit_side]) if slot < ROOM_SLOTS else pool("descent")
    if exit_side == "straight":
        doorway(piece, cx, 15, along_x=True)
        piece.exit(cx, 15, "south", next_pool)
    elif exit_side == "left":
        doorway(piece, 20, cz, along_x=False)
        piece.exit(20, cz, "east", next_pool)
    else:
        doorway(piece, 0, cz, along_x=False)
        piece.exit(0, cz, "west", next_pool)
    wall = side_door(slot, bearing, exit_side)
    x, z = {"east": (20, cz), "west": (0, cz), "south": (cx, 15)}[wall]
    assert piece.block(x, 1, z) != AIR, (piece.name, "its side door is its way on")
    doorway(piece, x, z, along_x=wall == "south")
    piece.side(x, z, wall, pool(f"chambers_{slot}"))
    return piece


def interior(kind, variant):
    """What one kind of room holds, in one variant: everything that differs from the bare room."""
    sentries, heart, chest = VARIANTS[variant]
    scratch = bare_room("scratch")
    bare = dict(scratch.blocks)
    scratch.random = random.Random(f"{kind}_{variant}")
    posts = ROOM_KINDS[kind](scratch, ROOM_TOP, heart, sentries)

    # Every room has two pots and two heaps to brush, wherever its furniture stands.
    scratch.pot(1, 1, 2, "east")
    scratch.pot(2, 1, 14, "north")
    scratch.heap(7, 2, (6, 2), "pale_ruin")
    scratch.heap(13, 14, (14, 14), "pale_ruin")
    if chest:
        scratch.chest(18, 14, "north", chest)
    for x, z in posts:
        for y in range(1, 4):
            assert scratch.block(x, y, z) in SOFT, (kind, x, y, z)
            scratch.set(x, y, z, AIR)
        scratch.entity(x, 1, z, f"{NAMESPACE}:ruin_sentry")
    # Whichever way out the room gets, the way to it stays open.
    for x, z in [(9, 2), (10, 2), (11, 2), (9, 14), (10, 14), (11, 14), (1, 7), (1, 8), (1, 9), (19, 7), (19, 8), (19, 9)]:
        for y in (1, 2):
            assert scratch.block(x, y, z) in SOFT, (kind, "blocks a doorway at", x, y, z)

    ox, oy, oz = INTERIOR_ORIGIN
    piece = Piece(f"interior/{kind}_{variant}", INTERIOR_SIZE)
    for (x, y, z), block in scratch.blocks.items():
        if bare.get((x, y, z)) != block:
            piece.set(x - ox, y - oy, z - oz, block[0], block[2], **dict(block[1]))
    for entity in scratch.entities:
        x, y, z = entity["blockPos"]
        piece.entity(x - ox, y - oy, z - oz, entity["nbt"]["id"])
    fx, fz = FURNISHED_FROM
    piece.jigsaw(fx - ox, 1 - oy, fz - oz, "down_north", INTERIOR, EMPTY, EMPTY, AIR)
    assert not scratch.unsupported(), (kind, variant, scratch.unsupported())
    # Its lanterns hang in the room it goes into.
    piece.lit = scratch.darkest_floor()
    return piece


CHAMBER_TOP = 6


def bare_chamber(name):
    """A dead end off a room: a short passage from the north into a room 9 x 9 inside and 5 high."""
    piece = Piece(f"chamber/{name}", (11, CHAMBER_TOP + 1, 14))
    corridor(piece, 5, 0, 2)
    shell(piece, 0, 3, 10, 13, CHAMBER_TOP, bands={3})
    doorway(piece, 5, 3, along_x=True)
    piece.entry(5, 0)
    piece.lantern(5, 3, 1, hanging=True)
    for x, z in ((2, 5), (8, 5), (2, 11), (8, 11), (5, 8)):
        piece.lantern(x, CHAMBER_TOP - 1, z, hanging=True)
    return piece


def store_room():
    """What the builders kept by: stacks of their materials, pots, and a chest."""
    piece = bare_chamber("store_room")
    piece.fill(1, 1, 4, 2, 2, 5, "minecraft:resin_bricks")
    piece.set(1, 3, 4, "minecraft:resin_bricks")
    piece.fill(8, 1, 4, 9, 1, 6, "minecraft:pale_oak_planks")
    piece.fill(9, 2, 4, 9, 2, 5, "minecraft:pale_oak_planks")
    piece.fill(1, 1, 10, 1, 2, 12, CUT_COPPER)
    piece.set(2, 1, 12, CUT_COPPER)
    piece.fill(8, 1, 11, 9, 2, 12, "minecraft:waxed_weathered_copper")
    piece.set(7, 1, 12, "minecraft:waxed_weathered_copper")
    piece.chest(5, 12, "north", "room")
    # A pot's front is the side away from where it faces: these show theirs to the door.
    piece.pot(3, 1, 12, "south")
    piece.pot(4, 1, 12, "south")
    piece.pot(6, 1, 12, "south")
    piece.pot(3, 1, 4, "north")
    overgrow(piece, 1, 4, 9, 12, CHAMBER_TOP, carpets=5, strands=4)
    return piece


def study():
    """Where the builders wrote it all down: shelves, a lectern, a desk with its candles."""
    piece = bare_chamber("study")
    for x in range(1, 10):
        for y in (1, 2, 3):
            piece.set(x, y, 12, "minecraft:bookshelf" if piece.random.random() < 0.8 else AIR)
    for z in (9, 10, 11):
        piece.set(1, 1, z, "minecraft:bookshelf")
        piece.set(1, 2, z, "minecraft:bookshelf")
    piece.set(5, 1, 9, "minecraft:lectern", facing="north", has_book="false", powered="false")
    for z in (5, 6):
        piece.set(8, 1, z, "minecraft:tuff_brick_slab", type="top", waterlogged="false")
    piece.set(8, 2, 5, "minecraft:candle", candles="3", lit="false", waterlogged="false")
    piece.set(8, 2, 6, "minecraft:candle", candles="1", lit="false", waterlogged="false")
    piece.set(9, 1, 4, "minecraft:cobweb")
    piece.set(1, CHAMBER_TOP - 1, 4, "minecraft:cobweb")
    piece.pot(1, 1, 4, "east")
    piece.pot(9, 1, 11, "east")
    piece.heap(3, 7, (2, 7), "pale_ruin")
    return piece


def tomb():
    """The builders' own: one of them under a slab, pots on plinths along the walls, heaps to brush."""
    piece = bare_chamber("tomb")
    for z in (7, 8, 9):
        piece.set(5, 1, z, POLISHED_TUFF)
    piece.set(5, 2, 8, "minecraft:tuff_brick_slab", type="bottom", waterlogged="false")
    piece.set(5, 2, 7, "minecraft:skeleton_skull", rotation="8", powered="false")
    for x, facing in ((1, "west"), (9, "east")):
        for z in (5, 8, 11):
            piece.set(x, 1, z, CHISELED_TUFF)
            piece.pot(x, 2, z, facing)
    piece.heap(3, 12, (2, 12), "pale_ruin")
    piece.heap(7, 12, (8, 12), "pale_ruin")
    piece.heap(4, 5, (4, 4), "pale_ruin")
    piece.set(1, CHAMBER_TOP - 1, 12, "minecraft:cobweb")
    piece.set(9, CHAMBER_TOP - 1, 4, "minecraft:cobweb")
    return piece


def collapsed_passage():
    """A passage that went somewhere once: fallen stone and roots after a few blocks, one heap before them."""
    piece = Piece("chamber/collapsed_passage", (5, 5, 9))
    corridor(piece, 2, 0, 8)
    piece.entry(2, 0)
    piece.lantern(2, 3, 1, hanging=True)
    for x in range(5):
        for y in range(5):
            piece.set(x, y, 8, piece.pick(RUBBLE if 0 < y < 4 and 0 < x < 4 else WALLS))
    for (x, z), height in (((1, 7), 3), ((2, 7), 2), ((3, 7), 3), ((1, 6), 2), ((3, 6), 1), ((2, 6), 1), ((3, 5), 1)):
        for y in range(1, height + 1):
            piece.set(x, y, z, piece.pick(RUBBLE))
    piece.set(2, 3, 7, ROOT, axis="z")
    piece.set(2, 3, 6, ROOT, axis="z")
    piece.set(1, 3, 6, HANGING_MOSS, tip="true")
    piece.heap(1, 4, (1, 5), "pale_ruin")
    return piece


def sentry_workshop():
    """
    How a Sentry of one's own is built, which nothing else in the game says: a pale oak log, a
    Creaking Heart on it, a carved pumpkin on top. Three plinths show it in order, the last with the
    builders' own Sentry still on its log; the pumpkin, the one part the Ruin hasn't shown, lies on
    the bench and grows along the wall.
    """
    piece = bare_chamber("sentry_workshop")
    for x in (3, 5, 7):
        piece.set(x, 1, 9, CHISELED_TUFF)
        piece.set(x, 2, 9, "minecraft:pale_oak_log", axis="y")
    # Uprooted, as a heart with no tree around it is: it never wakes, it only shows.
    piece.set(5, 3, 9, "minecraft:creaking_heart", axis="y", creaking_heart_state="uprooted", natural="false")
    # Read from the door, which looks south, the first plinth is the eastern one.
    piece.entity(3, 3, 9, f"{NAMESPACE}:heartwood_sentry")

    # The same three on the wall behind them. Its heart is resin brick: a real one under a real
    # pumpkin is the pattern itself, and the wall would build a Sentry.
    for x in (4, 5, 6):
        for y in (1, 2, 3, 4, 5):
            piece.set(x, y, 13, CALCITE)
    piece.set(5, 2, 13, "minecraft:pale_oak_log", axis="y")
    piece.set(5, 3, 13, "minecraft:chiseled_resin_bricks")
    piece.set(5, 4, 13, "minecraft:carved_pumpkin", facing="north")

    # The pumpkin: one carved on the bench with the shears in the chest beside it, more on the vine.
    for z in (6, 7):
        piece.set(9, 1, z, "minecraft:tuff_brick_slab", type="top", waterlogged="false")
    piece.set(9, 2, 6, "minecraft:carved_pumpkin", facing="west")
    piece.chest(9, 5, "west", "sentry_workshop")
    for z in range(5, 12):
        piece.set(1, 0, z, "minecraft:farmland", moisture="0")
    for z in (5, 10):
        piece.set(1, 0, z, "minecraft:coarse_dirt")
        piece.set(1, 1, z, "minecraft:pumpkin")
    piece.set(1, 1, 6, "minecraft:attached_pumpkin_stem", facing="north")
    piece.set(1, 1, 9, "minecraft:attached_pumpkin_stem", facing="south")
    piece.set(1, 1, 8, "minecraft:pumpkin_stem", age="7")
    piece.set(1, 1, 11, "minecraft:pumpkin_stem", age="4")

    # A pot's front is the side away from where it faces: these show theirs to the door.
    for x in (2, 8):
        piece.pot(x, 1, 12, "south", front=f"{NAMESPACE}:guardian_pottery_sherd")
    return piece


CHAMBER_KINDS = {"store_room": store_room, "study": study, "tomb": tomb, "collapsed_passage": collapsed_passage,
                 "sentry_workshop": sentry_workshop}


def descent():
    """The second stairway, from the last room down to the hall."""
    depth = DESCENT_DEPTH
    piece = Piece("descent", (5, depth + 6, depth + 4))
    # A short level stretch at the top, so the stairs don't start in the room's doorway.
    for z in range(2):
        for x in range(5):
            piece.set(x, depth, z, piece.pick(WALLS if x in (0, 4) else FLOOR))
            piece.set(x, depth + 5, z, piece.pick(WALLS))
            for y in range(depth + 1, depth + 5):
                piece.set(x, y, z, piece.pick(WALLS) if x in (0, 4) else AIR)
    piece.lantern(2, depth + 4, 0, hanging=True)
    piece.entry(2, 0, y=depth)
    stairway(piece, 2, 2, depth, depth)
    corridor(piece, 2, depth + 2, depth + 3, top=5)
    piece.lantern(2, 4, depth + 2, hanging=True)
    piece.exit(2, depth + 3, "south", pool("hall"))
    return piece


def hall():
    """The builders' research hall, 25 x 25 inside and 13 high, with the felled stump in its middle."""
    top = 14
    cx, cz = 13, 16
    piece = Piece("hall", (27, top + 1, 30))
    corridor(piece, cx, 0, 2, top=5)
    shell(piece, 0, 3, 26, 29, top, bands={5, 10})
    doorway(piece, cx, 3, along_x=True, height=4)
    piece.lantern(cx, 4, 1, hanging=True)
    piece.entry(cx, 0)

    # Two colonnades down the hall, with the Elderwood's place between them.
    for x in (6, 19):
        for z in (7, 12, 19, 24):
            column(piece, x, z, top, band=7)
        for z in range(5, 28):
            if piece.block(x, 0, z) not in (None,):
                piece.set(x, 0, z, piece.pick(WALLS))
                piece.set(x + 1, 0, z, piece.pick(WALLS))

    # A pale square in the floor marks where the tree stood, around its dais.
    for d in range(-5, 6):
        for x, z in ((cx + d, cz - 5), (cx + d, cz + 5), (cx - 5, cz + d), (cx + 5, cz + d)):
            piece.set(x, 0, z, CALCITE)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            ring = max(abs(dx), abs(dz))
            if ring == 3:
                piece.set(cx + dx, 1, cz + dz, "minecraft:tuff_brick_slab", type="bottom", waterlogged="false")
            else:
                piece.set(cx + dx, 1, cz + dz, CHISELED_TUFF if ring == 2 else POLISHED_TUFF)
    # The stump, whole but for its heart: a Creaking Heart on the copper wakes the Elderwood.
    piece.set(cx, 2, cz, "minecraft:pale_oak_log", axis="y")
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        piece.set(cx + dx, 2, cz + dz, "minecraft:resin_block")
    piece.set(cx, 3, cz, "minecraft:oxidized_copper")

    # Lanterns on posts along the walls and on chains over the nave.
    posts = [(1, z) for z in (6, 11, 16, 21, 26)] + [(25, z) for z in (6, 11, 16, 21, 26)]
    posts += [(x, 4) for x in (4, 9, 17, 22)] + [(x, 28) for x in (4, 9, 13, 17, 22)]
    for x, z in posts:
        piece.set(x, 1, z, "minecraft:tuff_brick_wall", up="true", north="none", south="none", east="none", west="none",
                  waterlogged="false")
        piece.lantern(x, 2, z, hanging=False)
    for x, z in ((cx, 8), (cx, 24), (9, 10), (17, 10), (9, 22), (17, 22), (3, 16), (23, 16)):
        for y in range(8, top):
            piece.set(x, y, z, CHAIN, axis="y", waterlogged="false")
        piece.lantern(x, 7, z, hanging=True)

    # On the wall behind the stump, what stood on it: the tree with its lens and its seven hearts,
    # four in the trunk, one on each branch and the core under the lens. The hearts are resin, not
    # hearts: seven real ones in a wall would be seven to carry off.
    for x in range(cx - 5, cx + 6):
        for y in range(1, 13):
            piece.set(x, y, 29, CALCITE)
    bark = [(cx, y) for y in range(1, 10)] + [(cx + dx, 1) for dx in (-2, -1, 1, 2)] + [(cx - 3, 1), (cx + 3, 1)]
    bark += [(cx + side * d, 8 + d // 2) for side in (-1, 1) for d in (1, 2, 3, 4)]
    bark += [(cx + side * 4, y) for side in (-1, 1) for y in (10, 11)] + [(cx + side, 10) for side in (-1, 1)] + [(cx, 10), (cx, 11)]
    for x, y in bark:
        piece.set(x, y, 29, ROOT, axis="y")
    for x, y in ((cx, 2), (cx - 1, 3), (cx + 1, 3), (cx, 4), (cx - 3, 9), (cx + 3, 9), (cx, 6)):
        piece.set(x, y, 29, "minecraft:resin_block")
    piece.set(cx, 7, 29, "minecraft:ochre_froglight", axis="z")

    root(piece, 25, 4, top, heart=False, reach=(-1, 1))
    root(piece, 1, 28, top, heart=False, reach=(1, -1))
    overgrow(piece, 1, 4, 25, 28, top, carpets=40, strands=30)

    # Before the stump, what it's missing: a whole Creaking Heart in the chest, between two pots
    # that show the tree with its heart. Heaps to brush in the corners hold the rarer finds.
    for x in (cx - 2, cx + 2):
        piece.set(x, 1, cz - 5, AIR)
        # A pot's front is the side away from where it faces: these show theirs to the door.
        piece.pot(x, 1, cz - 5, "south", front=f"{NAMESPACE}:creaking_pottery_sherd")
    piece.set(cx, 1, cz - 5, AIR)
    piece.chest(cx, cz - 5, "north", "hall")
    for (x, z), dx in (((3, 6), 1), ((23, 6), -1), ((3, 26), 1), ((23, 26), -1)):
        piece.set(x, 1, z, AIR)
        piece.set(x + dx, 1, z, AIR)
        piece.heap(x, z, (x + dx, z), "pale_ruin_hall")
    return piece


def room_name(slot, bearing, exit_side):
    return f"room_{slot}_{bearing}_{exit_side}"


def pool(slot, bearing=None):
    return f"{NAMESPACE}:pale_ruin/{slot if bearing is None else f'rooms_{slot}_{bearing}'}"


def write_json(path, value):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as out:
        out.write(json.dumps(value, indent=2) + "\n")


def write_pool(name, pieces):
    write_json(f"worldgen/template_pool/pale_ruin/{name}.json", {
        "elements": [{
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": f"{NAMESPACE}:pale_ruin/{piece}",
                "processors": "minecraft:empty",
                "projection": "rigid",
            },
            "weight": weight,
        } for piece, weight in pieces],
        "fallback": "minecraft:empty",
    })


def main():
    pieces = [entrance(depth) for depth in STAIR_DEPTHS] + [descent(), hall()]
    for slot in range(1, ROOM_SLOTS + 1):
        for bearing in (["ahead"] if slot == 1 else TURNS):
            pieces += [room(slot, bearing, side) for side in TURNS[bearing]]
            write_pool(f"rooms_{slot}_{bearing}", [(room_name(slot, bearing, side), 1) for side in TURNS[bearing]])
        write_pool(f"chambers_{slot}", [(f"chamber/{kind}", 1) for kind in CHAMBERS[slot]])
    pieces += [build() for build in CHAMBER_KINDS.values()]
    for variant in VARIANTS:
        pieces += [interior(kind, variant) for kind in ROOM_KINDS]
        write_pool(f"interior_{variant}", [(f"interior/{kind}_{variant}", 1) for kind in ROOM_KINDS])
    assert sum(VARIANTS[variant][0] for variant in ROOM_VARIANTS) == 7
    for depth in STAIR_DEPTHS:
        write_pool(entrance_name(depth), [(entrance_name(depth), 1)])
    write_pool("descent", [("descent", 1)])
    write_pool("hall", [("hall", 1)])
    for piece in pieces:
        piece.save()
        # An interior's floor is the room's, so it was checked with its room around it.
        assert piece.name.startswith("interior/") or not piece.unsupported(), (piece.name, piece.unsupported())
        darkest = piece.lit if hasattr(piece, "lit") else piece.darkest_floor()
        print(f"{piece.name}: {piece.size}, {len(piece.blocks)} blocks, {len(piece.entities)} entities, darkest floor {darkest}")
        # The pavilion is under the open sky; everywhere else the builders' lanterns reach.
        assert darkest > 0 or piece.name.startswith("entrance"), piece.name

    write_json("worldgen/structure/pale_ruin.json", {
        # Jigsaw structures to choose from, from the shallowest: a site gets the first that stays
        # under the ground there.
        "type": f"{NAMESPACE}:pale_ruin",
        "depths": [{
            "biomes": f"#{NAMESPACE}:has_structure/pale_ruin",
            # Built into an underground water pocket, its lanterns, chains and slabs would come out
            # waterlogged and flood the room.
            "liquid_settings": "ignore_waterlogging",
            "max_distance_from_center": 128,
            "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "size": ROOM_SLOTS + 5,
            # Nothing but its own guards lives in the Ruin, lit or not.
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": []}},
            # Jigsaw puts the start piece's bottom layer one below this height above the surface, wherever its
            # anchor is: the pavilion's floor is the stairway's depth above that bottom.
            "start_height": {"absolute": 1 - depth - SUNK},
            "start_jigsaw_name": ANCHOR,
            "start_pool": pool(entrance_name(depth)),
            "step": "underground_decoration",
            "terrain_adaptation": "none",
            "use_expansion_hack": False,
        } for depth in STAIR_DEPTHS],
    })
    write_json("worldgen/structure_set/pale_ruins.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 734202611, "separation": 6, "spacing": 12},
        "structures": [{"structure": f"{NAMESPACE}:pale_ruin", "weight": 1}],
    })
    write_json("tags/worldgen/biome/has_structure/pale_ruin.json", {"values": ["minecraft:pale_garden"]})


if __name__ == "__main__":
    main()
