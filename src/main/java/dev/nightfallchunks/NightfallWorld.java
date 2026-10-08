package dev.nightfallchunks;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;

/** Recognizes only the purpose-built flat-air preset, never an ordinary overworld. */
final class NightfallWorld {
    private static final ResourceKey<Biome> ISLAND_BIOME = ResourceKey.create(Registries.BIOME,
        new ResourceLocation(NightfallChunks.MOD_ID, "island"));
    private static final ResourceKey<Level> SOURCE_DIMENSION = ResourceKey.create(Registries.DIMENSION,
        new ResourceLocation(NightfallChunks.MOD_ID, "source"));

    private NightfallWorld() {}

    static boolean isSelected(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)
            || source(level) == null
            || !(level.getChunkSource().getGenerator() instanceof FlatLevelSource flat)) {
            return false;
        }
        var layers = flat.settings().getLayersInfo();
        if (layers.size() != 1) {
            return false;
        }
        FlatLayerInfo layer = layers.get(0);
        return layer.getHeight() == 1
            && layer.getBlockState().isAir()
            && flat.settings().getBiome().is(ISLAND_BIOME);
    }

    static ServerLevel source(ServerLevel level) {
        return level.getServer().getLevel(SOURCE_DIMENSION);
    }
}
