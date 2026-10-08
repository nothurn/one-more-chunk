package dev.nightfallchunks;

import java.util.List;
import java.util.HashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Copies terrain at its real world coordinates so seams retain the source world's height. */
final class NightfallTerrain {
    private static final int LEGACY_MARKER_Y = 74;
    private static final int NEARBY_START_RADIUS = 8;
    private static final int MAX_START_RADIUS = 64;

    record StarterSite(int chunkX, int chunkZ, BlockPos spawn) {}

    private NightfallTerrain() {}

    static StarterSite selectStarter(ServerLevel level) {
        ServerLevel source = requireSource(level);
        StarterSite grassyFallback = null;
        StarterSite dryFallback = null;
        for (int radius = 0; radius <= MAX_START_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                        continue;
                    }
                    // An ocean spawn can contain no safe dry column for many chunks.
                    // Beyond the nearby search, query biome noise before generating a chunk.
                    if (radius > NEARBY_START_RADIUS && isWetBiome(source, x, z)) {
                        continue;
                    }
                    LevelChunk chunk = source.getChunk(x, z);
                    BlockPos grass = findSafeSpawn(source, chunk, true);
                    if (grass != null && findChestPosition(source, chunk, grass) != null) {
                        StarterSite site = new StarterSite(x, z, grass);
                        if (hasNaturalLogs(chunk)) {
                            return site;
                        }
                        if (grassyFallback == null) {
                            grassyFallback = site;
                        }
                    } else if (dryFallback == null) {
                        BlockPos dry = findSafeSpawn(source, chunk, false);
                        if (dry != null && findChestPosition(source, chunk, dry) != null) {
                            dryFallback = new StarterSite(x, z, dry);
                        }
                    }
                }
            }
            // Prefer a natural tree without forcing the entire expanded search to generate.
            if (radius >= 3 && grassyFallback != null) {
                return grassyFallback;
            }
            if (radius >= 4 && dryFallback != null) {
                return dryFallback;
            }
        }
        throw new IllegalStateException("Could not find safe natural starter terrain within "
            + MAX_START_RADIUS + " chunks of world origin");
    }

    private static boolean isWetBiome(ServerLevel source, int chunkX, int chunkZ) {
        var chunkSource = source.getChunkSource();
        var biomeSource = chunkSource.getGenerator().getBiomeSource();
        var biome = biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkX * 16 + 8),
            QuartPos.fromBlock(80), QuartPos.fromBlock(chunkZ * 16 + 8),
            chunkSource.randomState().sampler());
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }

    static boolean hasMarker(ServerLevel level, int chunkX, int chunkZ) {
        return level.getBlockState(sourceMarker(chunkX, chunkZ)).is(Blocks.BARRIER)
            || level.getBlockState(sourceMarker(chunkX, chunkZ)).is(Blocks.STRUCTURE_VOID)
            || level.getBlockState(legacyMarker(chunkX, chunkZ)).is(Blocks.BEDROCK);
    }

    static void reveal(ServerLevel level, int chunkX, int chunkZ) {
        copyGeneratedChunk(level, chunkX, chunkZ);
    }

    static void revealStarter(ServerLevel level, StarterSite site) {
        ServerLevel source = requireSource(level);
        LevelChunk sourceChunk = source.getChunk(site.chunkX(), site.chunkZ());
        if (!isSafeSpawn(source, sourceChunk, site.spawn(), false)) {
            throw new IllegalStateException("Starter spawn is no longer safe at " + site.spawn());
        }
        copyGeneratedChunk(level, site.chunkX(), site.chunkZ());
        BlockPos chestPos = findChestPosition(source, sourceChunk, site.spawn());
        if (chestPos == null) {
            throw new IllegalStateException("No safe starter chest location near " + site.spawn());
        }
        placeStarterChest(level, chestPos, !hasNaturalLogs(sourceChunk));
    }

    private static void copyGeneratedChunk(ServerLevel level, int chunkX, int chunkZ) {
        ServerLevel source = requireSource(level);
        LevelChunk sourceChunk = source.getChunk(chunkX, chunkZ);
        LevelChunk targetChunk = level.getChunk(chunkX, chunkZ);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = pos.getMinBlockX() + localX;
                int z = pos.getMinBlockZ() + localZ;
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    blockPos.set(x, y, z);
                    BlockState from = sourceChunk.getBlockState(blockPos);
                    BlockState existing = targetChunk.getBlockState(blockPos);
                    // Fluids can cross from an unlocked chunk into a locked one before reveal.
                    // Preserve actual player blocks, but restore the source terrain through fluids.
                    if ((!existing.isAir() && existing.getFluidState().isEmpty())
                        || existing.equals(from) || (from.isAir() && existing.isAir())) {
                        continue;
                    }
                    if (!level.setBlock(blockPos, from, 2)) {
                        throw new IllegalStateException("Could not reveal generated block at " + blockPos);
                    }
                    if (from.hasBlockEntity()) {
                        var sourceEntity = sourceChunk.getBlockEntity(blockPos);
                        var targetEntity = targetChunk.getBlockEntity(blockPos);
                        if (sourceEntity != null && targetEntity != null) {
                            targetEntity.load(sourceEntity.saveWithFullMetadata());
                            targetEntity.setChanged();
                        }
                    }
                }
            }
        }

        targetChunk.fillBiomesFromNoise((x, y, z, sampler) -> sourceChunk.getNoiseBiome(x, y, z),
            Climate.empty());
        targetChunk.setAllStarts(new HashMap<>(sourceChunk.getAllStarts()));
        var references = new HashMap<Structure, LongSet>();
        sourceChunk.getAllReferences().forEach((structure, positions) ->
            references.put(structure, new LongOpenHashSet(positions)));
        targetChunk.setAllReferences(references);
        targetChunk.setUnsaved(true);
        if (!level.getBlockState(sourceMarker(chunkX, chunkZ)).is(Blocks.BARRIER)
            && !level.setBlock(sourceMarker(chunkX, chunkZ), Blocks.BARRIER.defaultBlockState(), 2)) {
            throw new IllegalStateException("Could not mark generated chunk " + chunkX + "," + chunkZ);
        }
        var biomes = ClientboundChunksBiomesPacket.forChunks(List.of(targetChunk));
        level.players().forEach(player -> player.connection.send(biomes));
    }

    private static ServerLevel requireSource(ServerLevel level) {
        ServerLevel source = NightfallWorld.source(level);
        if (source == null) {
            throw new IllegalStateException("Nightfall terrain source dimension is missing");
        }
        return source;
    }

    private static BlockPos legacyMarker(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, LEGACY_MARKER_Y, chunkZ * 16 + 8);
    }

    private static BlockPos sourceMarker(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16 + 8, -64, chunkZ * 16 + 8);
    }

    private static BlockPos findSafeSpawn(ServerLevel source, LevelChunk chunk, boolean grassOnly) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int localX = 2; localX <= 13; localX++) {
            for (int localZ = 2; localZ <= 13; localZ++) {
                int groundY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
                BlockPos candidate = new BlockPos(chunk.getPos().getMinBlockX() + localX, groundY + 1,
                    chunk.getPos().getMinBlockZ() + localZ);
                if (!isSafeSpawn(source, chunk, candidate, grassOnly)) {
                    continue;
                }
                int distance = Math.abs(localX - 8) + Math.abs(localZ - 8);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean isSafeSpawn(ServerLevel source, LevelChunk chunk, BlockPos spawn, boolean grassOnly) {
        if (spawn.getY() < 64 || spawn.getY() > 160
            || !new ChunkPos(spawn).equals(chunk.getPos())) {
            return false;
        }
        BlockPos groundPos = spawn.below();
        BlockState ground = chunk.getBlockState(groundPos);
        return (!grassOnly || ground.is(Blocks.GRASS_BLOCK))
            && ground.getFluidState().isEmpty()
            && ground.isFaceSturdy(source, groundPos, Direction.UP)
            && chunk.getBlockState(spawn).isAir()
            && chunk.getBlockState(spawn.above()).isAir();
    }

    private static boolean hasNaturalLogs(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                int x = pos.getMinBlockX() + localX;
                int z = pos.getMinBlockZ() + localZ;
                for (int y = Math.max(64, top - 12); y <= top; y++) {
                    if (chunk.getBlockState(new BlockPos(x, y, z)).is(BlockTags.LOGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BlockPos findChestPosition(ServerLevel source, LevelChunk chunk, BlockPos spawn) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int localX = 2; localX <= 13; localX++) {
            for (int localZ = 2; localZ <= 13; localZ++) {
                int groundY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
                BlockPos candidate = new BlockPos(chunk.getPos().getMinBlockX() + localX, groundY + 1,
                    chunk.getPos().getMinBlockZ() + localZ);
                int horizontal = Math.abs(candidate.getX() - spawn.getX())
                    + Math.abs(candidate.getZ() - spawn.getZ());
                if (horizontal < 2 || horizontal > 8 || !isSafeSpawn(source, chunk, candidate, false)) {
                    continue;
                }
                int score = horizontal + 4 * Math.abs(candidate.getY() - spawn.getY());
                if (score < bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private static void placeStarterChest(ServerLevel level, BlockPos pos, boolean needsWood) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.OAK_SAPLING, 4));
            chest.setItem(1, new ItemStack(Items.BREAD, 8));
            chest.setItem(2, new ItemStack(Items.WATER_BUCKET));
            chest.setItem(3, new ItemStack(Items.LAVA_BUCKET));
            chest.setItem(4, new ItemStack(NightfallChunks.CHUNK_SEED.get(), 2));
            if (needsWood) {
                chest.setItem(5, new ItemStack(Items.OAK_LOG, 4));
            }
            chest.setChanged();
        }
    }
}
