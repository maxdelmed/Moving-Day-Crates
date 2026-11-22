package dev.wwwrld.movingcrates.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemTags extends ItemTagsProvider {
    public ModItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                       CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, "movingcrates", files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.create(ResourceLocation.parse("movingcrates:portable_storage")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:white_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:orange_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:magenta_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:light_blue_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:yellow_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:lime_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:pink_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:gray_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:light_gray_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:cyan_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:purple_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:blue_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:brown_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:green_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:red_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:black_shulker_box")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:bundle")))
                .addOptional(ResourceLocation.parse("backpacked:backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:copper_backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:iron_backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:gold_backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:diamond_backpack"))
                .addOptional(ResourceLocation.parse("sophisticatedbackpacks:netherite_backpack"))
                .addOptional(ResourceLocation.parse("travelersbackpack:standard"))
                .addOptional(ResourceLocation.parse("carryon:entity_item"))
                .addOptional(ResourceLocation.parse("carryon:tile_item"))
                .addOptional(ResourceLocation.parse("supplementaries:sack"))
                .addOptional(ResourceLocation.parse("supplementaries:safe"))
                .addOptional(ResourceLocation.parse("supplementaries:lunch_basket"));
    }
}
