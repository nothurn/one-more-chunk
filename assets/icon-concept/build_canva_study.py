"""Editable voxel study of the user-supplied Canva composition.

This is a local, AI-assisted modeling aid, not an uploadable Modrinth icon.
Usage: py -3.10 build_canva_study.py <Minecraft 1.20.1 client.jar>
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

from build_blockbench_blockout import SIDES, textures_from_jar, uid


HERE = Path(__file__).resolve().parent
OUTPUT = HERE / "one-more-chunk-reference.bbmodel"
MATERIALS = (
    "moss_block",
    "dirt",
    "dirt_path_top",
    "stone",
    "deepslate",
    "black_concrete",
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
    "lava_still",
)


class Scene:
    def __init__(self) -> None:
        self.elements: list[dict] = []
        self.groups: list[dict] = []
        self.outliner: list[dict] = []
        self.occupied: set[tuple[int, int, int]] = set()

    def group(self, name: str, color: int) -> None:
        group_id = uid(f"canva/group/{name}")
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

    def box(
        self,
        group: str,
        name: str,
        lower: tuple[float, float, float],
        upper: tuple[float, float, float],
        material: str,
        *,
        top: str | None = None,
        bottom: str | None = None,
    ) -> None:
        if any(a >= b for a, b in zip(lower, upper)):
            raise ValueError(f"Invalid box: {name} {lower} {upper}")
        if material not in MATERIALS or (top and top not in MATERIALS):
            raise ValueError(f"Unknown material: {name}")
        box_id = uid(f"canva/box/{group}/{name}/{len(self.elements)}")
        materials = {side: material for side in SIDES}
        materials["up"] = top or material
        materials["down"] = bottom or material
        self.elements.append(
            {
                "name": name,
                "type": "cube",
                "uuid": box_id,
                "box_uv": False,
                "from": list(lower),
                "to": list(upper),
                "origin": [(a + b) / 2 for a, b in zip(lower, upper)],
                "autouv": 0,
                "faces": {
                    side: {"uv": [0, 0, 16, 16], "texture": uid(f"texture/{materials[side]}")}
                    for side in SIDES
                },
            }
        )
        group_id = uid(f"canva/group/{group}")
        next(item for item in self.outliner if item["uuid"] == group_id)["children"].append(box_id)

    def block(
        self,
        group: str,
        x: int,
        y: int,
        z: int,
        material: str,
        *,
        top: str | None = None,
        bottom: str | None = None,
    ) -> None:
        key = (x, y, z)
        if key in self.occupied:
            raise ValueError(f"Overlapping block at {key}")
        self.occupied.add(key)
        self.box(group, f"{material} {x},{y},{z}", key, (x + 1, y + 1, z + 1), material, top=top, bottom=bottom)


def terrain(scene: Scene) -> None:
    group = "01 Chunk - 16 x 16 x 16"
    ores = {
        (3, 6, 15): "coal_ore",
        (4, 6, 15): "coal_ore",
        (5, 7, 15): "coal_ore",
        (3, 5, 15): "coal_ore",
        (15, 7, 5): "iron_ore",
        (15, 7, 6): "iron_ore",
        (15, 6, 7): "iron_ore",
        (15, 5, 5): "iron_ore",
    }
    cave_hole = {(8, 1, 15), (9, 1, 15)}
    for y in range(2, 7):
        width = {2: (6, 10), 3: (6, 11), 4: (7, 11), 5: (8, 10), 6: (9, 9)}[y]
        cave_hole.update((x, y, 15) for x in range(width[0], width[1] + 1))

    path = {(11, z) for z in range(8, 14)} | {(12, z) for z in range(8, 12)}
    for y in range(16):
        for x in range(16):
            for z in range(16):
                if y != 15 and x not in (0, 15) and z not in (0, 15):
                    continue
                if (x, y, z) in cave_hole:
                    continue
                material = "deepslate" if y <= 2 else "stone" if y <= 11 else "dirt"
                material = ores.get((x, y, z), material)
                top = ("dirt_path_top" if (x, z) in path else "moss_block") if y == 15 else None
                scene.block(group, x, y, z, material, top=top)

    # A short grass side plus uneven lower fringe matches the visible block edge.
    rim = "02 Grass edge"
    for i in range(16):
        low = 15.42 - (0.14 if i % 5 in (1, 3) else 0)
        scene.box(rim, f"front grass lip {i}", (i, low, 15.98), (i + 1, 16, 16.02), "moss_block")
        scene.box(rim, f"back grass lip {i}", (i, low, -0.02), (i + 1, 16, 0.02), "moss_block")
        scene.box(rim, f"right grass lip {i}", (15.98, low, i), (16.02, 16, i + 1), "moss_block")
        scene.box(rim, f"left grass lip {i}", (-0.02, low, i), (0.02, 16, i + 1), "moss_block")


def cave(scene: Scene) -> None:
    group = "03 Cave and lava"
    # The visible front wall is truly cut open, not painted black. Short dark
    # side/back walls make it read as a recess when the camera turns.
    for x in range(5, 13):
        for y in range(2, 8):
            scene.block(group, x, y, 13, "black_concrete")
    for x in (5, 12):
        for y in range(2, 6):
            scene.block(group, x, y, 14, "deepslate")
    for x in range(6, 12):
        if x not in (7, 8, 9, 10):
            scene.block(group, x, 1, 14, "deepslate")
    scene.box(group, "recessed lava pool", (7, 1.04, 14.02), (10.5, 2.08, 14.98), "lava_still")
    scene.box(group, "lava stream in mouth", (8.85, 0.25, 15.01), (9.45, 1.23, 15.07), "lava_still")


def tree(scene: Scene) -> None:
    group = "04 Oak tree"
    for y in range(16, 22):
        scene.block(group, 3, y, 12, "oak_log", top="oak_log_top", bottom="oak_log_top")
    tiers = (
        (19, 0, 6, 9, 15),
        (20, 0, 7, 8, 15),
        (21, 0, 7, 8, 15),
        (22, 1, 6, 9, 14),
        (23, 1, 6, 9, 14),
        (24, 2, 5, 10, 13),
        (25, 2, 5, 10, 13),
    )
    for y, min_x, max_x, min_z, max_z in tiers:
        for x in range(min_x, max_x + 1):
            for z in range(min_z, max_z + 1):
                if (x, y, z) in scene.occupied:
                    continue
                if (x in (min_x, max_x)) and (z in (min_z, max_z)):
                    continue
                if (x in (min_x, max_x) or z in (min_z, max_z)) and (3 * x + 5 * z + y) % 11 == 0:
                    continue
                if y == 19 and (x + z) % 7 == 0:
                    continue
                scene.block(group, x, y, z, "azalea_leaves")
    for x, z in ((1, 10), (2, 14), (5, 9), (6, 12)):
        scene.block(group, x, 18, z, "azalea_leaves")
    for x, z in ((3, 11), (4, 11), (3, 12)):
        scene.block(group, x, 26, z, "azalea_leaves")


def house(scene: Scene) -> None:
    group = "05 Half village house"
    for x in range(10, 16):
        for z in range(1, 8):
            scene.block(group, x, 16, z, "cobblestone")
    for y in range(17, 20):
        for x in range(10, 16):
            for z in range(1, 8):
                if x != 10 and z not in (1, 7):
                    continue
                material = "cobblestone" if y == 17 else "oak_planks"
                if x == 10 and z == 4 and y in (17, 18):
                    material = "dark_oak_planks"
                elif z == 7 and x in (12, 13) and y == 18:
                    material = "glass"
                elif (x, z) in ((10, 1), (10, 7), (15, 1), (15, 7)):
                    material = "oak_log"
                scene.block(group, x, y, z, material, top="oak_log_top" if material == "oak_log" else None)

    roof = "06 Stair-stepped oak roof"
    for z in range(0, 9):
        roof_y = 19 + max(0, 4 - min(abs(z - 4), abs(z - 5)))
        for y in range(20, roof_y):
            scene.block(group, 10, y, z, "oak_planks")
        for x in range(9, 16):
            scene.block(roof, x, roof_y, z, "oak_planks")
    for x in range(9, 16):
        scene.box(roof, f"ridge cap {x}", (x, 24, 4.3), (x + 1, 24.5, 5.7), "oak_planks")


def grass_tufts(scene: Scene) -> None:
    group = "07 Grass tufts"
    for x, z in ((2, 8), (5, 13), (7, 4), (8, 11), (11, 14), (9, 8)):
        for index, (dx, dz, height) in enumerate(((0.2, 0.2, 0.8), (0.55, 0.3, 1.2), (0.35, 0.65, 1.0))):
            scene.box(
                group,
                f"tuft {x},{z} blade {index}",
                (x + dx, 16, z + dz),
                (x + dx + 0.14, 16 + height, z + dz + 0.14),
                "azalea_leaves",
            )


def face_default_camera(scene: Scene) -> None:
    """Present the designed front/left sides to Blockbench's initial camera."""
    for element in scene.elements:
        lower = element["from"]
        upper = element["to"]
        element["from"] = [16 - upper[0], lower[1], 16 - upper[2]]
        element["to"] = [16 - lower[0], upper[1], 16 - lower[2]]
        origin = element["origin"]
        element["origin"] = [16 - origin[0], origin[1], 16 - origin[2]]


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: py -3.10 build_canva_study.py <Minecraft 1.20.1 client.jar>")
    scene = Scene()
    for name, color in (
        ("01 Chunk - 16 x 16 x 16", 1),
        ("02 Grass edge", 3),
        ("03 Cave and lava", 7),
        ("04 Oak tree", 3),
        ("05 Half village house", 5),
        ("06 Stair-stepped oak roof", 4),
        ("07 Grass tufts", 2),
    ):
        scene.group(name, color)
    terrain(scene)
    cave(scene)
    tree(scene)
    house(scene)
    grass_tufts(scene)
    face_default_camera(scene)
    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "One More Chunk - Canva composition study",
        "model_identifier": "one_more_chunk_canva_study",
        "credit": "AI-assisted editable study from a user-supplied reference; not publication artwork.",
        "resolution": {"width": 16, "height": 16},
        "elements": scene.elements,
        "groups": scene.groups,
        "outliner": scene.outliner,
        "textures": textures_from_jar(Path(sys.argv[1]), MATERIALS),
    }
    OUTPUT.write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT} ({len(scene.elements)} boxes, {len(MATERIALS)} embedded textures)")


if __name__ == "__main__":
    main()
