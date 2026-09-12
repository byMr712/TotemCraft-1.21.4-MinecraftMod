package com.totemcraft.client.gui;

import com.totemcraft.config.TotemCraftConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class TotemCraftConfigScreen extends Screen {
    private final Screen parent;
    private final TotemCraftConfig configCopy;

    private static final int RESULT_SLOT = 9;
    private int selectedSlot = 0; // 0..8 - crafting grid, 9 - result

    // Mod Tabs
    public static class ModTab {
        public final String id; // "all", "minecraft", or mod namespace
        public final String displayName;

        public ModTab(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private final List<ModTab> modTabs = new ArrayList<>();
    private int selectedTabIdx = 0;
    private int tabScrollOffset = 0;
    private int visibleTabs = 4;

    // Dynamic Item picker catalog
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> filteredItems = new ArrayList<>();
    private int catalogPage = 0;
    private int catalogCols = 10;
    private int catalogRows = 5;
    private int itemsPerPage = 50;

    // Layout coordinates (calculated dynamically in init)
    private int leftPaneX;
    private int rightPaneX;
    private int contentY;
    private int catalogGridY;
    private int searchWidth;
    private static final int SLOT_SIZE = 24;
    private static final int LEFT_PANE_WIDTH = 186;

    private TextFieldWidget searchField;
    private ButtonWidget prevPageBtn;
    private ButtonWidget nextPageBtn;
    private ButtonWidget toggleEnabledBtn;
    private ButtonWidget prevTabBtn;
    private ButtonWidget nextTabBtn;
    private final List<ButtonWidget> tabButtons = new ArrayList<>();

    public TotemCraftConfigScreen(Screen parent) {
        super(Text.translatable("totemcraft.config.title"));
        this.parent = parent;
        this.configCopy = TotemCraftConfig.getInstance().copy();

        // 1. Gather all items
        Set<String> namespaces = new LinkedHashSet<>();
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(item);
                Identifier id = Registries.ITEM.getId(item);
                if (id != null) {
                    namespaces.add(id.getNamespace());
                }
            }
        }

        // 2. Build tabs: All -> Minecraft -> Other mods
        modTabs.add(new ModTab("all", "Все / All"));
        if (namespaces.remove("minecraft")) {
            modTabs.add(new ModTab("minecraft", "Minecraft"));
        }
        for (String ns : namespaces) {
            String modName = getFriendlyModName(ns);
            modTabs.add(new ModTab(ns, modName));
        }

        refreshFilteredItems();
    }

    private String getFriendlyModName(String namespace) {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(namespace);
        if (container.isPresent()) {
            return container.get().getMetadata().getName();
        }
        if (!namespace.isEmpty()) {
            return Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1);
        }
        return namespace;
    }

    private void refreshFilteredItems() {
        filteredItems.clear();
        String currentTabId = modTabs.get(selectedTabIdx).id;
        String query = searchField != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";

        for (Item item : allItems) {
            Identifier id = Registries.ITEM.getId(item);
            if (id == null) continue;

            // Namespace filter
            if (!currentTabId.equals("all")) {
                if (!id.getNamespace().equalsIgnoreCase(currentTabId)) {
                    continue;
                }
            }

            // Search query filter (matches item name or raw id)
            if (!query.isEmpty()) {
                String idStr = id.toString().toLowerCase(Locale.ROOT);
                String nameStr = item.getName().getString().toLowerCase(Locale.ROOT);
                if (!idStr.contains(query) && !nameStr.contains(query)) {
                    continue;
                }
            }

            filteredItems.add(item);
        }

        catalogPage = 0;
        updatePaginationButtons();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int topY = 22;

        // Top toggle button: Recipe Enabled / Disabled
        updateToggleBtn(centerX - 80, topY);

        // Content starts below top panel
        contentY = topY + 28; // ~50
        int bottomY = this.height - 26;

        // Dynamic Calculation of Catalog Dimensions based on screen size
        int availableTotalWidth = Math.max(300, this.width - 24);
        int availableRightWidth = Math.max(120, availableTotalWidth - LEFT_PANE_WIDTH - 16);
        catalogCols = Math.max(4, availableRightWidth / SLOT_SIZE);

        int actualCatalogWidth = catalogCols * SLOT_SIZE;
        searchWidth = actualCatalogWidth - 2;

        int totalContentWidth = LEFT_PANE_WIDTH + 16 + actualCatalogWidth;
        leftPaneX = Math.max(8, (this.width - totalContentWidth) / 2);
        rightPaneX = leftPaneX + LEFT_PANE_WIDTH + 16;

        // Dynamic Rows Calculation
        int searchY = contentY + 20;
        catalogGridY = searchY + 22;
        int availableCatalogHeight = Math.max(SLOT_SIZE * 3, (bottomY - 26) - catalogGridY);
        catalogRows = Math.max(3, availableCatalogHeight / SLOT_SIZE);
        itemsPerPage = catalogCols * catalogRows;

        // Dynamic Tabs Visibility
        visibleTabs = Math.max(2, (searchWidth - 44) / 58);

        // --- LEFT PANE (Crafting Grid & Actions) ---
        int actionBtnY = contentY + 104;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.clear_slot"), btn -> clearSelectedSlot())
                .dimensions(leftPaneX, actionBtnY, 88, 18)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.fill_outer"), btn -> fillOuterWithSelected())
                .dimensions(leftPaneX + 92, actionBtnY, 88, 18)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.clear_all"), btn -> clearAllSlots())
                .dimensions(leftPaneX, actionBtnY + 22, 180, 18)
                .build());

        // Result count buttons
        int resultX = leftPaneX + 138;
        int resultY = contentY + 44;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> adjustResultCount(-1))
                .dimensions(resultX - 3, resultY + 32, 18, 18)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> adjustResultCount(1))
                .dimensions(resultX + 19, resultY + 32, 18, 18)
                .build());

        // --- RIGHT PANE (Mod Tabs, Search, Dynamic Catalog) ---
        int tabY = contentY;
        prevTabBtn = ButtonWidget.builder(Text.literal("◀"), btn -> scrollTabs(-1))
                .dimensions(rightPaneX, tabY, 18, 16)
                .build();
        nextTabBtn = ButtonWidget.builder(Text.literal("▶"), btn -> scrollTabs(1))
                .dimensions(rightPaneX + searchWidth - 18, tabY, 18, 16)
                .build();
        this.addDrawableChild(prevTabBtn);
        this.addDrawableChild(nextTabBtn);
        rebuildTabButtons(rightPaneX + 22, tabY);

        // Search Field
        String previousSearch = searchField != null ? searchField.getText() : "";
        searchField = new TextFieldWidget(this.textRenderer, rightPaneX, searchY, searchWidth, 18, Text.literal("Search"));
        searchField.setText(previousSearch);
        searchField.setPlaceholder(Text.translatable("totemcraft.gui.search_placeholder").formatted(Formatting.GRAY));
        searchField.setChangedListener(query -> refreshFilteredItems());
        this.addDrawableChild(searchField);

        // Catalog Pagination Buttons
        int pageBtnY = catalogGridY + (catalogRows * SLOT_SIZE) + 6;
        prevPageBtn = ButtonWidget.builder(Text.literal("◀"), btn -> changePage(-1))
                .dimensions(rightPaneX, pageBtnY, 24, 18)
                .build();
        nextPageBtn = ButtonWidget.builder(Text.literal("▶"), btn -> changePage(1))
                .dimensions(rightPaneX + searchWidth - 24, pageBtnY, 24, 18)
                .build();
        this.addDrawableChild(prevPageBtn);
        this.addDrawableChild(nextPageBtn);
        updatePaginationButtons();

        // --- BOTTOM PANE (Controls) ---
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.reset_defaults"), btn -> resetDefaults())
                .dimensions(centerX - 190, bottomY, 115, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.save"), btn -> saveAndClose())
                .dimensions(centerX - 60, bottomY, 120, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("totemcraft.gui.cancel"), btn -> this.close())
                .dimensions(centerX + 75, bottomY, 115, 20)
                .build());
    }

    private void scrollTabs(int delta) {
        int maxOffset = Math.max(0, modTabs.size() - visibleTabs);
        tabScrollOffset = Math.max(0, Math.min(maxOffset, tabScrollOffset + delta));
        rebuildTabButtons(rightPaneX + 22, contentY);
    }

    private void rebuildTabButtons(int startX, int startY) {
        for (ButtonWidget btn : tabButtons) {
            this.remove(btn);
        }
        tabButtons.clear();

        int maxOffset = Math.max(0, modTabs.size() - visibleTabs);
        if (prevTabBtn != null) prevTabBtn.active = tabScrollOffset > 0;
        if (nextTabBtn != null) nextTabBtn.active = tabScrollOffset < maxOffset;

        int availableTabsWidth = searchWidth - 44;
        int tabWidth = Math.max(38, (availableTabsWidth - (visibleTabs - 1) * 2) / Math.max(1, visibleTabs));

        for (int i = 0; i < visibleTabs; i++) {
            int tabIndex = tabScrollOffset + i;
            if (tabIndex >= modTabs.size()) break;

            ModTab tab = modTabs.get(tabIndex);
            boolean isSelected = (tabIndex == selectedTabIdx);

            String label = tab.displayName;
            int maxChars = Math.max(4, tabWidth / 6);
            if (label.length() > maxChars) {
                label = label.substring(0, maxChars - 1) + "…";
            }
            Text tabText = isSelected
                    ? Text.literal(label).formatted(Formatting.YELLOW, Formatting.BOLD)
                    : Text.literal(label).formatted(Formatting.GRAY);

            final int chosenIdx = tabIndex;
            ButtonWidget btn = ButtonWidget.builder(tabText, b -> {
                selectedTabIdx = chosenIdx;
                refreshFilteredItems();
                rebuildTabButtons(startX, startY);
            }).dimensions(startX + (i * (tabWidth + 2)), startY, tabWidth, 16).build();

            tabButtons.add(btn);
            this.addDrawableChild(btn);
        }
    }

    private void updateToggleBtn(int x, int y) {
        if (toggleEnabledBtn != null) {
            this.remove(toggleEnabledBtn);
        }
        Text toggleText = configCopy.enabled
                ? Text.translatable("totemcraft.gui.recipe_enabled").formatted(Formatting.GREEN, Formatting.BOLD)
                : Text.translatable("totemcraft.gui.recipe_disabled").formatted(Formatting.RED, Formatting.BOLD);

        toggleEnabledBtn = ButtonWidget.builder(toggleText, btn -> {
            configCopy.enabled = !configCopy.enabled;
            updateToggleBtn(x, y);
        }).dimensions(x, y, 160, 20).build();
        this.addDrawableChild(toggleEnabledBtn);
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (filteredItems.size() - 1) / Math.max(1, itemsPerPage));
        catalogPage = Math.max(0, Math.min(maxPage, catalogPage + delta));
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        int maxPage = Math.max(0, (filteredItems.size() - 1) / Math.max(1, itemsPerPage));
        if (prevPageBtn != null) prevPageBtn.active = catalogPage > 0;
        if (nextPageBtn != null) nextPageBtn.active = catalogPage < maxPage;
    }

    private void adjustResultCount(int delta) {
        configCopy.resultCount = Math.max(1, Math.min(64, configCopy.resultCount + delta));
    }

    private void clearSelectedSlot() {
        if (selectedSlot >= 0 && selectedSlot < 9) {
            configCopy.setItemAt(selectedSlot, Items.AIR);
        } else if (selectedSlot == RESULT_SLOT) {
            configCopy.setResultItem(Items.TOTEM_OF_UNDYING);
        }
    }

    private void fillOuterWithSelected() {
        Item itemToUse;
        if (selectedSlot >= 0 && selectedSlot < 9) {
            itemToUse = configCopy.getItemAt(selectedSlot);
        } else {
            itemToUse = configCopy.getResultItem();
        }
        if (itemToUse == null || itemToUse == Items.AIR) {
            itemToUse = Items.GOLDEN_APPLE;
        }
        int[] outerIndices = {0, 1, 2, 3, 5, 6, 7, 8};
        for (int idx : outerIndices) {
            configCopy.setItemAt(idx, itemToUse);
        }
    }

    private void clearAllSlots() {
        for (int i = 0; i < 9; i++) {
            configCopy.setItemAt(i, Items.AIR);
        }
    }

    private void resetDefaults() {
        configCopy.resetToDefaults();
    }

    private void saveAndClose() {
        TotemCraftConfig actual = TotemCraftConfig.getInstance();
        actual.enabled = configCopy.enabled;
        actual.patternSlots = configCopy.patternSlots;
        actual.resultItemId = configCopy.resultItemId;
        actual.resultCount = configCopy.resultCount;
        actual.save();
        this.close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int catalogWidth = catalogCols * SLOT_SIZE + 10;
        int catalogHeight = (catalogRows * SLOT_SIZE) + 30;

        // Check if mouse is hovering anywhere over the right catalog pane
        if (mouseX >= rightPaneX && mouseX <= rightPaneX + catalogWidth &&
            mouseY >= contentY && mouseY <= catalogGridY + catalogHeight) {
            if (verticalAmount > 0) {
                changePage(-1);
                return true;
            } else if (verticalAmount < 0) {
                changePage(1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 8, 0xFFFFFF);

        // Section Headers
        context.drawTextWithShadow(this.textRenderer, Text.translatable("totemcraft.gui.grid_title").formatted(Formatting.YELLOW), leftPaneX, contentY + 2, 0xFFFFAA00);

        // Tooltip stack to render at the end
        ItemStack hoveredStack = ItemStack.EMPTY;

        // --- DRAW 3x3 CRAFTING GRID ---
        int gridStartX = leftPaneX + 4;
        int gridStartY = contentY + 16;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int x = gridStartX + col * SLOT_SIZE;
                int y = gridStartY + row * SLOT_SIZE;

                boolean isSelected = (selectedSlot == slotIndex);
                drawSlotBox(context, x, y, 22, 22, isSelected);

                Item item = configCopy.getItemAt(slotIndex);
                if (item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    context.drawItem(stack, x + 3, y + 3);
                    context.drawStackOverlay(this.textRenderer, stack, x + 3, y + 3);
                }

                if (mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22) {
                    if (item != Items.AIR) {
                        hoveredStack = new ItemStack(item);
                    }
                }
            }
        }

        // Draw Arrow
        int arrowX = gridStartX + 3 * SLOT_SIZE + 10;
        int arrowY = gridStartY + SLOT_SIZE + 2;
        context.drawTextWithShadow(this.textRenderer, Text.literal("➡").formatted(Formatting.GOLD, Formatting.BOLD), arrowX, arrowY, 0xFFFFAA00);

        // Draw Result Slot
        int resultX = arrowX + 24;
        int resultY = gridStartY + SLOT_SIZE - 3;
        boolean isResultSelected = (selectedSlot == RESULT_SLOT);
        drawSlotBox(context, resultX, resultY, 28, 28, isResultSelected);

        Item resItem = configCopy.getResultItem();
        ItemStack resStack = new ItemStack(resItem, configCopy.resultCount);
        context.drawItem(resStack, resultX + 6, resultY + 6);
        context.drawStackOverlay(this.textRenderer, resStack, resultX + 6, resultY + 6);

        if (mouseX >= resultX && mouseX <= resultX + 28 && mouseY >= resultY && mouseY <= resultY + 28) {
            hoveredStack = resStack;
        }

        // Result count label
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x" + configCopy.resultCount).formatted(Formatting.WHITE, Formatting.BOLD), resultX + 14, resultY + 36, 0xFFFFFFFF);

        // Active slot description text
        Text slotName = selectedSlot == RESULT_SLOT
                ? Text.translatable("totemcraft.gui.selected_result")
                : Text.translatable("totemcraft.gui.selected_slot", selectedSlot + 1);
        context.drawTextWithShadow(this.textRenderer, slotName.copy().formatted(Formatting.GREEN), leftPaneX, contentY + 90, 0xFF55FF55);

        // --- DRAW DYNAMIC CATALOG GRID ---
        int startIndex = catalogPage * itemsPerPage;
        int endIndex = Math.min(filteredItems.size(), startIndex + itemsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            int localIdx = i - startIndex;
            int cRow = localIdx / catalogCols;
            int cCol = localIdx % catalogCols;

            int slotX = rightPaneX + cCol * SLOT_SIZE;
            int slotY = catalogGridY + cRow * SLOT_SIZE;

            boolean isHovered = mouseX >= slotX && mouseX <= slotX + 22 && mouseY >= slotY && mouseY <= slotY + 22;
            drawSlotBox(context, slotX, slotY, 22, 22, isHovered);

            Item catItem = filteredItems.get(i);
            ItemStack catStack = new ItemStack(catItem);
            context.drawItem(catStack, slotX + 3, slotY + 3);

            if (isHovered) {
                hoveredStack = catStack;
            }
        }

        // Catalog Page Info
        int maxPage = Math.max(1, (filteredItems.size() + itemsPerPage - 1) / Math.max(1, itemsPerPage));
        MutableText pageInfo = Text.translatable("totemcraft.gui.items_total", catalogPage + 1, maxPage, filteredItems.size());
        context.drawCenteredTextWithShadow(this.textRenderer, pageInfo.formatted(Formatting.GRAY), rightPaneX + (searchWidth / 2), catalogGridY + (catalogRows * SLOT_SIZE) + 10, 0xFFAAAAAA);

        // Render Tooltip if hovered
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private void drawSlotBox(DrawContext context, int x, int y, int w, int h, boolean highlight) {
        int borderColor = highlight ? 0xFFFFD700 : 0xFF373737;
        int innerBg = highlight ? 0x66FFAA00 : 0x88000000;

        context.fill(x, y, x + w, y + h, innerBg);
        context.drawBorder(x, y, w, h, borderColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Check 3x3 crafting grid click
        int gridStartX = leftPaneX + 4;
        int gridStartY = contentY + 16;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int x = gridStartX + col * SLOT_SIZE;
                int y = gridStartY + row * SLOT_SIZE;

                if (mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22) {
                    if (button == 1) { // Right click clears slot
                        configCopy.setItemAt(slotIndex, Items.AIR);
                    } else {
                        selectedSlot = slotIndex;
                    }
                    return true;
                }
            }
        }

        // Check result slot click
        int arrowX = gridStartX + 3 * SLOT_SIZE + 10;
        int resultX = arrowX + 24;
        int resultY = gridStartY + SLOT_SIZE - 3;
        if (mouseX >= resultX && mouseX <= resultX + 28 && mouseY >= resultY && mouseY <= resultY + 28) {
            selectedSlot = RESULT_SLOT;
            return true;
        }

        // Check catalog grid click
        int startIndex = catalogPage * itemsPerPage;
        int endIndex = Math.min(filteredItems.size(), startIndex + itemsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            int localIdx = i - startIndex;
            int cRow = localIdx / catalogCols;
            int cCol = localIdx % catalogCols;

            int slotX = rightPaneX + cCol * SLOT_SIZE;
            int slotY = catalogGridY + cRow * SLOT_SIZE;

            if (mouseX >= slotX && mouseX <= slotX + 22 && mouseY >= slotY && mouseY <= slotY + 22) {
                Item chosenItem = filteredItems.get(i);
                if (selectedSlot >= 0 && selectedSlot < 9) {
                    configCopy.setItemAt(selectedSlot, chosenItem);
                } else if (selectedSlot == RESULT_SLOT) {
                    configCopy.setResultItem(chosenItem);
                }
                return true;
            }
        }

        return false;
    }
}
