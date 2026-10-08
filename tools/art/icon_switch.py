"""
The mod icon, made from the Voice Chat Mic Indicator icon: the same microphone, with the green plus
in its corner swapped for a switch flipped on.

The plus covers a piece of the microphone's arc and foot, so that piece is put back by mirroring
the untouched left half: the microphone is symmetric about x = 65.5, the rounded plate about 63.5.
The switch is drawn smooth (eight times oversampled), like the microphone, not as pixel art.

    python tools/art/icon_switch.py <mic-indicator icon.png> <out.png>
"""
import sys
from PIL import Image, ImageDraw

BACKGROUND = (32, 34, 37)
WHITE = (255, 255, 255)
GREEN = (80, 210, 80)          # the plus's own two greens
GREEN_DARK = (20, 70, 20)

MIC_AXIS = 131                  # x' = 131 - x mirrors the microphone
PLATE_AXIS = 127                # x' = 127 - x mirrors the plate
PATCH = (72, 72, 128, 128)      # the plus and a margin round it


def restore(src):
    out = src.copy()
    s, o = src.load(), out.load()
    left, top, right, bottom = PATCH
    for y in range(top, bottom):
        for x in range(left, right):
            plate = s[PLATE_AXIS - x, y]
            mic = s[MIC_AXIS - x, y]
            if plate[3] == 0:
                o[x, y] = plate
                continue
            # How white the mirrored pixel is: 0 is bare plate, 1 is the microphone, between is its
            # smoothed edge.
            t = max(0.0, min(1.0, (sum(mic[:3]) / 3 - 34) / (255 - 34)))
            o[x, y] = tuple(round(BACKGROUND[i] * (1 - t) + WHITE[i] * t) for i in range(3)) + (plate[3],)
    return out


def switch(size, box):
    """A pill switch flipped on, drawn at size x size so it can be shrunk down smooth."""
    k = size / 128
    x0, y0, x1, y1 = (v * k for v in box)
    h = y1 - y0
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    rim = 3.2 * k
    d.rounded_rectangle([x0, y0, x1, y1], radius=h / 2, fill=GREEN_DARK)
    d.rounded_rectangle([x0 + rim, y0 + rim, x1 - rim, y1 - rim], radius=h / 2 - rim, fill=GREEN)
    # Knob at the "on" end, with the dark rim round it as well.
    gap = rim + 1.6 * k
    kr = h / 2 - gap
    cx, cy = x1 - gap - kr, y0 + h / 2
    d.ellipse([cx - kr - rim, cy - kr - rim, cx + kr + rim, cy + kr + rim], fill=GREEN_DARK)
    d.ellipse([cx - kr, cy - kr, cx + kr, cy + kr], fill=WHITE)
    return layer


def compose(path):
    src = Image.open(path).convert("RGBA")
    base = restore(src)
    big = 128 * 8
    layer = switch(big, (70, 86, 123, 116)).resize((128, 128), Image.LANCZOS)
    base.alpha_composite(layer)
    return base


if __name__ == "__main__":
    compose(sys.argv[1]).save(sys.argv[2])
