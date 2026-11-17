"""Placeholder geometry and textures for the Elderwood, from one spec so the UVs always agree.
Writes the model's boxes (assets/vanillareforged/geometry/elderwood.json), one body texture per
copper stage, the lens glow and the heart overlays. Run it after changing the spec below:

    python3 tools/elderwood_model.py

The part names "tree", "upper", "crown", "right_branch" and "left_branch" are what the client's
ElderwoodModel animates, and the hearts' spots are mirrored by hand in ElderwoodHeart.

Model space: x to the entity's left, y down, z to its back; 16 px = 1 block. Every part hangs off
"tree", whose origin is on the ground under the trunk's center, so y = -height.
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SIZE = 512
CLEAR = (0, 0, 0, 0)
BARK = [(125, 114, 112), (110, 100, 98), (94, 83, 80), (78, 67, 64), (67, 59, 57), (58, 51, 49)]
SPECK = (199, 184, 184)
MOSS = [(150, 160, 150), (120, 132, 122), (96, 108, 100)]
HUSK = [(46, 36, 33), (60, 46, 41), (34, 27, 25)]
DULL = [(92, 70, 58), (78, 58, 48), (64, 48, 40)]
GLOW = [(255, 236, 170), (250, 179, 38), (243, 121, 27), (224, 85, 4), (150, 52, 8)]
COPPER = {  # light, mid, dark, rivet
    "unaffected": [(231, 145, 96), (192, 105, 66), (138, 70, 45), (255, 206, 160)],
    "exposed": [(196, 152, 124), (164, 120, 98), (120, 86, 70), (226, 196, 170)],
    "weathered": [(116, 178, 148), (78, 140, 115), (52, 96, 82), (176, 220, 192)],
    "oxidized": [(96, 196, 166), (62, 156, 130), (40, 106, 92), (158, 230, 206)],
}

# part: (name, parent, pivot xyz, rotation xyz, boxes); box: (kind, x, y, z, w, h, d[, heart name])
PARTS = []


def part(name, parent, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=()):
    PARTS.append((name, parent, pivot, rot, list(boxes)))


def twig(name, parent, at, rot, length, thick=4):
    half = thick / 2
    part(name, parent, (0, -at, 0), rot, [("bark", -half, -length, -half, thick, length, thick)])


def main_branch(side, sign):
    """sign -1 = the entity's right. Points straight up at rest; the model rotates it."""
    name = f"{side}_branch"
    base = 14 if side == "right" else 12
    part(name, "crown", (20 * sign, -72, 0), (0, 0, 0), [
        ("bark", -base / 2, -30, -base / 2, base, 30, base),
        ("bark", -5, -82, -5, 10, 54, 10),
        ("bark", -3, -92, -3, 6, 14, 6),
        ("copper", -base / 2 - 1, -28, -base / 2 - 1, base + 2, 5, base + 2),
        ("heart", -6, -69, -6, 12, 9, 12, f"{side}_branch"),
    ])
    twig(f"{side}_branch_twig_low", name, 49 if sign < 0 else 54, (0.25, 0, 0.8 * sign), 19, 4)
    twig(f"{side}_branch_twig_tip", name, 82, (-0.35, 0, -0.65 * sign), 16, 3)


def crown(name, pivot, rot, length, thick, twigs):
    half = thick / 2
    part(name, "crown", pivot, rot, [("bark", -half, -length, -half, thick, length, thick)])
    for i, (at, twig_rot, twig_length) in enumerate(twigs):
        twig(f"{name}_twig_{i}", name, at, twig_rot, twig_length, 3)


def trunk_section(width, height, depth, top, corner):
    # Three touching strips make a chamfered log without overlapping coplanar faces.
    return [
        ("bark", -width / 2 + corner, top, -depth / 2, width - 2 * corner, height, depth),
        ("bark", -width / 2, top, -depth / 2 + corner, corner, height, depth - 2 * corner),
        ("bark", width / 2 - corner, top, -depth / 2 + corner, corner, height, depth - 2 * corner),
    ]


def band(width, height, depth, top):
    # The wood passes through the collar; its top is a rim rather than a solid copper shelf.
    return [
        ("copper", -width / 2, top, -depth / 2, width, height, 2),
        ("copper", -width / 2, top, depth / 2 - 2, width, height, 2),
        ("copper", -width / 2, top, -depth / 2 + 2, 2, height, depth - 4),
        ("copper", width / 2 - 2, top, -depth / 2 + 2, 2, height, depth - 4),
    ]


# Three layers, so the dying tree can lose its crown, then its upper half, and leave a stump.
part("tree", None, (0, 24, 0), (0, 0, 0),
     trunk_section(48, 32, 48, -32, 8) + trunk_section(40, 24, 40, -56, 6) + band(42, 5, 42, -40) + [
    ("heart", -7, -54, -21, 14, 14, 1, "core"),
    ("heart", -5, -30, -25, 10, 10, 1, "trunk_front"), ("heart", -5, -30, 24, 10, 10, 1, "trunk_back"),
    ("heart", -25, -30, -5, 1, 10, 10, "trunk_right"), ("heart", 24, -30, -5, 1, 10, 10, "trunk_left"),
])
for name, pivot, angle, length, width in [
    ("root_front_right", (-14, -7, -16), 0.4, 27, 14),
    ("root_front_left", (14, -7, -16), -0.4, 23, 13),
    ("root_back_right", (-14, -7, 16), 2.65, 24, 13),
    ("root_back_left", (14, -7, 16), -2.65, 29, 15),
]:
    # The local Y grain follows the root's length; its far end sinks slightly into the ground.
    part(name, "tree", pivot, (math.pi / 2 + 0.12, angle, 0), [
        ("bark", -width / 2, -length, -width / 2, width, length + 4, width),
        ("bark", -width / 2 + 2, -length - 8, -width / 2 + 2, width - 4, 12, width - 4),
    ])
part("upper", "tree", (0, 0, 0), (0, 0, 0),
     trunk_section(40, 46, 40, -102, 6) + trunk_section(32, 24, 32, -126, 5) + [
    ("bark", -12, -138, -9, 22, 12, 20),
    ] + band(42, 5, 42, -88) + [
    # the builders' copper: bands, plates over old wounds, the lens's collar
    ("copper", -21, -78, -8, 1, 20, 16),
    ("copper", 20, -72, -2, 1, 14, 12),
    ("copper", -8, -80, 20, 16, 22, 1),
    ("copper", -9, -73, -24, 3, 18, 4),
    ("copper", 6, -73, -24, 3, 18, 4),
    ("copper", -6, -73, -24, 12, 3, 4),
    ("copper", -6, -58, -24, 12, 3, 4),
    ("lens", -6, -70, -23, 12, 12, 1),
])
part("crown", "upper", (0, 0, 0), (0, 0, 0), [
    # hanging moss
    ("moss", -30, -138, 9, 13, 27, 0), ("moss", 20, -132, 1, 11, 23, 0),
])
main_branch("right", -1)
main_branch("left", 1)
crown("crown_back", (-3, -119, 9), (-0.48, 0, 0.08), 40, 10, [(30, (0, 0, 0.65), 14)])
crown("crown_right", (-10, -122, -1), (0.12, 0, -0.65), 36, 8, [(25, (-0.45, 0, 0.3), 16)])
crown("crown_left", (10, -121, 1), (-0.1, 0, 0.65), 31, 6, [(23, (0.3, 0, -0.6), 13)])


def uv_size(w, h, d):
    return (2 * d + 2 * w, d + h)


def regions(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


# Shelf packing, tallest first.
boxes = [(p[0], i, b) for p in PARTS for i, b in enumerate(p[4])]
order = sorted(boxes, key=lambda e: -uv_size(*[int(math.ceil(n)) for n in e[2][4:7]])[1])
UV = {}
x = y = shelf = 0
for name, i, b in order:
    w, h = uv_size(*[int(math.ceil(n)) for n in b[4:7]])
    if x + w > SIZE:
        x, y, shelf = 0, y + shelf, 0
    UV[(name, i)] = (x, y)
    x += w
    shelf = max(shelf, h)
assert y + shelf <= SIZE, y + shelf


def rgba(c):
    return (*c, 255)


def paint(img, rect, fn):
    x0, y0, w, h = rect
    for py in range(y0, y0 + h):
        for px in range(x0, x0 + w):
            color = fn(px - x0, py - y0, w, h)
            if color is not None:
                img[py][px] = rgba(color)


def bark(rng):
    def fn(px, py, w, h):
        if rng.random() < 0.04:
            return SPECK
        groove = (px + (py // 7) % 2) % 5 == 0
        return BARK[rng.choice((3, 4, 5)) if groove else rng.choice((0, 1, 1, 2, 2, 2))]
    return fn


def copper(stage, rng):
    light, mid, dark, rivet = COPPER[stage]

    def fn(px, py, w, h):
        if w >= 6 and h >= 4 and px in (1, w - 2) and py in (1, h - 2):
            return rivet
        if py == 0:
            return light
        if py == h - 1 or px == w - 1:
            return dark
        return mid if rng.random() < 0.8 else light
    return fn


def disc(palette, cracked=False):
    def fn(px, py, w, h):
        if w < 6 or h < 6:
            return palette[-1]
        dist = math.hypot((px + 0.5) / w * 2 - 1, (py + 0.5) / h * 2 - 1)
        if dist > 1.1:
            return None
        if cracked and (abs(px - py) % 5 == 0 or (px + 2 * py) % 9 == 0):
            return HUSK[0]
        return palette[min(int(dist * 4), len(palette) - 1)]
    return fn


def moss(rng):
    def fn(px, py, w, h):
        strand = px % 3 != 1 and py < h * (0.55 + 0.45 * ((px * 7) % 5) / 4)
        return rng.choice(MOSS) if strand else None
    return fn


def noise(palette, rng):
    return lambda px, py, w, h: rng.choice(palette)


def each_box(kind):
    for name, i, b in boxes:
        if b[0] == kind:
            dims = [int(math.ceil(n)) for n in b[4:7]]
            yield b, regions(*UV[(name, i)], *dims)


def body(stage):
    rng = random.Random(7)
    img = [[CLEAR] * SIZE for _ in range(SIZE)]
    for _, faces in each_box("bark"):
        for rect in faces.values():
            paint(img, rect, bark(rng))
    for _, faces in each_box("copper"):
        for rect in faces.values():
            paint(img, rect, copper(stage, rng))
    for _, faces in each_box("moss"):
        for rect in faces.values():
            paint(img, rect, moss(rng))
    for _, faces in each_box("lens"):
        for rect in faces.values():
            paint(img, rect, lambda px, py, w, h: COPPER[stage][2])
        paint(img, faces["front"], disc(GLOW))
    for b, faces in each_box("heart"):
        # A heart that can't be hit is dull wood; the core hides behind a copper shutter.
        fill = copper(stage, rng) if b[7] == "core" else noise(DULL, rng)
        for rect in faces.values():
            paint(img, rect, fill)
    return img


def heart_overlay(heart, fn_for_face):
    img = [[CLEAR] * SIZE for _ in range(SIZE)]
    for b, faces in each_box("heart"):
        if b[7] != heart:
            continue
        for face, rect in faces.items():
            if face in ("top", "bottom") or rect[2] < 6:
                continue
            paint(img, rect, fn_for_face)
    return img


def lens_glow():
    img = [[CLEAR] * SIZE for _ in range(SIZE)]
    for _, faces in each_box("lens"):
        paint(img, faces["front"], disc(GLOW))
    return img


base = os.path.join(ROOT, "src/main/resources/assets/vanillareforged/textures/entity/elderwood")
os.makedirs(os.path.join(base, "heart"), exist_ok=True)
for stage in COPPER:
    png.write(os.path.join(base, f"elderwood_{stage}.png"), body(stage))
png.write(os.path.join(base, "elderwood_lens.png"), lens_glow())
hearts = sorted({b[7] for _, _, b in boxes if b[0] == "heart"})
husk_rng = random.Random(3)
for heart in hearts:
    png.write(os.path.join(base, "heart", f"{heart}.png"), heart_overlay(heart, disc(GLOW)))
    png.write(os.path.join(base, "heart", f"{heart}_cracked.png"), heart_overlay(heart, disc(GLOW, cracked=True)))
    png.write(os.path.join(base, "heart", f"{heart}_broken.png"), heart_overlay(heart, noise(HUSK, husk_rng)))
png.write(os.path.join(base, "heart", "core_open.png"), heart_overlay("core", disc(GLOW[1:] + [HUSK[0]])))


def number(n):
    return int(n) if float(n).is_integer() else n


def box_json(name, i, b):
    u, v = UV[(name, i)]
    what = b[7] + " heart" if b[0] == "heart" else b[0]
    return json.dumps({"what": what, "uv": [u, v], "from": [number(n) for n in b[1:4]], "size": [number(n) for n in b[4:7]]})


parts_json = []
for name, parent, pivot, rot, part_boxes in PARTS:
    head = {"name": name, "parent": parent, "pivot": [number(n) for n in pivot], "rotation": list(rot)}
    rows = ",\n".join("        " + box_json(name, i, b) for i, b in enumerate(part_boxes))
    parts_json.append("    {" + json.dumps(head)[1:-1] + ', "boxes": [\n' + rows + "\n      ]}")
geometry = os.path.join(ROOT, "src/main/resources/assets/vanillareforged/geometry/elderwood.json")
os.makedirs(os.path.dirname(geometry), exist_ok=True)
with open(geometry, "w") as out:
    out.write('{\n  "texture": [%d, %d],\n  "parts": [\n%s\n  ]\n}\n' % (SIZE, SIZE, ",\n".join(parts_json)))
print("packed height", y + shelf, "hearts", hearts)
