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
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_H = SLOT_H + 4;
    private static final int HEADER_H = 12;
    private static final int GRID_H = VISIBLE_ROWS * ROW_H;
    private static final int GRID_OFFSET = 24;
    private static final int TITLE_Y = 8;
    private static final int ARROW_W = 7;
    private static final int ARROW_GAP = 4;
    private static final int OPTION_H = 14;
    private static final int OPTION_PAD = 6;

    private record Row(@Nullable String header, List<CapeEntry> entries) {
        int height() {
            return this.header != null ? HEADER_H : ROW_H;
        }
    }

    private record Slot(CapeEntry entry, int x, int y) {
    }

    @Nullable
    private final Screen parent;
    private List<CapeEntry> visible = List.of();
    private List<Row> rows = List.of();
    private int scroll;
    private boolean filterOpen;
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
        this.filterOpen = false;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.none"), button -> CapeManager.unequip())
                .bounds(x + PANEL_W - 92, y + PANEL_H - 22, 84, 18)
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

    private static Component filterName(String game) {
        return Component.translatable("gui.localcapes.filter." + game);
    }

    private Component titleText() {
        return this.title.copy().append(" · ").append(filterName(CapeManager.getGameFilter()));
    }

    private int titleWidth() {
        return this.font.width(this.titleText()) + ARROW_GAP + ARROW_W;
    }

    private int titleLeft() {
        return this.width / 2 - this.titleWidth() / 2;
    }

    private int titleTop() {
        return this.height / 2 - PANEL_H / 2 + TITLE_Y;
    }

    private boolean isTitleHit(double mouseX, double mouseY) {
        int x = this.titleLeft();
        int y = this.titleTop();
        return mouseX >= x - 2 && mouseX < x + this.titleWidth() + 2 && mouseY >= y - 2 && mouseY < y + 11;
    }

    private int optionWidth() {
        int width = 0;
        for (String game : CapeEntry.GAMES) {
            width = Math.max(width, this.font.width(filterName(game)));
        }
        return width + OPTION_PAD * 2;
    }

    private int optionLeft() {
        return this.width / 2 - this.optionWidth() / 2;
    }

    private int optionTop() {
        return this.titleTop() + 12;
    }

    private int optionAt(double mouseX, double mouseY) {
        int x = this.optionLeft();
        int y = this.optionTop();
        if (mouseX < x || mouseX >= x + this.optionWidth() || mouseY < y) {
            return -1;
        }
        int index = (int) ((mouseY - y) / OPTION_H);
        return index < CapeEntry.GAMES.size() ? index : -1;
    }

    private void renderTitle(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.titleLeft();
        int y = this.titleTop();
        int color = this.filterOpen || this.isTitleHit(mouseX, mouseY) ? 0xFFFFFF55 : 0xFFFFFFFF;
        graphics.drawString(this.font, this.titleText(), x, y, color);
        int ax = x + this.font.width(this.titleText()) + ARROW_GAP;
        int ay = y + 2;
        for (int i = 0; i < 4; i++) {
            int row = this.filterOpen ? 3 - i : i;
            graphics.fill(ax + i, ay + row, ax + ARROW_W - i, ay + row + 1, color);
        }
    }

    private void renderFilterMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.optionLeft();
        int y = this.optionTop();
        int w = this.optionWidth();
        int h = CapeEntry.GAMES.size() * OPTION_H;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFA0A0A0);
        graphics.fill(x, y, x + w, y + h, 0xF0101010);
        int hover = this.optionAt(mouseX, mouseY);
        for (int i = 0; i < CapeEntry.GAMES.size(); i++) {
            String game = CapeEntry.GAMES.get(i);
            int oy = y + i * OPTION_H;
            if (i == hover) {
                graphics.fill(x, oy, x + w, oy + OPTION_H, 0x60FFFFFF);
            }
            boolean current = game.equals(CapeManager.getGameFilter());
            graphics.drawString(this.font, filterName(game), x + OPTION_PAD, oy + 3, current ? 0xFFFFFF55 : 0xFFFFFFFF);
        }
        graphics.pose().popPose();
    }

    private static Component glowLabel() {
        return Component.translatable(
                CapeManager.isNightGlow() ? "gui.localcapes.glow.on" : "gui.localcapes.glow.off"
        );
    }

    private void refreshVisible() {
        this.visible = CapeManager.entriesForGame(CapeManager.getGameFilter());
        this.rows = buildRows(this.visible);
        this.clampScroll();
        if (this.uploadButton != null) {
            this.uploadButton.visible = CapeEntry.GAME_MINECRAFT.equals(CapeManager.getGameFilter());
        }
    }

    private static List<Row> buildRows(List<CapeEntry> entries) {
        boolean headers = entries.stream().map(e -> e.category).distinct().count() > 1;
        List<Row> rows = new ArrayList<>();
        List<CapeEntry> current = new ArrayList<>();
        String category = null;
        for (CapeEntry entry : entries) {
            boolean newCategory = !entry.category.equals(category);
            if (newCategory || current.size() == COLUMNS) {
                if (!current.isEmpty()) {
                    rows.add(new Row(null, current));
                    current = new ArrayList<>();
                }
            }
            if (newCategory) {
                category = entry.category;
                if (headers) {
                    rows.add(new Row(category, List.of()));
                }
            }
            current.add(entry);
        }
        if (!current.isEmpty()) {
            rows.add(new Row(null, current));
        }
        return rows;
    }

    private List<Integer> sectionStarts() {
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < this.rows.size(); i++) {
            if (this.rows.get(i).header() != null) {
                starts.add(i);
            }
        }
        return starts;
    }

    private int maxScroll() {
        int height = 0;
        for (int i = this.rows.size() - 1; i >= 0; i--) {
            height += this.rows.get(i).height();
            if (height > GRID_H) {
                return i + 1;
            }
        }
        return 0;
    }

    private List<Slot> visibleSlots(int gridLeft, int gridTop, @Nullable List<Row> headersOut, @Nullable List<Integer> headerY) {
        List<Slot> slots = new ArrayList<>();
        int y = gridTop;
        for (int r = this.scroll; r < this.rows.size(); r++) {
            Row row = this.rows.get(r);
            if (y + row.height() > gridTop + GRID_H) {
                break;
            }
            if (row.header() != null) {
                if (headersOut != null && headerY != null) {
                    headersOut.add(row);
                    headerY.add(y);
                }
            } else {
                for (int c = 0; c < row.entries().size(); c++) {
                    slots.add(new Slot(row.entries().get(c), gridLeft + c * (SLOT_W + 4), y));
                }
            }
            y += row.height();
        }
        return slots;
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - PANEL_W / 2;
        int top = this.height / 2 - PANEL_H / 2;
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - PANEL_W / 2;
        int top = this.height / 2 - PANEL_H / 2;
        this.renderTitle(graphics, mouseX, mouseY);

        int gridTop = top + GRID_OFFSET;
        int gridLeft = left + 16;
        int gridBottom = gridTop + GRID_H;
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
        List<Row> headers = new ArrayList<>();
        List<Integer> headerY = new ArrayList<>();
        List<Slot> slots = this.visibleSlots(gridLeft, gridTop, headers, headerY);
        for (int h = 0; h < headers.size(); h++) {
            graphics.drawString(
                    this.font,
                    Component.translatable("gui.localcapes.category." + headers.get(h).header()),
                    gridLeft,
                    headerY.get(h) + 2,
                    0xFFE0C060
            );
        }
        for (Slot slot : slots) {
            CapeEntry entry = slot.entry();
            int sx = slot.x();
            int sy = slot.y();
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
            List<Integer> sections = this.sectionStarts();
            int page = this.scroll + 1;
            int pages = this.maxScroll() + 1;
            if (!sections.isEmpty()) {
                page = 0;
                for (int start : sections) {
                    if (start <= this.scroll) {
                        page++;
                    }
                }
                page = Math.max(page, 1);
                pages = sections.size();
                if (this.scroll >= this.maxScroll()) {
                    page = pages;
                }
            }
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(page + "/" + pages).withStyle(ChatFormatting.GRAY),
                    this.width / 2,
                    gridBottom + 2,
                    0xAAAAAA
            );
        }

        if (this.filterOpen) {
            this.renderFilterMenu(graphics, mouseX, mouseY);
            return;
        }
        if (hovered != null) {
            List<FormattedCharSequence> lines = tooltipLines(hovered);
            if (!lines.isEmpty()) {
                graphics.renderTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.filterOpen) {
            int option = this.optionAt(mouseX, mouseY);
            if (option >= 0 && button == 0) {
                CapeManager.setGameFilter(CapeEntry.GAMES.get(option));
                this.scroll = 0;
                this.refreshVisible();
            }
            this.filterOpen = false;
            return true;
        }
        if (button == 0 && this.isTitleHit(mouseX, mouseY)) {
            this.filterOpen = true;
            return true;
        }
        int left = this.width / 2 - PANEL_W / 2;
        int top = this.height / 2 - PANEL_H / 2;
        int gridTop = top + GRID_OFFSET;
        int gridLeft = left + 16;
        for (Slot slot : this.visibleSlots(gridLeft, gridTop, null, null)) {
            int sx = slot.x();
            int sy = slot.y();
            if (mouseX >= sx && mouseX < sx + SLOT_W && mouseY >= sy && mouseY < sy + SLOT_H) {
                CapeEntry entry = slot.entry();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (this.filterOpen) {
            return true;
        }
        List<Integer> sections = this.sectionStarts();
        if (sections.isEmpty()) {
            if (delta > 0) {
                this.scroll--;
            } else if (delta < 0) {
                this.scroll++;
            }
        } else if (delta > 0) {
            int target = 0;
            for (int start : sections) {
                if (start < this.scroll) {
                    target = start;
                }
            }
            this.scroll = target;
        } else if (delta < 0) {
            for (int start : sections) {
                if (start > this.scroll) {
                    this.scroll = start;
                    break;
                }
            }
        }
        this.clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.filterOpen && keyCode == 256) {
            this.filterOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        String nameKey = "cape.localcapes." + entry.category + "." + entry.id + ".name";
        lines.addAll(this.font.split(Component.translatableWithFallback(nameKey, entry.displayName), TOOLTIP_WIDTH));
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
