package dev.nightfallchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NightfallChunks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightfallGameTests {
    private NightfallGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void revealIsLocalAndRetrySafe(GameTestHelper helper) {
        var level = helper.getLevel();
        int chunkZ = 40;
        int chunkX = findFreshChunk(level, 40, chunkZ);
        var source = NightfallWorld.source(level);
        helper.assertTrue(source != null, "Terrain source dimension is missing");
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        var sourceChunk = source.getChunk(chunkX, chunkZ);
        int y = sourceChunk.getHeight(Heightmap.Types.OCEAN_FLOOR, 8, 8);
        BlockPos surface = new BlockPos(x, y, z);
        helper.assertTrue(!sourceChunk.getBlockState(surface).isAir(), "Source has no terrain at sample point");
        helper.assertTrue(level.getBlockState(surface).isAir(), "Test chunk is not empty before reveal");
        level.setBlock(surface, Blocks.WATER.defaultBlockState(), 2);
        NightfallTerrain.reveal(level, chunkX, chunkZ);
        helper.assertTrue(level.getBlockState(surface).equals(source.getBlockState(surface)),
            "Generated terrain did not replace water that entered a locked chunk");
        int quartX = QuartPos.fromBlock(x);
        int quartY = QuartPos.fromBlock(y);
        int quartZ = QuartPos.fromBlock(z);
        helper.assertTrue(level.getChunk(chunkX, chunkZ).getNoiseBiome(quartX, quartY, quartZ)
            .equals(sourceChunk.getNoiseBiome(quartX, quartY, quartZ)),
            "Copied chunk biome does not match the source world");
        helper.assertTrue(level.getChunk(chunkX, chunkZ).getAllStarts().keySet()
            .equals(sourceChunk.getAllStarts().keySet()),
            "Copied chunk structure starts do not match the source world");
        helper.assertTrue(level.getChunk(chunkX, chunkZ).getAllReferences()
            .equals(sourceChunk.getAllReferences()),
            "Copied chunk structure references do not match the source world");
        helper.assertTrue(NightfallTerrain.hasMarker(level, chunkX, chunkZ), "Repair marker missing");
        BlockPos neighbor = new BlockPos((chunkX + 1) * 16 + 8, y, z);
        helper.assertTrue(level.getBlockState(neighbor).isAir(), "Reveal changed a neighboring chunk");

        BlockPos playerChange = surface;
        level.setBlock(playerChange, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        NightfallTerrain.reveal(level, chunkX, chunkZ);
        helper.assertTrue(level.getBlockState(playerChange).is(Blocks.GOLD_BLOCK), "Retry replaced a player block");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void copiesGeneratedContainerContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel source = NightfallWorld.source(level);
        helper.assertTrue(source != null, "Terrain source dimension is missing");
        int chunkZ = 120;
        for (int previousX = 120; previousX < 136; previousX += 2) {
            if (NightfallTerrain.hasMarker(level, previousX, chunkZ)) {
                BlockPos previousChest = new BlockPos(previousX * 16 + 4, 200, chunkZ * 16 + 4);
                helper.assertTrue(level.getBlockEntity(previousChest) instanceof ChestBlockEntity chest
                    && chest.getItem(0).is(Items.EMERALD) && chest.getItem(0).getCount() == 3,
                    "A copied chest lost its contents after save and reload");
            }
        }
        int chunkX = findFreshChunk(level, 120, chunkZ);
        BlockPos chestPos = new BlockPos(chunkX * 16 + 4, 200, chunkZ * 16 + 4);
        source.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        helper.assertTrue(source.getBlockEntity(chestPos) instanceof ChestBlockEntity,
            "Source test chest could not be created");
        ChestBlockEntity sourceChest = (ChestBlockEntity) source.getBlockEntity(chestPos);
        sourceChest.setItem(0, new ItemStack(Items.EMERALD, 3));
        sourceChest.setChanged();

        NightfallTerrain.reveal(level, chunkX, chunkZ);
        helper.assertTrue(level.getBlockEntity(chestPos) instanceof ChestBlockEntity,
            "Generated chest block entity was not copied");
        ChestBlockEntity targetChest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        helper.assertTrue(targetChest.getItem(0).is(Items.EMERALD)
            && targetChest.getItem(0).getCount() == 3,
            "Generated chest contents were not copied");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void starterContainsSuppliesAndPreset(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkUnlockState state = ChunkUnlockState.get(level);
        helper.assertTrue(state.isInitialized(), "Starter chunk is not initialized");
        int chunkX = state.starterX();
        int chunkZ = state.starterZ();
        var source = NightfallWorld.source(level);
        helper.assertTrue(source != null, "Terrain source dimension is missing");
        BlockPos spawn = state.spawn();
        helper.assertTrue(level.getBlockState(spawn.below()).equals(source.getBlockState(spawn.below())),
            "Starter surface does not match natural source terrain");
        helper.assertTrue(level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir(),
            "Starter spawn is obstructed");
        helper.assertTrue(level.getSharedSpawnPos().equals(spawn), "World spawn is not on the natural starter");
        BlockPos deepBlock = null;
        for (int y = level.getMinBuildHeight() + 1; y < 60; y++) {
            BlockPos pos = new BlockPos(spawn.getX(), y, spawn.getZ());
            if (!source.getBlockState(pos).isAir()) {
                deepBlock = pos;
                break;
            }
        }
        helper.assertTrue(deepBlock != null && level.getBlockState(deepBlock).equals(source.getBlockState(deepBlock)),
            "Starter is not a full-height natural chunk");
        ChestBlockEntity chest = null;
        for (var blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
            if (blockEntity instanceof ChestBlockEntity candidate
                && candidate.getItem(4).is(NightfallChunks.CHUNK_SEED.get())) {
                chest = candidate;
                break;
            }
        }
        helper.assertTrue(chest != null, "Starter chest with Chunk Seeds missing");
        helper.assertTrue(chest.getItem(4).is(NightfallChunks.CHUNK_SEED.get()), "Starter Chunk Seeds missing");
        helper.assertTrue(chest.getItem(4).getCount() == 2, "Starter chest must contain two safety seeds");
        var journal = NightfallNetwork.snapshotFor(state, chunkX, chunkZ);
        helper.assertTrue(journal.valid() && journal.total() == state.unlockedCount()
            && journal.isUnlocked(3, 3), "Chunk Journal omitted the current unlocked starter");
        helper.assertTrue(!journal.isUnlocked(-1, 3) && !journal.isUnlocked(7, 3),
            "Chunk Journal accepted coordinates outside its bounded grid");
        helper.assertTrue(level.getServer().getRecipeManager().byKey(
            new ResourceLocation(NightfallChunks.MOD_ID, "chunk_seed")).isPresent(), "Chunk Seed recipe was not loaded");
        var recipe = level.getServer().getRecipeManager().byKey(
            new ResourceLocation(NightfallChunks.MOD_ID, "chunk_seed")).orElseThrow();
        helper.assertTrue(recipe.getIngredients().size() == 6, "Chunk Seed recipe must have six ingredients");
        var fuel = recipe.getIngredients().get(5);
        helper.assertTrue(fuel.test(new ItemStack(Items.COAL)) && fuel.test(new ItemStack(Items.CHARCOAL)),
            "Chunk Seed recipe does not accept both coal and charcoal");
        helper.assertTrue(!fuel.test(new ItemStack(Items.COBBLESTONE)),
            "Chunk Seed recipe allows expansion without fuel");
        helper.assertTrue(level.getServer().getAdvancements().getAdvancement(
            new ResourceLocation(NightfallChunks.MOD_ID, "recipes/chunk_seed")) != null,
            "Chunk Seed recipe-book unlock advancement was not loaded");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .containsKey(new ResourceLocation(NightfallChunks.MOD_ID, "one_chunk")), "World preset was not registered");
        helper.assertTrue(level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(NightfallChunks.MOD_ID, "source"))) != null,
            "Vanilla terrain source dimension was not loaded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void unlockCommitsOnceAndRespectsAdjacency(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkUnlockState state = ChunkUnlockState.get(level);
        helper.assertTrue(state.isInitialized(), "Selected world state was not initialized");
        // On a repeated run of the same saved test world, terrain markers from earlier
        // unlocks must still correspond to restored SavedData entries.
        int starterX = state.starterX();
        int starterZ = state.starterZ();
        for (int offset = -1; offset >= -8; offset--) {
            if (NightfallTerrain.hasMarker(level, starterX + offset, starterZ)) {
                helper.assertTrue(state.isUnlocked(starterX + offset, starterZ),
                    "A revealed chunk was not restored from SavedData after restart");
            }
        }
        int targetX = starterX - 1;
        while (state.isUnlocked(targetX, starterZ) && targetX > starterX - 64) {
            targetX--;
        }
        helper.assertTrue(targetX > starterX - 64, "No free neighbor available for unlock test");
        int sourceX = targetX + 1;
        int before = state.unlockedCount();
        helper.assertTrue(ChunkUnlockService.unlock(level, sourceX, starterZ, targetX - 2, starterZ)
            == ChunkUnlockService.Result.NOT_ADJACENT,
            "A non-adjacent chunk was unlocked");
        helper.assertTrue(ChunkUnlockService.unlock(level, sourceX, starterZ, targetX, starterZ)
            == ChunkUnlockService.Result.UNLOCKED,
            "Adjacent target was not unlocked");
        helper.assertTrue(state.unlockedCount() == before + 1, "Unlock count was not committed exactly once");
        helper.assertTrue(NightfallTerrain.hasMarker(level, targetX, starterZ), "Unlocked target has no terrain marker");
        var journal = NightfallNetwork.snapshotFor(state, sourceX, starterZ);
        helper.assertTrue(journal.total() == state.unlockedCount() && journal.isUnlocked(2, 3),
            "Chunk Journal did not reflect the newly unlocked neighbor");
        // Both sides of the seam must retain the exact source-world Y coordinates.
        ServerLevel source = NightfallWorld.source(level);
        helper.assertTrue(source != null, "Terrain source dimension is missing");
        int seamZ = starterZ * 16 + 8;
        for (int x : new int[]{starterX * 16, targetX * 16 + 15}) {
            for (int y = level.getMinBuildHeight() + 1; y <= 180; y++) {
                BlockPos pos = new BlockPos(x, y, seamZ);
                helper.assertTrue(level.getBlockState(pos).equals(source.getBlockState(pos)),
                    "Starter/revealed seam differs from natural terrain at " + pos);
            }
        }
        helper.assertTrue(ChunkUnlockService.unlock(level, sourceX, starterZ, targetX, starterZ)
            == ChunkUnlockService.Result.ALREADY_UNLOCKED,
            "A duplicate unlock was accepted");
        helper.succeed();
    }

    private static int findFreshChunk(ServerLevel level, int startX, int chunkZ) {
        for (int chunkX = startX; chunkX < startX + 64; chunkX += 2) {
            BlockPos center = new BlockPos(chunkX * 16 + 8, 80, chunkZ * 16 + 8);
            BlockPos neighbor = center.offset(16, 0, 0);
            if (!NightfallTerrain.hasMarker(level, chunkX, chunkZ)
                && level.getBlockState(center).isAir() && level.getBlockState(neighbor).isAir()) {
                return chunkX;
            }
        }
        throw new AssertionError("No empty GameTest chunk available");
    }
}
