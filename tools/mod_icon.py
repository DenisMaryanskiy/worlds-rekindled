"""Draw the Vanilla Reforged mark on a 40px grid; export without interpolation."""
from pathlib import Path

import png


ROOT = Path(__file__).resolve().parents[1]
SIZE = 40
COLORS = {
    "background": (30, 37, 35),
    "border": (52, 65, 54),
    "inset": (38, 47, 41),
    "shadow": (21, 26, 25),
    "outline": (29, 28, 24),
    "grass": (116, 148, 70),
    "grass_light": (153, 175, 88),
    "grass_dark": (85, 119, 60),
    "grass_left": (86, 119, 57),
    "grass_right": (63, 91, 47),
    "earth": (124, 86, 57),
    "earth_light": (154, 111, 72),
    "earth_dark": (101, 70, 49),
    "earth_right": (88, 63, 46),
    "earth_right_light": (111, 79, 53),
    "stone": (143, 136, 110),
    "iron_light": (177, 182, 166),
    "iron": (111, 124, 114),
    "iron_dark": (71, 85, 79),
    "iron_shadow": (48, 61, 57),
    "ember_dark": (157, 72, 35),
    "ember": (227, 133, 48),
    "ember_light": (255, 211, 111),
}


def draw():
    image = [[(*COLORS["background"], 255) for _ in range(SIZE)] for _ in range(SIZE)]

    def rect(x, y, width, height, color):
        for py in range(y, y + height):
            for px in range(x, x + width):
                image[py][px] = (*COLORS[color], 255)

    def polygon(points, color):
        # Test pixel centers so diagonal edges have a consistent, hard staircase.
        for y in range(SIZE):
            for x in range(SIZE):
                inside = False
                previous = points[-1]
                for current in points:
                    ax, ay = previous
                    bx, by = current
                    if (ay > y + 0.5) != (by > y + 0.5):
                        crossing = ax + (y + 0.5 - ay) * (bx - ax) / (by - ay)
                        if x + 0.5 < crossing:
                            inside = not inside
                    previous = current
                if inside:
                    image[y][x] = (*COLORS[color], 255)

    # A quiet inset frame keeps the silhouette readable on either site theme.
    rect(3, 2, 34, 1, "border")
    rect(2, 3, 1, 34, "border")
    rect(3, 37, 34, 1, "inset")
    rect(37, 3, 1, 34, "inset")
    rect(9, 36, 22, 1, "shadow")

    # Anvil: broad horn, narrow waist, planted feet.
    polygon([(5, 25), (34, 25), (34, 27), (29, 30), (25, 30),
             (25, 32), (29, 34), (29, 36), (10, 36), (10, 34),
             (14, 32), (14, 30), (10, 29)], "outline")
    polygon([(6, 26), (33, 26), (29, 28), (11, 28)], "iron")
    rect(8, 26, 24, 1, "iron_light")
    rect(12, 28, 16, 1, "iron_dark")
    rect(15, 29, 9, 4, "iron_shadow")
    rect(15, 29, 2, 4, "iron_dark")
    polygon([(15, 33), (24, 33), (27, 35), (12, 35)], "iron_dark")
    rect(12, 35, 15, 1, "iron_shadow")
    rect(15, 33, 8, 1, "iron")

    # Original grass-block artwork: no extracted Minecraft textures.
    polygon([(20, 5), (33, 12), (33, 22), (20, 29), (7, 22), (7, 12)], "outline")
    polygon([(8, 12), (20, 18), (20, 28), (8, 22)], "earth")
    polygon([(20, 18), (32, 12), (32, 22), (20, 28)], "earth_right")
    polygon([(20, 6), (32, 12), (20, 18), (8, 12)], "grass")
    polygon([(8, 12), (20, 18), (20, 21), (8, 15)], "grass_left")
    polygon([(20, 18), (32, 12), (32, 15), (20, 21)], "grass_right")

    # Small connected clusters suggest turf and earth without noisy dithering.
    for x, y, w, h in [(17, 8, 4, 1), (13, 10, 3, 1), (17, 11, 2, 1),
                        (22, 10, 3, 1), (24, 12, 3, 1), (17, 14, 3, 1)]:
        rect(x, y, w, h, "grass_light")
    for x, y, w in [(20, 12, 3), (12, 12, 2), (21, 15, 2), (27, 11, 1)]:
        rect(x, y, w, 1, "grass_dark")
    for x, y in [(9, 15), (14, 18), (17, 20)]:
        rect(x, y, 1, 1, "grass_left")
    for x, y in [(23, 19), (28, 16)]:
        rect(x, y, 1, 1, "grass_right")
    for x, y, w in [(9, 17, 2), (13, 20, 2), (10, 21, 1), (16, 23, 2), (17, 25, 1)]:
        rect(x, y, w, 1, "earth_light")
    for x, y, w in [(11, 18, 2), (15, 22, 2), (12, 23, 2), (18, 24, 1)]:
        rect(x, y, w, 1, "earth_dark")
    for x, y, w in [(27, 18, 2), (23, 22, 2), (29, 20, 2), (22, 25, 1)]:
        rect(x, y, w, 1, "earth_right_light")
    rect(10, 19, 1, 1, "stone")
    rect(16, 24, 2, 1, "stone")

    # The warm seam and one flying spark carry the "reforged" idea at small sizes.
    for x, y in [(20, 19), (20, 20), (19, 21), (19, 22), (20, 23),
                 (20, 24), (19, 25), (19, 26), (20, 27)]:
        rect(x - 1, y, 2, 1, "ember_dark")
        rect(x, y, 1, 1, "ember")
    rect(20, 19, 1, 2, "ember_light")
    rect(19, 21, 1, 1, "ember_light")
    rect(34, 19, 1, 3, "ember")
    rect(33, 20, 3, 1, "ember")
    rect(34, 20, 1, 1, "ember_light")
    return image


def enlarge(pixels, factor):
    return [
        [pixel for pixel in row for _ in range(factor)]
        for row in pixels for _ in range(factor)
    ]


if __name__ == "__main__":
    pixels = draw()
    destination = ROOT / "docs/publishing/curseforge"
    destination.mkdir(parents=True, exist_ok=True)
    png.write(destination / "icon.png", enlarge(pixels, 10))
    png.write(ROOT / "src/main/resources/assets/vanillareforged/icon.png", enlarge(pixels, 4))
