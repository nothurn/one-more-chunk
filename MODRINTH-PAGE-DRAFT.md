# Modrinth publishing draft — One More Chunk

Prepared for a possible first public beta. This document does not authorize an upload or establish eligibility under Modrinth's content rules.

## Project fields

- Project type: **Mod**
- Name: **One More Chunk**
- Suggested URL slug: `one-more-chunk` (availability not checked)
- Short description: **Start on one natural chunk. Craft Chunk Seeds to reveal the world, one neighbor at a time.**
- Categories: **Adventure**, **Game Mechanics**, **World Generation** (select only if Modrinth offers these exact categories)
- License: **All Rights Reserved**
- Minecraft: **1.20.1**
- Loader: **Forge** (minimum 47.4.20)
- Java: **17**
- Environment: **Client and server** (the mod is installed on both sides; multiplayer play remains unverified)
- Dependencies: **Forge only**; no separate library mod is required
- Project icon: **Not ready.** Do not use the generated concept art as a Modrinth icon.

## Project description — copy-ready English text

### One chunk to begin. A world to uncover.

**One More Chunk** turns a new Minecraft world into a one-chunk survival challenge. You begin on a single, full-height, naturally generated Overworld chunk surrounded by void. Each **Chunk Seed** reveals one neighboring chunk at its real world coordinates. Hills, caves, water, biomes, and neighboring terrain come from the same world seed, so newly revealed land meets the existing world without an artificial height offset.

#### How it works

1. Create a new world. **One Chunk** is preselected, but you can switch back to **Normal** before creation.
2. Open the starter chest for basic supplies and your first two Chunk Seeds.
3. Face an adjacent locked chunk and use a Chunk Seed to reveal it.
4. Craft more seeds with **one log, four cobblestone, and one coal or charcoal**. The recipe appears in the recipe book after you pick up a starter seed.
5. Press **J** for the Chunk Journal, which shows nearby revealed chunks and your total. Press **R** in the journal to refresh it. The key can be remapped in Controls.

Unlock feedback appears briefly above the hotbar instead of adding a chat message for every chunk. Your unlocked chunks are saved with the world. The Nether and End keep their normal world generation.

#### Beta scope

This beta targets **Minecraft 1.20.1 with Forge 47.4.20 or newer**. Create a new world for the intended experience. The source terrain's live entities are not copied: a revealed village may contain its buildings and loot but not its original villagers. Multiplayer play, dedicated-server use, structure locating, and complete survival progression have not been independently verified. Development worlds from `0.1.0-dev` are not migration targets; already generated starter terrain from `0.2.0-dev` is not rebuilt.

The chunk-by-chunk survival idea was inspired by [Chunk By Chunk](https://github.com/immortius/chunkbychunk) by dividesBy0/immortius. **One More Chunk is an independent implementation** with its own handheld-seed interaction and progress journal; it does not bundle the original mod or its assets.

## First version fields

- Version number: **`1.0.0-beta`**
- Version title: **One More Chunk 1.0.0-beta — Initial Beta**
- Release type: **Beta**, not Release
- Game version / loader: **Minecraft 1.20.1 / Forge**
- File to upload only after all gates are resolved: `B:\Claude\Projekte\ModBuilds\forge-1.20.1\nightfallchunks-1.0.0-beta.jar`
- Version changelog: use `RELEASE-NOTES-1.0.0-beta.md` verbatim or as the source; do not invent categorized fixes.

## Before any public upload

- Confirm the exact staged JAR, its hash, and an in-game launch with that exact JAR. Local tests are not a substitute for Noah's final gameplay check.
- Confirm public-project eligibility under [Modrinth's AI-use policy](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai). Significant AI assistance in code, text, and any assets must be disclosed accurately. A project created almost entirely through prompting/testing without substantial human-authored work may not be publicly listed. Noah's own code/asset contribution has not yet been established; do not claim eligibility or evade this gate.
- Do not upload AI-generated or AI-assisted concept artwork as a project icon/gallery image. A genuinely human-created compliant icon or genuine in-game screenshot can be assessed separately.
- Confirm that the slug and categories actually exist, review the page and file metadata, and publish only on Noah's explicit instruction.
