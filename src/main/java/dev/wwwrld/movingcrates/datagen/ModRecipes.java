package dev.wwwrld.movingcrates.datagen;

import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRecipes extends RecipeProvider {
    public ModRecipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("movingcrates:moving_crate"), 2)
                .pattern("PSP")
                .pattern("P P")
                .pattern("PPP")
                .define('P', tag("minecraft:planks"))
                .define('S', item("minecraft:string"))
                .unlockedBy("has_planks", has(tag("minecraft:planks")))
                .save(writer, id("moving_crate"));
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
    }

    private static TagKey<Item> tag(String id) {
        return ItemTags.create(ResourceLocation.parse(id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("movingcrates", path);
    }

    @SuppressWarnings("unchecked")
    private static RecipeSerializer<? extends CraftingRecipe> serializer(String id) {
        return (RecipeSerializer<? extends CraftingRecipe>) ForgeRegistries.RECIPE_SERIALIZERS.getValue(ResourceLocation.parse(id));
    }
}
