"""Refresh the remaining placeholder materials and painting without changing any model UVs."""
from pathlib import Path

import png
from texture_materials import BARK, bark_pixel


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vanillareforged/textures"
CLEAR = (0, 0, 0, 0)
STONE = [(62, 63, 59), (82, 82, 78), (106, 106, 99), (133, 133, 122), (160, 160, 146)]


def write(name, pixels):
    png.write(ROOT / f"{name}.png", pixels)


def sentries():
    # Replace only the old bark palette; the pumpkin, heart and UV cutouts retain their pixels.
    old_bark = {*BARK, (199, 184, 184)}
    for name in ("heartwood_sentry", "heartwood_sentry_jack_o_lantern", "ruin_sentry"):
        _, _, pixels = png.read(ROOT / f"entity/heartwood_sentry/{name}.png")
        for y, row in enumerate(pixels):
            for x, color in enumerate(row):
                if color[3] and color[:3] in old_bark:
                    row[x] = (*bark_pixel(x, y + 7), color[3])
        write(f"entity/heartwood_sentry/{name}", pixels)
        if name == "heartwood_sentry_jack_o_lantern":
            continue
        statue = []
        for row in pixels:
            stone_row = []
            for r, g, b, alpha in row:
                brightness = (r * 3 + g * 6 + b) // 10
                shade = min(4, max(0, (brightness - 25) // 34))
                stone_row.append((*STONE[shade], alpha) if alpha else CLEAR)
            statue.append(stone_row)
        statue_name = "heartwood_sentry_statue" if name == "heartwood_sentry" else "ruin_sentry_statue"
        write(f"entity/heartwood_sentry/{statue_name}", statue)


def roots():
    write("entity/elderwood/elderwood_roots", [
        [(*bark_pixel(x, y), 255) for x in range(64)] for y in range(64)
    ])


def ruin_marker():
    colors = {
        ".": CLEAR,
        "d": (73, 65, 49, 255),
        "b": (145, 128, 91, 255),
        "h": (220, 203, 157, 255),
        "a": (243, 121, 27, 255),
    }
    rows = [
        "........",
        "...hh...",
        "..hbbd..",
        ".hbbbbd.",
        "..dbbd..",
        "..daad..",
        ".hbbbbb.",
        "..ddddd.",
    ]
    write("map/decorations/pale_ruin", [[colors[c] for c in row] for row in rows])


def painting():
    # A view of the builders' tree: pale bark against the woods, copper bands and seven hearts.
    palette = {
        "sky": (74, 82, 70), "haze": (85, 93, 79), "woods": (59, 69, 59),
        "far": (67, 78, 66), "leaf": (48, 59, 49), "floor": (44, 49, 44),
        "stone": (90, 96, 81), "stone_shade": (63, 71, 60),
        "outline": (40, 39, 34), "bark_shade": (81, 75, 64),
        "bark": (125, 115, 95), "bark_light": (169, 155, 124),
        "copper_shade": (52, 84, 67), "copper": (83, 124, 94),
        "copper_light": (132, 157, 112), "resin_shade": (128, 42, 26),
        "resin": (224, 85, 4), "gold": (250, 179, 38), "glint": (255, 219, 177),
    }
    colors = {name: (*rgb, 255) for name, rgb in palette.items()}
    image = [[colors["sky"] for _ in range(32)] for _ in range(48)]

    def rect(x, y, w, h, color):
        for py in range(max(0, y), min(48, y + h)):
            for px in range(max(0, x), min(32, x + w)):
                image[py][px] = colors[color]

    def line(start, end, color, width=1):
        x, y = start
        end_x, end_y = end
        dx, dy = abs(end_x - x), -abs(end_y - y)
        sx, sy = (1 if x < end_x else -1), (1 if y < end_y else -1)
        error = dx + dy
        while True:
            rect(x - width // 2, y - width // 2, width, width, color)
            if (x, y) == (end_x, end_y):
                break
            twice = 2 * error
            if twice >= dy:
                error += dy
                x += sx
            if twice <= dx:
                error += dx
                y += sy

    rect(0, 10, 32, 14, "haze")
    rect(0, 24, 32, 16, "woods")
    for x, top, width in ((1, 2, 2), (6, 5, 2), (10, 1, 1), (24, 4, 2), (29, 0, 2)):
        rect(x, top, width, 42 - top, "far")
        line((x, top + 9), (x - 3, top + 4), "far")
        line((x, top + 15), (x + 4, top + 10), "far")
    rect(0, 38, 32, 10, "floor")
    for x, y, width in ((0, 23, 4), (3, 28, 3), (27, 20, 5), (25, 30, 4), (0, 34, 5)):
        rect(x, y, width, 40 - y, "leaf")
    # The hall's two weathered columns frame the tree without becoming an ornamental border.
    for x in (0, 29):
        rect(x, 15, 3, 27, "stone_shade")
        rect(x, 15, 1, 25, "stone")
        rect(x, 14, 4, 2, "stone")
        rect(x, 39, 4, 3, "stone")

    branches = [
        ((15, 26), (9, 17), 5), ((9, 17), (7, 7), 4),
        ((8, 12), (3, 8), 2), ((7, 8), (9, 3), 2),
        ((17, 24), (23, 14), 5), ((23, 14), (25, 5), 4),
        ((24, 10), (29, 6), 2), ((24, 7), (22, 2), 2),
        ((16, 20), (16, 7), 4), ((16, 11), (13, 5), 2),
        ((17, 13), (20, 8), 2),
    ]
    for start, end, width in branches:
        line(start, end, "outline", width + 2)
    rect(11, 22, 11, 19, "outline")
    for start, end, width in branches:
        line(start, end, "bark_shade", width)
        line((start[0] - 1, start[1]), (end[0] - 1, end[1]), "bark", max(1, width - 2))
    rect(12, 23, 9, 17, "bark_shade")
    rect(12, 23, 3, 17, "bark")
    rect(12, 26, 1, 8, "bark_light")
    rect(16, 25, 2, 14, "bark")
    rect(19, 29, 1, 7, "outline")
    for start, end, width in [((14, 38), (8, 43), 3), ((18, 38), (25, 42), 3), ((14, 40), (12, 45), 2)]:
        line(start, end, "outline", width + 2)
        line(start, end, "bark_shade", width)
        line(start, end, "bark")
    for x, y, width in ((12, 24, 9), (12, 36, 9), (5, 11, 5), (21, 10, 5)):
        rect(x, y, width, 2, "copper_shade")
        rect(x, y, width - 1, 1, "copper")
        rect(x, y, 1, 1, "copper_light")
    rect(14, 18, 5, 5, "copper_shade")
    rect(14, 18, 4, 1, "copper_light")
    rect(15, 19, 3, 3, "resin")
    rect(15, 19, 2, 2, "gold")
    rect(15, 19, 1, 1, "glint")
    for x, y in ((7, 16), (23, 15), (13, 28), (19, 29), (15, 33), (13, 37), (19, 37)):
        rect(x - 1, y - 1, 3, 3, "resin_shade")
        rect(x, y - 1, 1, 2, "gold")
        rect(x + 1, y, 1, 1, "resin")
    rect(3, 44, 5, 1, "stone_shade")
    rect(24, 45, 5, 1, "stone_shade")
    rect(4, 42, 1, 2, "leaf")
    rect(27, 43, 1, 2, "leaf")
    write("painting/elderwood", image)


if __name__ == "__main__":
    sentries()
    roots()
    ruin_marker()
    painting()
