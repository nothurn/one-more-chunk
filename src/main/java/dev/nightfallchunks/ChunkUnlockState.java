package dev.nightfallchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world unlock state; the terrain marker makes interrupted reveals repairable. */
public final class ChunkUnlockState extends SavedData {
    private static final String DATA_NAME = "nightfallchunks_unlocked";
    private ChunkUnlockPlan plan = new ChunkUnlockPlan();
    private boolean initialized;
    private int starterX;
    private int starterZ;
    private BlockPos spawn = new BlockPos(8, 81, 8);

    public static ChunkUnlockState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ChunkUnlockState::load, ChunkUnlockState::new, DATA_NAME);
    }

    private static ChunkUnlockState load(CompoundTag tag) {
        ChunkUnlockState state = new ChunkUnlockState();
        state.plan = ChunkUnlockPlan.restore(tag.getLongArray("Unlocked"));
        state.initialized = tag.getBoolean("Initialized");
        state.starterX = tag.getInt("StarterX");
        state.starterZ = tag.getInt("StarterZ");
        if (tag.contains("SpawnX") && tag.contains("SpawnY") && tag.contains("SpawnZ")) {
            state.spawn = new BlockPos(tag.getInt("SpawnX"), tag.getInt("SpawnY"), tag.getInt("SpawnZ"));
        }
        return state;
    }

    public boolean isInitialized() {
        return initialized;
    }

    void markInitialized(int x, int z, BlockPos spawn) {
        if (initialized) {
            throw new IllegalStateException("Starter chunk is already initialized");
        }
        plan.initialize(x, z);
        starterX = x;
        starterZ = z;
        this.spawn = spawn;
        initialized = true;
        setDirty();
    }

    int starterX() {
        return starterX;
    }

    int starterZ() {
        return starterZ;
    }

    BlockPos spawn() {
        return spawn;
    }

    public boolean isUnlocked(int x, int z) {
        return plan.isUnlocked(x, z);
    }

    public boolean canUnlock(int x, int z) {
        return plan.canUnlock(x, z);
    }

    public int unlockedCount() {
        return plan.count();
    }

    /** Called only after the target terrain was placed on the server thread. */
    boolean commitUnlock(int x, int z) {
        if (!plan.unlock(x, z)) {
            return false;
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Initialized", initialized);
        tag.putInt("StarterX", starterX);
        tag.putInt("StarterZ", starterZ);
        tag.putInt("SpawnX", spawn.getX());
        tag.putInt("SpawnY", spawn.getY());
        tag.putInt("SpawnZ", spawn.getZ());
        tag.putLongArray("Unlocked", plan.packedKeys().stream().mapToLong(Long::longValue).sorted().toArray());
        return tag;
    }
}
