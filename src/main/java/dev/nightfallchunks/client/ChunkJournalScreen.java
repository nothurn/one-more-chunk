package dev.nightfallchunks.client;

import dev.nightfallchunks.NightfallNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A compact, read-only snapshot; the server remains authoritative for every unlock. */
final class ChunkJournalScreen extends Screen {
    private static final int GRID_SIZE = 7;
    private static final int BACKGROUND = 0xEE151D1A;
    private static final int BORDER = 0xFF3A5145;
    private static final int OPEN = 0xFF4C9F74;
    private static final int LOCKED = 0xFF303B36;
    private static final int ACCENT = 0xFF80DBAA;
    private static final int TEXT = 0xFFE7F0E9;
    private static final int MUTED = 0xFFB4C4B9;

    private final NightfallNetwork.Snapshot snapshot;

    ChunkJournalScreen(NightfallNetwork.Snapshot snapshot) {
        super(Component.translatable("screen.nightfallchunks.journal.title"));
        this.snapshot = snapshot;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(300, width - 24);
        int panelHeight = Math.min(250, height - 16);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + 2, ACCENT);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, BORDER);
        graphics.drawString(font, title, left + 14, top + 12, TEXT);

        if (!snapshot.valid()) {
            graphics.drawString(font, Component.translatable("screen.nightfallchunks.journal.wrong_world"),
                left + 14, top + 38, MUTED);
        } else {
            graphics.drawString(font, Component.translatable("screen.nightfallchunks.journal.total", snapshot.total()),
                left + 14, top + 30, ACCENT);
            if (panelHeight >= 150) {
                renderGrid(graphics, left, top, panelWidth, panelHeight);
            }
            graphics.drawString(font, Component.translatable("screen.nightfallchunks.journal.position",
                snapshot.currentX(), snapshot.currentZ()), left + 14, top + panelHeight - 34, MUTED);
        }
        graphics.drawString(font, Component.translatable("screen.nightfallchunks.journal.controls"),
            left + 14, top + panelHeight - 17, MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight) {
        boolean showLegend = panelHeight >= 210;
        int reservedHeight = showLegend ? 120 : 96;
        int cell = Math.max(8, Math.min(20, Math.min((panelWidth - 40) / GRID_SIZE,
            (panelHeight - reservedHeight) / GRID_SIZE)));
        int gridWidth = cell * GRID_SIZE;
        int startX = left + (panelWidth - gridWidth) / 2;
        int startY = top + 49;
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int x0 = startX + x * cell;
                int y0 = startY + z * cell;
                graphics.fill(x0, y0, x0 + cell - 2, y0 + cell - 2,
                    snapshot.isUnlocked(x, z) ? OPEN : LOCKED);
                if (x == GRID_SIZE / 2 && z == GRID_SIZE / 2) {
                    int center = cell / 2 - 1;
                    graphics.fill(x0 + center - 1, y0 + center - 1,
                        x0 + center + 2, y0 + center + 2, TEXT);
                }
            }
        }
        if (showLegend) {
            graphics.drawCenteredString(font, Component.translatable("screen.nightfallchunks.journal.legend"),
                left + panelWidth / 2, startY + gridWidth + 6, MUTED);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            NightfallNetwork.requestJournal();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
