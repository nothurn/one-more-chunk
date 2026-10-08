# One More Chunk 1.0.0-beta — Initial Beta

Begin on a single, full-height, naturally generated Overworld chunk. Use Chunk Seeds to reveal neighboring chunks one at a time. The terrain comes from the same world seed at its original coordinates, so adjoining cliffs, caves, water, and biomes line up without an artificial starter-platform height.

## In this beta

- **One Chunk** is preselected when creating a new world; **Normal** remains selectable.
- A starter chest provides basic supplies and two Chunk Seeds. Craft more with one log, four cobblestone, and coal or charcoal.
- The **Chunk Journal** shows your nearby revealed chunks and total progress. Press **J** to open it and **R** to refresh it; the key can be remapped.
- Successful unlocks give a short cue above the hotbar instead of filling chat.
- Revealed chunks and your progress persist with the world. Nether and End generation remain vanilla.

## Compatibility and known limitations

- **Minecraft 1.20.1**, **Forge 47.4.20 or newer**, **Java 17**. Install the mod on both client and server where applicable.
- Use a **new world** for the intended experience. `0.1.0-dev` worlds are not migration targets, and already generated artificial starter chunks in `0.2.0-dev` worlds are not rebuilt.
- Live entities are not copied from the hidden terrain source. A revealed village can contain its buildings and loot without its original villagers.
- Multiplayer gameplay, dedicated servers, structure locating, and full survival progression have not been independently verified for this beta.

Inspired by [Chunk By Chunk](https://github.com/immortius/chunkbychunk) by dividesBy0/immortius. One More Chunk uses independent code and assets and does not include the original mod.
