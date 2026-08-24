package dev.localcapes.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CapeSelectScreen extends Screen {
    private static final int PANEL_W = 352;
    private static final int PANEL_H = 248;
    private static final int COLUMNS = 12;
    private static final int SLOT_W = 22;
    private static final int SLOT_H = 34;
    private static final int PREVIEW_W = 20;
    private static final int PREVIEW_H = 32;
    private static final int TOOLTIP_WIDTH = 240;
    private static final int DELETE_SIZE = 9;
    private static final int VISIBLE_ROWS = 4;

    @Nullable
    private final Screen parent;
    private List<CapeEntry> visible = List.of();
    private int scroll;
    @Nullable
    private Button uploadButton;

    public CapeSelectScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.localcapes.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - PANEL_W / 2;
        int y = this.height / 2 - PANEL_H / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.filter.minecraft"), button -> {
            CapeManager.setGameFilter(CapeEntry.GAME_MINECRAFT);
            this.scroll = 0;
            this.refreshVisible();
        }).bounds(x + 8, y + 22, 100, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.filter.dungeons"), button -> {
            CapeManager.setGameFilter(CapeEntry.GAME_DUNGEONS);
            this.scroll = 0;
            this.refreshVisible();
        }).bounds(x + 112, y + 22, 140, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.none"), button -> CapeManager.unequip())
                .bounds(x + PANEL_W - 92, y + 22, 84, 18)
                .build());
        this.uploadButton = Button.builder(Component.translatable("gui.localcapes.upload"), button -> this.openUpload())
                .bounds(x + 8, y + PANEL_H - 22, 84, 18)
                .build();
        this.addRenderableWidget(this.uploadButton);
        this.addRenderableWidget(Button.builder(glowLabel(), button -> {
            CapeManager.toggleNightGlow();
            button.setMessage(glowLabel());
        }).bounds(x + 96, y + PANEL_H - 22, 120, 18).build());
        this.refreshVisible();
    }

    private static Component glowLabel() {
        return Component.translatable(
                CapeManager.isNightGlow() ? "gui.localcapes.glow.on" : "gui.localcapes.glow.off"
        );
    }

    private void refreshVisible() {
        this.visible = CapeManager.entriesForGame(CapeManager.getGameFilter());
        this.clampScroll();
        if (this.uploadButton != null) {
            this.uploadButton.visible = CapeEntry.GAME_MINECRAFT.equals(CapeManager.getGameFilter());
        }
    }

    private int maxScroll() {
        int rows = (this.visible.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    private void clampScroll() {
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
    }

    private void openUpload() {
        if (this.minecraft == null) {
            return;
        }
        this.uploadButton.active = false;
        Thread picker = new Thread(() -> {
            CapeUpload.Result picked = CapeUpload.pickPng();
            this.minecraft.execute(() -> {
                if (this.uploadButton != null) {
                    this.uploadButton.active = true;
                }
                if (picked.status() == CapeUpload.Status.CANCELLED) {
                    return;
                }
                if (picked.status() == CapeUpload.Status.FAILED || picked.path() == null) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.translatable("gui.localcapes.upload.dialog_fail"),
                                false
                        );
                    }
                    return;
                }
                CapeEntry imported = CapeManager.importCape(picked.path());
                if (imported == null) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.translatable("gui.localcapes.upload.fail"),
                                false
                        );
                    }
                    return;
                }
                CapeManager.wear(imported);
                CapeManager.setGameFilter(CapeEntry.GAME_MINECRAFT);
                this.scroll = 0;
                this.refreshVisible();
                if (this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.translatable("gui.localcapes.upload.ok"),
                            false
                    );
                }
            });
        }, "LocalCapes-Upload");
        picker.setDaemon(false);
        picker.start();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - PANEL_W / 2;
        int top = this.height / 2 - PANEL_H / 2;
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);

        int gridTop = top + 46;
        int gridLeft = left + 16;
        int gridBottom = gridTop + VISIBLE_ROWS * (SLOT_H + 4);
        CapeEntry hovered = null;
        if (this.visible.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.localcapes.empty." + CapeManager.getGameFilter()),
                    this.width / 2,
                    gridTop + 40,
                    0xAAAAAA
            );
        }
        for (int i = 0; i < this.visible.size(); i++) {
            int row = i / COLUMNS;
            if (row < this.scroll || row >= this.scroll + VISIBLE_ROWS) {
                continue;
            }
            CapeEntry entry = this.visible.get(i);
            int col = i % COLUMNS;
            int sx = gridLeft + col * (SLOT_W + 4);
            int sy = gridTop + (row - this.scroll) * (SLOT_H + 4);
            boolean over = mouseX >= sx && mouseX < sx + SLOT_W && mouseY >= sy && mouseY < sy + SLOT_H;
            int bg = CapeManager.isSelected(entry) ? 0xFFFFFF55 : over ? 0x80FFFFFF : 0x80000000;
            graphics.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, bg);
            graphics.blit(entry.texture, sx + 1, sy + 1, PREVIEW_W, PREVIEW_H, 1.0F, 1.0F, 10, 16, 64, 32);
            if (over && entry.localFile) {
                int dx = sx + SLOT_W - DELETE_SIZE;
                int dy = sy;
                boolean deleteOver = isDeleteHit(mouseX, mouseY, sx, sy);
                graphics.fill(dx, dy, dx + DELETE_SIZE, dy + DELETE_SIZE, deleteOver ? 0xCCFF3333 : 0xAA550000);
                graphics.drawString(this.font, "×", dx + 2, dy, 0xFFFFFFFF);
            }
            if (over) {
                hovered = entry;
            }
        }
        if (this.maxScroll() > 0) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal((this.scroll + 1) + "/" + (this.maxScroll() + 1)).withStyle(ChatFormatting.GRAY),
                    this.width / 2,
                    gridBottom + 2,
                    0xAAAAAA
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            List<FormattedCharSequence> lines = tooltipLines(hovered);
            if (!lines.isEmpty()) {
                graphics.renderTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = this.width / 2 - PANEL_W / 2;
        int top = this.height / 2 - PANEL_H / 2;
        int gridTop = top + 46;
        int gridLeft = left + 16;
        for (int i = 0; i < this.visible.size(); i++) {
            int row = i / COLUMNS;
            if (row < this.scroll || row >= this.scroll + VISIBLE_ROWS) {
                continue;
            }
            int col = i % COLUMNS;
            int sx = gridLeft + col * (SLOT_W + 4);
            int sy = gridTop + (row - this.scroll) * (SLOT_H + 4);
            if (mouseX >= sx && mouseX < sx + SLOT_W && mouseY >= sy && mouseY < sy + SLOT_H) {
                CapeEntry entry = this.visible.get(i);
                if (button == 0) {
                    if (entry.localFile && isDeleteHit(mouseX, mouseY, sx, sy)) {
                        if (CapeManager.deleteLocal(entry)) {
                            this.refreshVisible();
                            if (this.minecraft.player != null) {
                                this.minecraft.player.displayClientMessage(
                                        Component.translatable("gui.localcapes.delete.ok"),
                                        false
                                );
                            }
                        } else if (this.minecraft.player != null) {
                            this.minecraft.player.displayClientMessage(
                                    Component.translatable("gui.localcapes.delete.fail"),
                                    false
                            );
                        }
                    } else {
                        CapeManager.wear(entry);
                    }
                    return true;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            this.scroll--;
        } else if (delta < 0) {
            this.scroll++;
        }
        this.clampScroll();
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<FormattedCharSequence> tooltipLines(CapeEntry entry) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String descKey = "cape.localcapes." + entry.category + "." + entry.id + ".desc";
        String desc = Component.translatableWithFallback(descKey, "").getString();
        if (desc.isBlank() && entry.localFile) {
            desc = Component.translatable("cape.localcapes.local.file").getString();
        }
        if (!desc.isBlank()) {
            for (String paragraph : desc.split("\n", -1)) {
                if (paragraph.isBlank()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    continue;
                }
                lines.addAll(this.font.split(Component.literal(paragraph).withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
            }
        }
        if (entry.localFile) {
            lines.add(Component.translatable("gui.localcapes.delete.hint").withStyle(ChatFormatting.RED).getVisualOrderText());
        }
        return lines;
    }

    private static boolean isDeleteHit(double mouseX, double mouseY, int sx, int sy) {
        int dx = sx + SLOT_W - DELETE_SIZE;
        int dy = sy;
        return mouseX >= dx && mouseX < dx + DELETE_SIZE && mouseY >= dy && mouseY < dy + DELETE_SIZE;
    }
}
