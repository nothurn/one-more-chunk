package dev.nightfallchunks;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/** The only gameplay path that commits a newly revealed chunk. Server thread only. */
final class ChunkUnlockService {
    private static final Logger LOGGER = LogUtils.getLogger();

    enum Result {
        UNLOCKED,
        WRONG_WORLD,
        NOT_ADJACENT,
        OUTSIDE_BORDER,
        SOURCE_LOCKED,
        ALREADY_UNLOCKED,
        GENERATION_FAILED
    }

    private ChunkUnlockService() {}

    static Result unlock(ServerLevel level, int sourceX, int sourceZ, int targetX, int targetZ) {
        if (!NightfallWorld.isSelected(level)) {
            return Result.WRONG_WORLD;
        }
        if (Math.abs((long) targetX - sourceX) + Math.abs((long) targetZ - sourceZ) != 1L) {
            return Result.NOT_ADJACENT;
        }
        long blockX = (long) targetX * 16L + 8L;
        long blockZ = (long) targetZ * 16L + 8L;
        if (blockX < Integer.MIN_VALUE || blockX > Integer.MAX_VALUE
            || blockZ < Integer.MIN_VALUE || blockZ > Integer.MAX_VALUE
            || !level.getWorldBorder().isWithinBounds(new BlockPos((int) blockX, 0, (int) blockZ))) {
            return Result.OUTSIDE_BORDER;
        }
        ChunkUnlockState state = ChunkUnlockState.get(level);
        if (!state.isInitialized() || !state.isUnlocked(sourceX, sourceZ)) {
            return Result.SOURCE_LOCKED;
        }
        if (!state.canUnlock(targetX, targetZ)) {
            return Result.ALREADY_UNLOCKED;
        }

        // A crash before the SavedData commit leaves the terrain marker, and a retry safely commits it.
        try {
            if (!NightfallTerrain.hasMarker(level, targetX, targetZ)) {
                NightfallTerrain.reveal(level, targetX, targetZ);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Could not reveal chunk {},{}; the Chunk Seed was not consumed", targetX, targetZ, exception);
            return Result.GENERATION_FAILED;
        }
        return state.commitUnlock(targetX, targetZ) ? Result.UNLOCKED : Result.ALREADY_UNLOCKED;
    }
}
