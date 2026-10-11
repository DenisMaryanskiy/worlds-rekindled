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
    part(name, "crown", (20 * sign, -72, 0), (0, 0, 0), [
        ("bark", -5, -92, -5, 10, 92, 10),
        ("copper", -6, -30, -6, 12, 6, 12),
        ("copper", -6, -54, -6, 12, 5, 12),
        ("heart", -6, -69, -6, 12, 9, 12, f"{side}_branch"),
    ])
    twig(f"{side}_branch_twig_low", name, 44, (0.3, 0, 0.8 * sign), 26)
    twig(f"{side}_branch_twig_mid", name, 78, (-0.5, 0, -0.6 * sign), 22)
    twig(f"{side}_branch_twig_tip", name, 90, (0.2, 0, 0.5 * sign), 20, 3)


def crown(name, pivot, rot, length, thick, twigs):
    half = thick / 2
    part(name, "crown", pivot, rot, [("bark", -half, -length, -half, thick, length, thick)])
    for i, (at, twig_rot, twig_length) in enumerate(twigs):
        twig(f"{name}_twig_{i}", name, at, twig_rot, twig_length, 3)


# Three layers, so the dying tree can lose its crown, then its upper half, and leave a stump.
part("tree", None, (0, 24, 0), (0, 0, 0), [
    ("bark", -24, -32, -24, 48, 32, 48),
    ("bark", -20, -56, -20, 40, 24, 40),
    # roots
    ("bark", -7, -12, -44, 14, 12, 20), ("bark", -7, -12, 24, 14, 12, 20),
    ("bark", -44, -12, -7, 20, 12, 14), ("bark", 24, -12, -7, 20, 12, 14),
    ("bark", -36, -8, -36, 13, 8, 13), ("bark", 23, -8, -36, 13, 8, 13),
    ("bark", -36, -8, 23, 13, 8, 13), ("bark", 23, -8, 23, 13, 8, 13),
    ("copper", -21, -40, -21, 42, 5, 42),
    ("heart", -7, -54, -21, 14, 14, 1, "core"),
    ("heart", -5, -30, -25, 10, 10, 1, "trunk_front"), ("heart", -5, -30, 24, 10, 10, 1, "trunk_back"),
    ("heart", -25, -30, -5, 1, 10, 10, "trunk_right"), ("heart", 24, -30, -5, 1, 10, 10, "trunk_left"),
])
part("upper", "tree", (0, 0, 0), (0, 0, 0), [
    ("bark", -20, -96, -20, 40, 40, 40),
    ("bark", -16, -124, -16, 32, 28, 32),
    ("bark", -10, -134, -10, 20, 10, 20),
    # the builders' copper: bands, plates over old wounds, the lens's collar
    ("copper", -21, -88, -21, 42, 5, 42),
    ("copper", -17, -112, -17, 34, 4, 34),
    ("copper", -21, -78, -8, 1, 20, 16),
    ("copper", 20, -72, -2, 1, 14, 12),
    ("copper", -8, -80, 20, 16, 22, 1),
    ("copper", -9, -73, -22, 18, 18, 2),
    ("lens", -6, -70, -23, 12, 12, 1),
])
part("crown", "upper", (0, 0, 0), (0, 0, 0), [
    # hanging moss
    ("moss", -40, -150, 6, 16, 34, 0), ("moss", 22, -146, -4, 16, 30, 0), ("moss", -6, -140, 34, 14, 28, 0),
])
main_branch("right", -1)
main_branch("left", 1)
crown("crown_back", (0, -116, 10), (-0.7, 0, 0), 46, 8, [(26, (0, 0, 0.7), 18), (40, (0.5, 0, -0.6), 16)])
crown("crown_right", (-10, -120, 0), (0.15, 0, -0.65), 44, 8, [(24, (-0.6, 0, 0.3), 18), (38, (0.4, 0, -0.5), 16)])
crown("crown_left", (10, -120, 0), (0.15, 0, 0.65), 44, 8, [(24, (-0.6, 0, -0.3), 18), (38, (0.4, 0, 0.5), 16)])
crown("crown_front", (0, -124, -8), (0.4, 0, 0.1), 36, 6, [(20, (0, 0, -0.7), 14), (32, (0.3, 0, 0.6), 12)])


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
