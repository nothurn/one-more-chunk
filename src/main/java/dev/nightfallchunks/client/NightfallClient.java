package dev.nightfallchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nightfallchunks.NightfallChunks;
import dev.nightfallchunks.NightfallNetwork;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NightfallChunks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NightfallClient {
    private static final ResourceKey<WorldPreset> ONE_CHUNK_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET, new ResourceLocation(NightfallChunks.MOD_ID, "one_chunk"));
    private static final Set<CreateWorldScreen> OPENED_CREATION_SCREENS =
        Collections.newSetFromMap(new WeakHashMap<>());
    private static final KeyMapping OPEN_JOURNAL = new KeyMapping(
        "key.nightfallchunks.open_journal", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.nightfallchunks");

    private NightfallClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_JOURNAL);
    }

    public static void showJournal(NightfallNetwork.Snapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && (minecraft.screen == null || minecraft.screen instanceof ChunkJournalScreen)) {
            minecraft.setScreen(new ChunkJournalScreen(snapshot));
        }
    }

    @Mod.EventBusSubscriber(modid = NightfallChunks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (!(event.getNewScreen() instanceof CreateWorldScreen screen)
                || !OPENED_CREATION_SCREENS.add(screen)) {
                return;
            }
            WorldCreationUiState state = screen.getUiState();
            // Re-create World carries its original seed; do not change copied world settings.
            if (!state.getSeed().isBlank() || state.getWorldType().preset() == null
                || !state.getWorldType().preset().is(WorldPresets.NORMAL)) {
                return;
            }
            state.getNormalPresetList().stream()
                .filter(entry -> entry.preset() != null && entry.preset().is(ONE_CHUNK_PRESET))
                .findFirst().ifPresent(state::setWorldType);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (OPEN_JOURNAL.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null && minecraft.screen == null) {
                    NightfallNetwork.requestJournal();
                }
            }
        }
    }
}
