import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
CAPES = ROOT / "common/src/main/resources/assets/localcapes/textures/cape"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/1.20.1/minecraft-client.jar"
OUT = ROOT / "cover.png"

W, H = 1440, 900
SCALE = 7
CAPE_W, CAPE_H = 10 * SCALE, 16 * SCALE
GAP = 20
COLS = 15
ROWS_TOP, ROWS_BOTTOM = 2, 2

JAVA_ORDER = [
    "account/pan", "account/migrator", "account/vanilla", "account/common",
    "staff/mojang_classic", "staff/mojang", "staff/microsoft_xbox_360", "staff/4j_studios", "staff/mojang_studios",
    "event_physical/minecon_2011", "event_physical/minecon_2012", "event_physical/minecon_2013",
    "event_physical/minecon_2015", "event_physical/minecon_2016", "event_physical/minecraft_experience",
    "event_physical/moonlight_trail", "event_physical/crafter",
    "event_virtual/founders", "event_virtual/progress_pride", "event_virtual/cherry_blossom",
    "event_virtual/followers", "event_virtual/purple_heart", "event_virtual/15th_anniversary",
    "event_virtual/mcc_15th_year", "event_virtual/mojang_office", "event_virtual/home", "event_virtual/menace",
    "event_virtual/yearn", "event_virtual/copper", "event_virtual/zombie_horse", "event_virtual/builder",
    "event_virtual/aurora",
    "personal/bacon", "personal/millionth_customer", "personal/dannybstyle", "personal/julianclark",
    "personal/cheapsh0t", "personal/mrmessiah", "personal/prismarine", "personal/turtle", "personal/birthday",
    "personal/valentine", "personal/oxeye", "personal/blueprint",
    "competition/scrolls_champion", "competition/cobalt",
    "volunteer/translator", "volunteer/chinese_translator", "volunteer/moderator", "volunteer/mapmaker",
]
DUNGEONS = [
    "dungeons/hero", "dungeons/glow", "dungeons/luminous_night", "dungeons/prism", "dungeons/amethyst",
    "dungeons/iceologer", "dungeons/cloudy_climb", "dungeons/phantom",
    "dungeons2/twisted", "dungeons2/hero_mcd2",
]


def cape_front(rel):
    tex = Image.open(CAPES / f"{rel}.png").convert("RGBA")
    return tex.crop((1, 1, 11, 17)).resize((CAPE_W, CAPE_H), Image.NEAREST)


def load_font():
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        sheet = Image.open(BytesIO(jar.read("assets/minecraft/textures/font/ascii.png"))).convert("RGBA")
    glyphs = {}
    for code in range(32, 127):
        gx, gy = (code % 16) * 8, (code // 16) * 8
        glyph = sheet.crop((gx, gy, gx + 8, gy + 8))
        alpha = glyph.getchannel("A")
        box = alpha.getbbox()
        width = 3 if code == 32 else (box[2] if box else 0)
        glyphs[chr(code)] = (glyph, width)
    return glyphs


def text_size(glyphs, text, scale):
    width = sum(glyphs[c][1] + 1 for c in text) - 1
    return width * scale, 8 * scale


def draw_text(canvas, glyphs, text, x, y, scale, color, shadow):
    def paint(ox, oy, rgb):
        cx = ox
        for c in text:
            glyph, width = glyphs[c]
            mask = glyph.getchannel("A").resize((8 * scale, 8 * scale), Image.NEAREST)
            layer = Image.new("RGBA", mask.size, rgb + (255,))
            canvas.paste(layer, (cx, oy), mask)
            cx += (width + 1) * scale

    paint(x + scale, y + scale, shadow)
    paint(x, y, color)


def background():
    bg = Image.new("RGBA", (W, H))
    top, bottom = (10, 14, 40), (34, 20, 70)
    draw = ImageDraw.Draw(bg)
    for y in range(H):
        t = y / (H - 1)
        draw.line([(0, y), (W, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)) + (255,))
    rng = random.Random(7)
    for _ in range(260):
        x, y = rng.randrange(W), rng.randrange(H)
        size = rng.choice([3, 3, 3, 6])
        shade = rng.randrange(120, 230)
        draw.rectangle([x, y, x + size - 1, y + size - 1], fill=(shade, shade, min(255, shade + 25), 255))
    return bg


def main():
    capes = [cape_front(rel) for rel in JAVA_ORDER + DUNGEONS]
    rows = ROWS_TOP + ROWS_BOTTOM
    assert len(capes) == COLS * rows, len(capes)

    canvas = background()
    grid_w = COLS * CAPE_W + (COLS - 1) * GAP
    left = (W - grid_w) // 2
    margin_y = 40
    row_y = [margin_y + r * (CAPE_H + GAP) for r in range(ROWS_TOP)]
    row_y += [H - margin_y - CAPE_H - r * (CAPE_H + GAP) for r in reversed(range(ROWS_BOTTOM))]

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    placed = []
    for i, cape in enumerate(capes):
        r, c = divmod(i, COLS)
        x = left + c * (CAPE_W + GAP)
        y = row_y[r]
        placed.append((cape, x, y))
        glow.paste(cape, (x, y), cape)
        shadow.paste(Image.new("RGBA", cape.size, (0, 0, 0, 170)), (x + SCALE, y + SCALE), cape)

    glow = glow.filter(ImageFilter.GaussianBlur(14))
    glow.putalpha(glow.getchannel("A").point(lambda a: int(a * 0.55)))
    canvas = Image.alpha_composite(canvas, glow)
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(3)))
    for cape, x, y in placed:
        canvas.paste(cape, (x, y), cape)

    band_top = row_y[ROWS_TOP - 1] + CAPE_H + GAP
    band_bottom = row_y[ROWS_TOP] - GAP
    band = Image.new("RGBA", (W, band_bottom - band_top), (6, 8, 24, 200))
    canvas.alpha_composite(band, (0, band_top))
    line = Image.new("RGBA", (W, 4), (255, 214, 90, 255))
    canvas.alpha_composite(line, (0, band_top))
    canvas.alpha_composite(line, (0, band_bottom - 4))

    glyphs = load_font()
    title, title_scale = "LocalCapes", 11
    tw, th = text_size(glyphs, title, title_scale)
    sub, sub_scale = "Bundled & custom capes  |  Fabric & Forge 1.20.1", 4
    sw, sh = text_size(glyphs, sub, sub_scale)
    gap = 28
    block = th + gap + sh
    ty = band_top + (band_bottom - band_top - block) // 2
    draw_text(canvas, glyphs, title, (W - tw) // 2, ty, title_scale, (255, 255, 255), (63, 63, 63))
    draw_text(canvas, glyphs, sub, (W - sw) // 2, ty + th + gap, sub_scale, (255, 214, 90), (63, 53, 22))

    canvas.convert("RGB").save(OUT, optimize=True)
    print(OUT, canvas.size, "band", band_top, band_bottom)


if __name__ == "__main__":
    main()
