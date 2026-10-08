package dev.nightfallchunks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkUnlockPlanTest {
    @Test
    void startsAtTheChosenNaturalChunk() {
        ChunkUnlockPlan plan = new ChunkUnlockPlan();
        plan.initialize(4, -3);
        assertEquals(1, plan.count());
        assertTrue(plan.isUnlocked(4, -3));
        assertFalse(plan.isUnlocked(0, 0));
    }

    @Test
    void acceptsOnlyCardinallyAdjacentChunks() {
        ChunkUnlockPlan plan = new ChunkUnlockPlan();
        plan.initialize(0, 0);
        assertFalse(plan.unlock(1, 1));
        assertFalse(plan.unlock(4, 0));
        assertTrue(plan.unlock(1, 0));
        assertTrue(plan.unlock(1, 1));
        assertFalse(plan.unlock(1, 0));
        assertEquals(3, plan.count());
    }

    @Test
    void handlesNegativeCoordinatesWithoutCollision() {
        ChunkUnlockPlan plan = new ChunkUnlockPlan();
        plan.initialize(0, 0);
        assertTrue(plan.unlock(-1, 0));
        assertTrue(plan.unlock(-1, -1));
        assertFalse(plan.isUnlocked(1, -1));
        for (int x : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            for (int z : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                long packed = ChunkUnlockPlan.pack(x, z);
                assertEquals(x, ChunkUnlockPlan.unpackX(packed));
                assertEquals(z, ChunkUnlockPlan.unpackZ(packed));
            }
        }
    }

    @Test
    void restoresExactlyTheSavedChunks() {
        ChunkUnlockPlan plan = ChunkUnlockPlan.restore(new long[]{ChunkUnlockPlan.pack(4, -3), ChunkUnlockPlan.pack(-1, 0)});
        assertTrue(plan.isUnlocked(4, -3));
        assertTrue(plan.isUnlocked(-1, 0));
        assertFalse(plan.isUnlocked(0, 0));
        assertEquals(2, plan.count());
    }

    @Test
    void doesNotTreatIntegerOverflowAsAdjacency() {
        ChunkUnlockPlan east = ChunkUnlockPlan.restore(new long[]{ChunkUnlockPlan.pack(Integer.MAX_VALUE, 7)});
        ChunkUnlockPlan south = ChunkUnlockPlan.restore(new long[]{ChunkUnlockPlan.pack(7, Integer.MAX_VALUE)});
        assertFalse(east.canUnlock(Integer.MIN_VALUE, 7));
        assertFalse(south.canUnlock(7, Integer.MIN_VALUE));
    }
}
