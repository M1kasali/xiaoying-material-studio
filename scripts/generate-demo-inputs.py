"""Create two original uploadable PNG illustrations with Python's standard library."""
import math
import struct
import zlib
from pathlib import Path


def chunk(kind, data):
    return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)


def draw(variant):
    width, height = 480, 640
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            sky = (224, 236, 222) if variant == 1 else (235, 216, 198)
            sea = (48, 114, 122) if variant == 1 else (62, 104, 108)
            sand = (224, 184, 130) if variant == 1 else (211, 157, 128)
            color = sky
            if (x - 340) ** 2 + (y - 130) ** 2 < 49 ** 2:
                color = (222, 130, 91)
            if y > 260 + 12 * math.sin(x / 110 + variant):
                color = sea
            coast = 465 + 40 * math.sin(x / 130 + variant)
            if coast - 7 < y <= coast:
                color = (222, 232, 208)
            if y > coast:
                color = sand
            if 295 < y < 430 and int(y + 5 * math.sin(x / 32)) % 43 < 2 and 35 < x < 430:
                color = (133, 177, 166)
            # A quiet printed border, like a small travel card.
            if x < 20 or x >= width - 20 or y < 20 or y >= height - 20:
                color = (246, 239, 221)
            row.extend(color)
        rows.append(bytes(row))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', width, height, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(b''.join(rows))) + chunk(b'IEND', b''))


if __name__ == '__main__':
    output = Path(__file__).resolve().parents[1] / 'frontend' / 'public' / 'demo'
    output.mkdir(parents=True, exist_ok=True)
    for variant in (1, 2):
        destination = output / f'input-{variant:02}.png'
        destination.write_bytes(draw(variant))
        print(destination.name)
