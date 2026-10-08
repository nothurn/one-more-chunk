package dev.nightfallchunks;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Only server-derived, bounded status data crosses this channel; clients cannot request chunks by coordinate. */
public final class NightfallNetwork {
    private static final String PROTOCOL = "1";
    private static final int GRID_SIZE = 7;
    private static final int GRID_RADIUS = GRID_SIZE / 2;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NightfallChunks.MOD_ID, "journal"), () -> PROTOCOL,
        PROTOCOL::equals, PROTOCOL::equals);

    private NightfallNetwork() {}

    static void register() {
        CHANNEL.registerMessage(0, Request.class, (message, buffer) -> {},
            buffer -> new Request(), NightfallNetwork::handleRequest);
        CHANNEL.registerMessage(1, Snapshot.class, Snapshot::encode, Snapshot::decode,
            NightfallNetwork::handleSnapshot);
    }

    public static void requestJournal() {
        CHANNEL.sendToServer(new Request());
    }

    private static void handleRequest(Request request, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ChunkPos current = player.chunkPosition();
                Snapshot snapshot = NightfallWorld.isSelected(player.serverLevel())
                    ? snapshotFor(ChunkUnlockState.get(player.serverLevel()), current.x, current.z)
                    : new Snapshot(false, current.x, current.z, 0, 0L);
                CHANNEL.sendTo(snapshot, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleSnapshot(Snapshot snapshot, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> dev.nightfallchunks.client.NightfallClient.showJournal(snapshot)));
        context.setPacketHandled(true);
    }

    static Snapshot snapshotFor(ChunkUnlockState state, int currentX, int currentZ) {
        long mask = 0L;
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (state.isUnlocked(currentX + x - GRID_RADIUS, currentZ + z - GRID_RADIUS)) {
                    mask |= 1L << (z * GRID_SIZE + x);
                }
            }
        }
        return new Snapshot(true, currentX, currentZ, state.unlockedCount(), mask);
    }

    private record Request() {}

    public record Snapshot(boolean valid, int currentX, int currentZ, int total, long mask) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(valid);
            buffer.writeInt(currentX);
            buffer.writeInt(currentZ);
            buffer.writeVarInt(total);
            buffer.writeLong(mask);
        }

        private static Snapshot decode(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readBoolean(), buffer.readInt(), buffer.readInt(),
                buffer.readVarInt(), buffer.readLong());
        }

        public boolean isUnlocked(int gridX, int gridZ) {
            return gridX >= 0 && gridX < GRID_SIZE && gridZ >= 0 && gridZ < GRID_SIZE
                && (mask & (1L << (gridZ * GRID_SIZE + gridX))) != 0L;
        }
    }
}
