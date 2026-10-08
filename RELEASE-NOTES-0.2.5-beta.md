# One More Chunk 0.2.5-beta

Start on one naturally generated, full-height Overworld chunk surrounded by void. Craft and use Chunk Seeds to reveal neighboring chunks one at a time. The terrain is taken from a hidden source world at the same coordinates, so neighboring chunks retain their natural heights and connect without an artificial starter platform.

## Features

- The **One Chunk** world preset is preselected for fresh worlds; Normal remains available.
- A starter chest provides initial supplies and two Chunk Seeds. Further seeds use a log, four cobblestone, and coal or charcoal.
- Press **J** for the Chunk Journal. It shows nearby revealed chunks and the total; **R** refreshes it.
- Unlock confirmations appear briefly above the hotbar instead of filling chat.
- Nether and End use their normal world generators.

## Beta notes

- Forge 1.20.1, minimum Forge 47.4.20; Java 17.
- Live entities are not copied from the hidden terrain source. Revealed villages can therefore contain buildings and loot but lack their original villagers.
- Multiplayer, dedicated-server use, structure locating, and full survival progression have not been independently verified for this candidate.
- Development worlds created with `0.1.0-dev` are not migration targets. Already generated artificial starter chunks from `0.2.0-dev` are not rebuilt. For the intended experience, create a new world.

Inspired by [Chunk By Chunk](https://github.com/immortius/chunkbychunk) by dividesBy0/immortius. One More Chunk uses its own code, assets, handheld seed interaction, and journal; it does not include the original mod.
