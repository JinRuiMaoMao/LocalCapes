"""Convert a 32x16 Minecraft Dungeons II cape texture into the 64x32 Java cape layout.

Dungeons II layout: columns 0-11 are the outer face (columns 0 and 11 are the side
edges), columns 12-21 are the inner face. Dungeons II has no elytra, so the elytra
region stays transparent and the mod falls back to the vanilla elytra texture.

Usage: python tools/convert_mcd2_cape.py <input.png> <output.png>
"""
import sys

from PIL import Image


def convert(src_path, out_path):
    src = Image.open(src_path).convert("RGBA")
    if src.size != (32, 16):
        raise SystemExit(f"{src_path} is {src.size[0]}x{src.size[1]}, expected 32x16")

    def outer(x, y):
        return src.getpixel((x, y))

    def inner(x, y):
        return src.getpixel((12 + x, y))

    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    for y in range(16):
        for x in range(10):
            out.putpixel((1 + x, 1 + y), outer(1 + x, y))
            out.putpixel((12 + x, 1 + y), inner(x, y))
        out.putpixel((0, 1 + y), outer(0, y))
        out.putpixel((11, 1 + y), outer(11, y))
    for x in range(10):
        out.putpixel((1 + x, 0), outer(1 + x, 0))
        out.putpixel((11 + x, 0), outer(1 + x, 15))

    out.save(out_path)
    print(f"{src_path} -> {out_path}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    convert(sys.argv[1], sys.argv[2])
