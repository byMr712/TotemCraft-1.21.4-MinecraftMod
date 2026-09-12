package com.totemcraft;

import com.totemcraft.config.TotemCraftConfig;
import com.totemcraft.recipe.TotemCraftCustomRecipe;
import net.fabricmc.api.ModInitializer;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotemCraftMod implements ModInitializer {
    public static final String MOD_ID = "totemcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final RecipeSerializer<TotemCraftCustomRecipe> RECIPE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(MOD_ID, "dynamic_totem"),
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(TotemCraftCustomRecipe::new)
    );

    @Override
    public void onInitialize() {
        TotemCraftConfig.getInstance();
        LOGGER.info("TotemCraft initialized! Dynamic crafting recipe for Totem of Undying is active.");
    }
}
