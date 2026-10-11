"""Generates the Pale Ruin: its structure templates and the worldgen files that chain them.

The Ruin is a line of jigsaw pieces: the entrance on the surface with its stairway down, ROOM_SLOTS
rooms, a second stairway down and the hall. Every slot has its own pools, so the hall always ends
the line. A slot's room is one of several kinds, and its exit is straight ahead, to the left or to
the right, so every Ruin is furnished and winds differently.

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

# How many Ruin Sentries stand in each room: 7 in all.
SENTRIES_PER_ROOM = [2, 1, 2, 2]
ROOM_SLOTS = len(SENTRIES_PER_ROOM)
# Rooms whose root carries a natural Creaking Heart.
HEART_ROOMS = {2, 4}
STAIR_DEPTH = 20
# How far below the surface the pavilion's floor lies.
SUNK = 3
# The hall's ceiling stays below the rooms' floors, whichever way the line turned.
DESCENT_DEPTH = 16
# For each bearing of the line so far, the exits a room may have and the bearing each leaves it on.
TURNS = {
    "ahead": {"straight": "ahead", "left": "left", "right": "right"},
    "left": {"straight": "left", "right": "ahead"},
    "right": {"straight": "right", "left": "ahead"},
}

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

WALLS = [(TUFF_BRICKS, 1)]
DARK_WALLS = [(DEEPSLATE_BRICKS, 5), ("minecraft:cracked_deepslate_bricks", 1)]
FLOOR = [(DEEPSLATE_TILES, 7), ("minecraft:cracked_deepslate_tiles", 2), ("minecraft:cobbled_deepslate", 1)]
RUBBLE = [("minecraft:cobbled_deepslate", 3), (DEEPSLATE_BRICKS, 1), ("minecraft:tuff", 2)]

LIGHT = {LANTERN: 15}
# Blocks light passes through; everything else a piece places stops it.
CLEAR = {AIR, LANTERN, CHAIN, MOSS_CARPET, HANGING_MOSS, "minecraft:tuff_brick_wall", "minecraft:tuff_brick_slab",
         "minecraft:tuff_brick_stairs", "minecraft:closed_eyeblossom", "minecraft:decorated_pot"}
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

    def jigsaw(self, x, y, z, facing, name, target, pool, final_state):
        self.set(x, y, z, "minecraft:jigsaw", {
            "id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final_state,
            "joint": "aligned", "selection_priority": 0, "placement_priority": 0,
        }, orientation=facing + "_up")

    def entry(self, x, z, y=0, final_state=DEEPSLATE_TILES):
        """Where the piece before this one attaches: in the floor of the doorway on its north side."""
        self.jigsaw(x, y, z, "north", ENTRY, EMPTY, EMPTY, final_state)

    def exit(self, x, z, facing, pool, final_state=DEEPSLATE_TILES):
        self.jigsaw(x, 0, z, facing, f"{NAMESPACE}:pale_ruin/exit", ENTRY, pool, final_state)

    def entity(self, x, y, z, entity_id):
        self.entities.append({"pos": [x + 0.5, float(y), z + 0.5], "blockPos": [x, y, z], "nbt": {"id": entity_id}})

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
        piece.set(x, 2, z, "minecraft:creaking_heart", axis="y", creaking_heart_state="uprooted", natural="true")
    dx, dz = reach
    piece.set(x + dx, top - 1, z, ROOT, axis="x")
    piece.set(x, top - 1, z + dz, ROOT, axis="z")
    piece.set(x + dx, 1, z + dz, ROOT, axis="x")
    for mx, mz in ((x + dx, z), (x, z + dz), (x + 2 * dx, z + dz), (x + dx, z + 2 * dz)):
        piece.set(mx, 0, mz, MOSS)


def stairway(piece, x, z, depth, open_above=None):
    """Stairs three wide running south from z: step i lies i blocks south and i blocks down, with four blocks of headroom."""
    for i in range(depth):
        y = depth - i
        for sx in range(x - 2, x + 3):
            inside = abs(sx - x) <= 1
            piece.set(sx, y - 1, z + i, piece.pick(DARK_WALLS))
            for dy in range(0, 6):
                if open_above is not None and y + dy > open_above and (not inside or dy == 5):
                    continue  # above ground the stairwell is open to the sky
                if dy == 5 or not inside:
                    piece.set(sx, y + dy, z + i, piece.pick(WALLS))
                elif dy == 0:
                    piece.set(sx, y, z + i, "minecraft:tuff_brick_stairs", facing="north", half="bottom", shape="straight",
                              waterlogged="false")
                else:
                    piece.set(sx, y + dy, z + i, AIR)
        if i % 6 == 5 and (open_above is None or y + 5 <= open_above):
            piece.lantern(x, y + 4, z + i, hanging=True)


def entrance():
    """A ruined pavilion, sunk into the ground it was built on, and the stairway down from it, running south."""
    depth = STAIR_DEPTH
    piece = Piece("entrance", (9, depth + 6, depth + 5))
    # The pavilion: a floor on a footing, and what's left of its four columns. On a slope the footing
    # shows, so it reaches well down and ends raggedly, like rubble.
    for x in range(9):
        for z in range(9):
            piece.set(x, depth, z, piece.pick(WALLS if piece.random.random() < 0.8 else [(MOSS, 1)]))
            for down in range(1, piece.random.randint(4, 8)):
                piece.set(x, depth - down, z, piece.pick(DARK_WALLS if down < 3 else RUBBLE))
    # The Ruin starts SUNK blocks below the surface: open air above the floor keeps the pavilion a
    # pit to climb into on level ground, not a room buried under it.
    piece.fill(0, depth + 1, 0, 8, depth + 5, 8, AIR)
    for (x, z), height in zip(((1, 1), (7, 1), (1, 7), (7, 7)), (5, 3, 4, 2)):
        piece.set(x, depth + 1, z, CHISELED_TUFF)
        for y in range(depth + 2, depth + 1 + height):
            piece.set(x, y, z, CALCITE)
    for x, z in ((2, 1), (6, 7), (7, 2), (1, 5), (4, 0)):
        piece.set(x, depth + 1, z, MOSS_CARPET)
    piece.set(6, depth + 1, 1, CALCITE)
    piece.jigsaw(4, depth, 1, "north", ANCHOR, EMPTY, EMPTY, TUFF_BRICKS)

    stairway(piece, 4, 3, depth, open_above=depth)

    # The landing at the foot of the stairs, and the way on.
    end = depth + 4
    corridor(piece, 4, depth + 3, end, top=5)
    piece.lantern(4, 4, depth + 3, hanging=True)
    piece.exit(4, end, "south", pool(1, "ahead"))
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
        piece.set(8, 1, 8, "minecraft:creaking_heart", axis="x", creaking_heart_state="uprooted", natural="true")

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
    for z in (3, 4, 5, 6, 9, 10, 11, 12):
        piece.set(1, 1, z, "minecraft:tuff_brick_slab", type="top", waterlogged="false")
    for x, y, z, facing in ((1, 2, 4, "east"), (1, 2, 11, "east"), (18, 1, 13, "north"), (19, 1, 12, "west"), (19, 1, 6, "west")):
        piece.set(x, y, z, "minecraft:decorated_pot", facing=facing, cracked="false", waterlogged="false")

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


ROOM_KINDS = {"guard": guard_room, "overgrown": overgrown_room, "workshop": workshop}


def room(kind, slot, bearing, exit_side):
    """A room 19 wide, 13 deep and 7 high inside, entered from the north, furnished by its kind."""
    top = 8
    cx, cz = 10, 8
    piece = Piece(room_name(kind, slot, bearing, exit_side), (21, top + 1, 16))
    # Every copy of a slot's room is the same room: only its way out differs.
    piece.random = random.Random(f"{kind}_{slot}")
    corridor(piece, cx, 0, 0)
    shell(piece, 0, 1, 20, 15, top, bands={4})
    doorway(piece, cx, 1, along_x=True)
    piece.entry(cx, 0)
    for x, z in ((3, 3), (17, 3), (3, 13), (17, 13), (10, 3), (10, 13), (10, 8), (3, 8), (17, 8)):
        piece.lantern(x, top - 1, z, hanging=True)

    posts = ROOM_KINDS[kind](piece, top, slot in HEART_ROOMS, SENTRIES_PER_ROOM[slot - 1])

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

    for x, z in posts:
        for y in range(1, 4):
            assert piece.block(x, y, z) in (AIR, MOSS_CARPET, HANGING_MOSS, "minecraft:closed_eyeblossom"), (piece.name, x, y, z)
            piece.set(x, y, z, AIR)
        piece.entity(x, 1, z, f"{NAMESPACE}:ruin_sentry")
    return piece


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
    stairway(piece, 2, 2, depth)
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

    root(piece, 25, 4, top, heart=False, reach=(-1, 1))
    root(piece, 1, 28, top, heart=False, reach=(1, -1))
    overgrow(piece, 1, 4, 25, 28, top, carpets=40, strands=30)
    return piece


def room_name(kind, slot, bearing, exit_side):
    return f"room_{slot}_{kind}_{bearing}_{exit_side}"


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
    pieces = [entrance(), descent(), hall()]
    for slot in range(1, ROOM_SLOTS + 1):
        for bearing in (["ahead"] if slot == 1 else TURNS):
            choices = [(kind, side) for kind in ROOM_KINDS for side in TURNS[bearing]]
            pieces += [room(kind, slot, bearing, side) for kind, side in choices]
            write_pool(f"rooms_{slot}_{bearing}", [(room_name(kind, slot, bearing, side), 1) for kind, side in choices])
    write_pool("entrance", [("entrance", 1)])
    write_pool("descent", [("descent", 1)])
    write_pool("hall", [("hall", 1)])
    for piece in pieces:
        piece.save()
        darkest = piece.darkest_floor()
        print(f"{piece.name}: {piece.size}, {len(piece.blocks)} blocks, {len(piece.entities)} entities, darkest floor {darkest}")
        # The pavilion is under the open sky; everywhere else the builders' lanterns reach.
        assert darkest > 0 or piece.name == "entrance", piece.name

    write_json("worldgen/structure/pale_ruin.json", {
        "type": "minecraft:jigsaw",
        "biomes": f"#{NAMESPACE}:has_structure/pale_ruin",
        "max_distance_from_center": 128,
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "size": ROOM_SLOTS + 4,
        # Nothing but its own guards lives in the Ruin, lit or not.
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": []}},
        # Jigsaw puts the start piece's bottom layer one below this height above the surface, wherever its
        # anchor is: the pavilion's floor is STAIR_DEPTH above that bottom.
        "start_height": {"absolute": 1 - STAIR_DEPTH - SUNK},
        "start_jigsaw_name": ANCHOR,
        "start_pool": pool("entrance"),
        "step": "underground_decoration",
        "terrain_adaptation": "none",
        "use_expansion_hack": False,
    })
    write_json("worldgen/structure_set/pale_ruins.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 734202611, "separation": 6, "spacing": 12},
        "structures": [{"structure": f"{NAMESPACE}:pale_ruin", "weight": 1}],
    })
    write_json("tags/worldgen/biome/has_structure/pale_ruin.json", {"values": ["minecraft:pale_garden"]})


if __name__ == "__main__":
    main()
