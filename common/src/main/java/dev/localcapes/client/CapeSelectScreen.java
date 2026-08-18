package dev.localcapes.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import java.util.List;

public final class CapeSelectScreen extends Screen {
    private static final int COLUMNS = 8;
    private static final int SLOT_W = 22;
    private static final int SLOT_H = 34;
    private static final int PREVIEW_W = 20;
    private static final int PREVIEW_H = 32;

    @Nullable
    private final Screen parent;
    private String category = CapeManager.CATEGORY_ORDER.get(0);
    private List<CapeEntry> visible = List.of();

    public CapeSelectScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.localcapes.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 176;
        int y = this.height / 2 - 110;
        int bx = x + 8;
        int by = y + 22;
        for (String id : CapeManager.CATEGORY_ORDER) {
            if (CapeManager.entriesIn(id).isEmpty() && !"local".equals(id)) {
                continue;
            }
            String categoryId = id;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.category." + id), button -> {
                this.category = categoryId;
                this.visible = CapeManager.entriesIn(this.category);
            }).bounds(bx, by, 84, 18).build());
            bx += 86;
            if (bx > x + 8 + 86 * 3) {
                bx = x + 8;
                by += 20;
            }
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.none"), button -> CapeManager.unequip())
                .bounds(x + 8 + 86 * 3, by, 84, 18)
                .build());
        this.visible = CapeManager.entriesIn(this.category);
        if (this.visible.isEmpty()) {
            for (String id : CapeManager.CATEGORY_ORDER) {
                List<CapeEntry> entries = CapeManager.entriesIn(id);
                if (!entries.isEmpty()) {
                    this.category = id;
                    this.visible = entries;
                    break;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 176;
        int top = this.height / 2 - 110;
        graphics.fill(left, top, left + 352, top + 220, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);

        int gridTop = top + 66;
        int gridLeft = left + 16;
        CapeEntry hovered = null;
        for (int i = 0; i < this.visible.size(); i++) {
            CapeEntry entry = this.visible.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int sx = gridLeft + col * (SLOT_W + 4);
            int sy = gridTop + row * (SLOT_H + 4);
            boolean over = mouseX >= sx && mouseX < sx + SLOT_W && mouseY >= sy && mouseY < sy + SLOT_H;
            int bg = CapeManager.isSelected(entry) ? 0xFFFFFF55 : over ? 0x80FFFFFF : 0x80000000;
            graphics.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, bg);
            graphics.blit(entry.texture, sx + 1, sy + 1, PREVIEW_W, PREVIEW_H, 1.0F, 1.0F, 10, 16, 64, 32);
            if (over) {
                hovered = entry;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            java.util.List<Component> tooltip = new ArrayList<>();
            String nameKey = "cape.localcapes." + hovered.category + "." + hovered.id + ".name";
            String descKey = "cape.localcapes." + hovered.category + "." + hovered.id + ".desc";
            Component nameComp = Component.translatableWithFallback(nameKey, hovered.displayName);
            Component descComp = Component.translatableWithFallback(descKey, "").withStyle(ChatFormatting.GRAY);
            tooltip.add(nameComp);
            String descStr = descComp.getString();
            if (!descStr.isBlank()) {
                tooltip.add(descComp);
            }
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = this.width / 2 - 176;
        int top = this.height / 2 - 110;
        int gridTop = top + 66;
        int gridLeft = left + 16;
        for (int i = 0; i < this.visible.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int sx = gridLeft + col * (SLOT_W + 4);
            int sy = gridTop + row * (SLOT_H + 4);
            if (mouseX >= sx && mouseX < sx + SLOT_W && mouseY >= sy && mouseY < sy + SLOT_H) {
                CapeManager.wear(this.visible.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
}
