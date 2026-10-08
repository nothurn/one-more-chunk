"""Build an editable Blockbench scene for composition work, not a release icon.

Usage on this PC: py -3.10 build_blockbench_blockout.py <Minecraft 1.20.1 client.jar>
The generated .bbmodel embeds unmodified vanilla textures for local preview.
"""

from __future__ import annotations

import base64
import json
import sys
import uuid
import zipfile
from pathlib import Path


HERE = Path(__file__).resolve().parent
OUTPUT = HERE / "one-more-chunk-blockout.bbmodel"
MATERIALS = (
    "moss_block",
    "dirt",
    "stone",
    "bedrock",
    "coal_ore",
    "iron_ore",
    "oak_log",
    "oak_log_top",
    "azalea_leaves",
    "oak_planks",
    "spruce_planks",
    "cobblestone",
    "glass",
    "dark_oak_planks",
)
SIDES = ("north", "east", "south", "west", "up", "down")
NAMESPACE = uuid.UUID("c8530f3d-dab7-41a7-96f8-2411e9954920")


def uid(name: str) -> str:
    return str(uuid.uuid5(NAMESPACE, name))


def textures_from_jar(jar_path: Path, materials: tuple[str, ...] = MATERIALS) -> list[dict]:
    textures = []
    with zipfile.ZipFile(jar_path) as jar:
        for name in materials:
            entry = f"assets/minecraft/textures/block/{name}.png"
            data = jar.read(entry)
            if data[:8] != b"\x89PNG\r\n\x1a\n":
                raise ValueError(f"Not a PNG: {entry}")
            width = int.from_bytes(data[16:20], "big")
            height = int.from_bytes(data[20:24], "big")
            textures.append(
                {
                    "name": f"{name}.png",
                    "uuid": uid(f"texture/{name}"),
                    "id": name,
                    "width": width,
                    "height": height,
                    "uv_width": width,
                    "uv_height": height,
                    "source": "data:image/png;base64," + base64.b64encode(data).decode("ascii"),
                }
            )
    return textures


class Scene:
    def __init__(self) -> None:
        self.elements: list[dict] = []
        self.groups: list[dict] = []
        self.outliner: list[dict] = []
        self.used_positions: set[tuple[int, int, int]] = set()

    def group(self, name: str, color: int) -> None:
        group_id = uid(f"group/{name}")
        self.groups.append(
            {
                "uuid": group_id,
                "name": name,
                "origin": [8, 8, 8],
                "rotation": [0, 0, 0],
                "color": color,
                "export": True,
                "visibility": True,
                "children": [],
            }
        )
        self.outliner.append({"uuid": group_id, "isOpen": False, "children": []})

    def cube(
        self,
        group: str,
        name: str,
        x: int,
        y: int,
        z: int,
        material: str,
        *,
        top: str | None = None,
        bottom: str | None = None,
    ) -> None:
        # Blockbench's default three-quarter view reverses the earlier flat
        # preview's horizontal order. Mirror only the props so the tree reads
        # on the left and the cut house on the right when the file first opens.
        if group != "01 Terrain - 16 x 16":
            x = 15 - x
            y += 8
            name = f"{name.split()[0]} {x},{y},{z}"
        key = (x, y, z)
        if key in self.used_positions:
            raise ValueError(f"Overlapping block at {key}: {name}")
        self.used_positions.add(key)
        face_materials = {side: material for side in SIDES}
        face_materials["up"] = top or material
        face_materials["down"] = bottom or material
        cube_id = uid(f"cube/{group}/{x}/{y}/{z}")
        self.elements.append(
            {
                "name": name,
                "type": "cube",
                "uuid": cube_id,
                "box_uv": False,
                "from": [x, y, z],
                "to": [x + 1, y + 1, z + 1],
                "origin": [x + 0.5, y + 0.5, z + 0.5],
                "autouv": 0,
                "faces": {
                    side: {
                        "uv": [0, 0, 16, 16],
                        "texture": uid(f"texture/{face_materials[side]}"),
                    }
                    for side in SIDES
                },
            }
        )
        entry = next(item for item in self.outliner if item["uuid"] == uid(f"group/{group}"))
        entry["children"].append(cube_id)


def build_scene() -> Scene:
    scene = Scene()
    for name, color in (
        ("01 Terrain - 16 x 16", 1),
        ("02 Oak tree", 3),
        ("03 Half village house", 5),
        ("04 Roof", 4),
    ):
        scene.group(name, color)

    terrain = "01 Terrain - 16 x 16"
    # A 16 x 16 x 16 exterior shell gives the island a real chunk-sized
    # silhouette without putting all hidden interior blocks in the outliner.
    visible_ores = {
        (15, 4, 4): "coal_ore",
        (15, 5, 5): "coal_ore",
        (15, 8, 11): "iron_ore",
        (15, 8, 12): "iron_ore",
        (3, 6, 15): "iron_ore",
        (4, 6, 15): "iron_ore",
        (10, 3, 15): "coal_ore",
        (11, 3, 15): "coal_ore",
    }
    for y in range(16):
        for x in range(16):
            for z in range(16):
                if y != 15 and x not in (0, 15) and z not in (0, 15):
                    continue
                material = "bedrock" if y == 0 else "stone" if y <= 10 else "dirt"
                material = visible_ores.get((x, y, z), material)
                top = "moss_block" if y == 15 else None
                scene.cube(terrain, f"ground {x},{y},{z}", x, y, z, material, top=top)

    tree = "02 Oak tree"
    for y in range(8, 13):
        scene.cube(tree, f"trunk {y}", 2, y, 3, "oak_log", top="oak_log_top", bottom="oak_log_top")
    for x in range(5):
        for z in range(1, 6):
            if (x in (0, 4) and z in (1, 5)) or (x, z) == (2, 3):
                continue
            scene.cube(tree, f"canopy lower {x},{z}", x, 12, z, "azalea_leaves")
    for x in range(1, 4):
        for z in range(2, 5):
            scene.cube(tree, f"canopy upper {x},{z}", x, 13, z, "azalea_leaves")
    scene.cube(tree, "canopy crown", 2, 14, 3, "azalea_leaves")

    house = "03 Half village house"
    for x in range(10, 16):
        for z in range(6, 12):
            scene.cube(house, f"foundation {x},{z}", x, 8, z, "cobblestone")
    for y in range(9, 12):
        for x in range(10, 16):
            for z in range(6, 12):
                if x != 10 and z not in (6, 11):
                    continue
                material = "oak_planks"
                if x == 10 and z == 8 and y in (9, 10):
                    material = "dark_oak_planks"
                elif z == 11 and x == 12 and y == 10:
                    material = "glass"
                scene.cube(house, f"wall {x},{y},{z}", x, y, z, material)

    roof = "04 Roof"
    for z in range(5, 13):
        roof_y = 11 + max(0, 3 - min(abs(z - 8), abs(z - 9)))
        for y in range(12, roof_y):
            scene.cube(house, f"gable {y},{z}", 10, y, z, "oak_planks")
        for x in range(9, 16):
            scene.cube(roof, f"roof {x},{z}", x, roof_y, z, "spruce_planks")

    return scene


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: py -3.10 build_blockbench_blockout.py <Minecraft 1.20.1 client.jar>")
    textures = textures_from_jar(Path(sys.argv[1]))
    scene = build_scene()
    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "One More Chunk - editable icon blockout",
        "model_identifier": "one_more_chunk_icon_blockout",
        "credit": "AI-assisted editable blockout; not approved publication artwork.",
        "resolution": {"width": 16, "height": 16},
        "elements": scene.elements,
        "groups": scene.groups,
        "outliner": scene.outliner,
        "textures": textures,
    }
    OUTPUT.write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT} ({len(scene.elements)} cubes, {len(textures)} embedded textures)")


if __name__ == "__main__":
    main()
