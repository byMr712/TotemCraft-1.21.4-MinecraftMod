package com.totemcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.totemcraft.TotemCraftMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class TotemCraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TotemCraftConfig INSTANCE;

    public boolean enabled = true;
    public String[] patternSlots = new String[] {
            "minecraft:golden_apple", "minecraft:golden_apple", "minecraft:golden_apple",
            "minecraft:golden_apple", "minecraft:ghast_tear",   "minecraft:golden_apple",
            "minecraft:golden_apple", "minecraft:golden_apple", "minecraft:golden_apple"
    };
    public String resultItemId = "minecraft:totem_of_undying";
    public int resultCount = 1;

    public static TotemCraftConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("totemcraft.json");
    }

    public static TotemCraftConfig load() {
        File configFile = getConfigPath().toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                TotemCraftConfig config = GSON.fromJson(reader, TotemCraftConfig.class);
                if (config != null) {
                    config.validate();
                    INSTANCE = config;
                    return config;
                }
            } catch (Exception e) {
                TotemCraftMod.LOGGER.error("Failed to load TotemCraft config, using defaults", e);
            }
        }
        TotemCraftConfig config = new TotemCraftConfig();
        config.save();
        INSTANCE = config;
        return config;
    }

    public void save() {
        try {
            File configFile = getConfigPath().toFile();
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            TotemCraftMod.LOGGER.error("Failed to save TotemCraft config", e);
        }
    }

    public void validate() {
        if (patternSlots == null || patternSlots.length != 9) {
            resetSlots();
        }
        for (int i = 0; i < 9; i++) {
            if (patternSlots[i] == null || patternSlots[i].trim().isEmpty()) {
                patternSlots[i] = "minecraft:air";
            }
        }
        if (resultItemId == null || resultItemId.trim().isEmpty()) {
            resultItemId = "minecraft:totem_of_undying";
        }
        if (resultCount < 1) {
            resultCount = 1;
        } else if (resultCount > 64) {
            resultCount = 64;
        }
    }

    public void resetSlots() {
        patternSlots = new String[] {
                "minecraft:golden_apple", "minecraft:golden_apple", "minecraft:golden_apple",
                "minecraft:golden_apple", "minecraft:ghast_tear",   "minecraft:golden_apple",
                "minecraft:golden_apple", "minecraft:golden_apple", "minecraft:golden_apple"
        };
    }

    public void resetToDefaults() {
        enabled = true;
        resetSlots();
        resultItemId = "minecraft:totem_of_undying";
        resultCount = 1;
    }

    public Item getItemAt(int slot) {
        if (slot < 0 || slot >= 9 || patternSlots == null || slot >= patternSlots.length) {
            return Items.AIR;
        }
        String idStr = patternSlots[slot];
        if (idStr == null || idStr.equals("minecraft:air") || idStr.isEmpty()) {
            return Items.AIR;
        }
        Identifier id = Identifier.tryParse(idStr);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return Items.AIR;
        }
        return Registries.ITEM.get(id);
    }

    public void setItemAt(int slot, Item item) {
        if (slot >= 0 && slot < 9) {
            if (item == null || item == Items.AIR) {
                patternSlots[slot] = "minecraft:air";
            } else {
                Identifier id = Registries.ITEM.getId(item);
                patternSlots[slot] = id != null ? id.toString() : "minecraft:air";
            }
        }
    }

    public Item getResultItem() {
        if (resultItemId == null || resultItemId.isEmpty()) {
            return Items.TOTEM_OF_UNDYING;
        }
        Identifier id = Identifier.tryParse(resultItemId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return Items.TOTEM_OF_UNDYING;
        }
        return Registries.ITEM.get(id);
    }

    public void setResultItem(Item item) {
        if (item == null || item == Items.AIR) {
            resultItemId = "minecraft:totem_of_undying";
        } else {
            Identifier id = Registries.ITEM.getId(item);
            resultItemId = id != null ? id.toString() : "minecraft:totem_of_undying";
        }
    }

    public TotemCraftConfig copy() {
        TotemCraftConfig copy = new TotemCraftConfig();
        copy.enabled = this.enabled;
        copy.patternSlots = Arrays.copyOf(this.patternSlots, 9);
        copy.resultItemId = this.resultItemId;
        copy.resultCount = this.resultCount;
        return copy;
    }
}
