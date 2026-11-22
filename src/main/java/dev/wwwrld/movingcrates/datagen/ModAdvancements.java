package dev.wwwrld.movingcrates.datagen;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.registries.ForgeRegistries;

public class ModAdvancements implements ForgeAdvancementProvider.AdvancementGenerator {
    @Override
    public void generate(HolderLookup.Provider registries, Consumer<Advancement> saver, ExistingFileHelper files) {
        Advancement.Builder.advancement()
                .parent(Advancement.Builder.advancement().build(ResourceLocation.parse("minecraft:story/root")))
                .display(item("movingcrates:packed_crate"), Component.translatable("advancements.movingcrates.packed_crate.title"), Component.translatable("advancements.movingcrates.packed_crate.description"), null, FrameType.TASK, true, true, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(item("movingcrates:packed_crate")))
                .save(saver, "movingcrates:packed_crate");
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
    }
}
