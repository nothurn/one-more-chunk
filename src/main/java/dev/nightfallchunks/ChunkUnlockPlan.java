package dev.nightfallchunks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** World-independent rules for revealing adjacent chunks. No world generation is performed here. */
public final class ChunkUnlockPlan {
    private final Set<Long> unlocked = new HashSet<>();

    public ChunkUnlockPlan() {
    }

    public void initialize(int x, int z) {
        if (!unlocked.isEmpty()) {
            throw new IllegalStateException("Chunk unlock plan is already initialized");
        }
        unlocked.add(pack(x, z));
    }

    public boolean isUnlocked(int x, int z) {
        return unlocked.contains(pack(x, z));
    }

    public boolean canUnlock(int x, int z) {
        if (isUnlocked(x, z)) {
            return false;
        }
        return (x > Integer.MIN_VALUE && isUnlocked(x - 1, z))
            || (x < Integer.MAX_VALUE && isUnlocked(x + 1, z))
            || (z > Integer.MIN_VALUE && isUnlocked(x, z - 1))
            || (z < Integer.MAX_VALUE && isUnlocked(x, z + 1));
    }

    public boolean unlock(int x, int z) {
        return canUnlock(x, z) && unlocked.add(pack(x, z));
    }

    public int count() {
        return unlocked.size();
    }

    public Set<Long> packedKeys() {
        return Collections.unmodifiableSet(unlocked);
    }

    static ChunkUnlockPlan restore(long[] keys) {
        ChunkUnlockPlan plan = new ChunkUnlockPlan();
        for (long key : keys) {
            plan.unlocked.add(key);
        }
        return plan;
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    public static int unpackX(long key) {
        return (int) (key >> 32);
    }

    public static int unpackZ(long key) {
        return (int) key;
    }
}
