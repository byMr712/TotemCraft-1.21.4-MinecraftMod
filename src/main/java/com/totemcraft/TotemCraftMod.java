package com.totemcraft;

import com.mojang.serialization.MapCodec;
import com.totemcraft.config.TotemCraftConfig;
import com.totemcraft.recipe.TotemCraftCustomRecipe;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
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
            new RecipeSerializer<TotemCraftCustomRecipe>() {
                private final MapCodec<TotemCraftCustomRecipe> CODEC = MapCodec.unit(() -> new TotemCraftCustomRecipe(CraftingRecipeCategory.MISC));
                private final PacketCodec<RegistryByteBuf, TotemCraftCustomRecipe> PACKET_CODEC = PacketCodec.unit(new TotemCraftCustomRecipe(CraftingRecipeCategory.MISC));

                @Override
                public MapCodec<TotemCraftCustomRecipe> codec() {
                    return CODEC;
                }

                @Override
                public PacketCodec<RegistryByteBuf, TotemCraftCustomRecipe> packetCodec() {
                    return PACKET_CODEC;
                }
            }
    );

    @Override
    public void onInitialize() {
        TotemCraftConfig.getInstance();
        LOGGER.info("TotemCraft initialized! Dynamic shaped recipe for Totem of Undying is active.");
    }
}
