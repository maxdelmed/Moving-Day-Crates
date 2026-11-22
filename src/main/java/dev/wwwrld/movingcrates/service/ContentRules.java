package dev.wwwrld.movingcrates.service;

import dev.wwwrld.movingcrates.MovingCrates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class ContentRules {
    public static int countItems(BlockEntity blockEntity) {
        IItemHandler items = itemsOf(blockEntity);
        int count = 0;
        for (int slot = 0; items != null && slot < items.getSlots(); slot++) {
            count += items.getStackInSlot(slot).getCount();
        }
        return count;
    }

    public static ItemStack findPortableStorage(BlockEntity blockEntity) {
        IItemHandler items = itemsOf(blockEntity);
        for (int slot = 0; items != null && slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (isPortableStorage(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static IItemHandler itemsOf(BlockEntity blockEntity) {
        return blockEntity == null ? null : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    public static boolean isPortableStorage(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(MovingCrates.PORTABLE_STORAGE)
                || stack.is(MovingCrates.PACKED_CRATE.get())
                || stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()
                || carriesStoredItems(stack);
    }

    private static boolean carriesStoredItems(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        CompoundTag blockData = tag.getCompound("BlockEntityTag");
        return !tag.getList("Items", Tag.TAG_COMPOUND).isEmpty() || !blockData.getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
}
