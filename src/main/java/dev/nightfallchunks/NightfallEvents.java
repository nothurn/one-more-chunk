package dev.nightfallchunks;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

final class NightfallEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FIRST_JOIN_TAG = "nightfallchunks_first_join_done";
    private static final int MAX_REPAIRS_PER_TICK = 2;
    private final Set<Long> scheduledRepairs = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Long> repairQueue = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        if (!NightfallWorld.isSelected(level)) {
            return;
        }
        ChunkUnlockState state = ChunkUnlockState.get(level);
        if (!state.isInitialized()) {
            NightfallTerrain.StarterSite site = NightfallTerrain.selectStarter(level);
            NightfallTerrain.revealStarter(level, site);
            state.markInitialized(site.chunkX(), site.chunkZ(), site.spawn());
            LOGGER.info("Nightfall One Chunk natural starter at chunk {},{} and spawn {}",
                site.chunkX(), site.chunkZ(), site.spawn());
        } else if (!NightfallTerrain.hasMarker(level, state.starterX(), state.starterZ())) {
            NightfallTerrain.revealStarter(level,
                new NightfallTerrain.StarterSite(state.starterX(), state.starterZ(), state.spawn()));
        }
        level.setDefaultSpawnPos(state.spawn(), 0.0F);
    }

    @SubscribeEvent
    public void onPlayerJoined(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!NightfallWorld.isSelected(level) || !ChunkUnlockState.get(level).isInitialized()) {
            return;
        }
        if (!player.getPersistentData().getBoolean(FIRST_JOIN_TAG)) {
            BlockPos spawn = ChunkUnlockState.get(level).spawn();
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                Set.of(), 0.0F, 0.0F);
            player.getPersistentData().putBoolean(FIRST_JOIN_TAG, true);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 65, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.nightfallchunks.welcome.title").withStyle(ChatFormatting.GREEN)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("message.nightfallchunks.welcome.subtitle").withStyle(ChatFormatting.GOLD)));
            player.sendSystemMessage(Component.translatable("message.nightfallchunks.welcome")
                .withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.translatable("message.nightfallchunks.welcome.hint")
                .withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public void onPlayerClone(Clone event) {
        // Entity persistent data is not automatically retained when a player dies.
        // Otherwise the next login would unexpectedly return an established player to spawn.
        if (event.getOriginal().getPersistentData().getBoolean(FIRST_JOIN_TAG)) {
            event.getEntity().getPersistentData().putBoolean(FIRST_JOIN_TAG, true);
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !NightfallWorld.isSelected(level)) {
            return;
        }
        ChunkUnlockState state = ChunkUnlockState.get(level);
        if (state.isInitialized()) {
            ChunkPos pos = new ChunkPos(event.getPos());
            if (!state.isUnlocked(pos.x, pos.z)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
            || !NightfallWorld.isSelected(level)) {
            return;
        }
        ChunkPos pos = event.getChunk().getPos();
        long key = ChunkUnlockPlan.pack(pos.x, pos.z);
        if (scheduledRepairs.add(key)) {
            repairQueue.add(key);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
            || !level.dimension().equals(Level.OVERWORLD) || !NightfallWorld.isSelected(level)) {
            return;
        }
        ChunkUnlockState state = ChunkUnlockState.get(level);
        for (int i = 0; i < MAX_REPAIRS_PER_TICK; i++) {
            Long key = repairQueue.poll();
            if (key == null) {
                return;
            }
            scheduledRepairs.remove(key);
            int chunkX = ChunkUnlockPlan.unpackX(key);
            int chunkZ = ChunkUnlockPlan.unpackZ(key);
            if (state.isInitialized() && state.isUnlocked(chunkX, chunkZ)
                && level.getChunkSource().getChunkNow(chunkX, chunkZ) != null
                && !NightfallTerrain.hasMarker(level, chunkX, chunkZ)) {
                if (chunkX == state.starterX() && chunkZ == state.starterZ()) {
                    NightfallTerrain.revealStarter(level,
                        new NightfallTerrain.StarterSite(chunkX, chunkZ, state.spawn()));
                } else {
                    NightfallTerrain.reveal(level, chunkX, chunkZ);
                }
            }
        }
    }
}
