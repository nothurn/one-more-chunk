"""Generate the small deterministic empty structure used by Forge GameTests."""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


def name(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def list_tag(label: str, element_type: int, count: int, payload: bytes = b"") -> bytes:
    return bytes((9,)) + name(label) + bytes((element_type,)) + struct.pack(">i", count) + payload


root = bytearray(bytes((10,)) + name(""))
root += list_tag("size", 3, 3, struct.pack(">iii", 1, 1, 1))
root += list_tag("palette", 10, 1, bytes((8,)) + name("Name") + name("minecraft:air") + bytes((0,)))
root += list_tag("blocks", 10, 0)
root += list_tag("entities", 10, 0)
root += bytes((3,)) + name("DataVersion") + struct.pack(">i", 3465)
root += bytes((0,))

target = Path(__file__).resolve().parents[1] / "src/main/resources/data/nightfallchunks/structures/empty.nbt"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.compress(bytes(root), mtime=0))
print(target)
