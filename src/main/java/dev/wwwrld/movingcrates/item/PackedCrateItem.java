package dev.wwwrld.movingcrates.item;

import dev.wwwrld.movingcrates.MovingCrates;
import dev.wwwrld.movingcrates.service.CrateService;
import dev.wwwrld.movingcrates.shipment.Shipment;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PackedCrateItem extends Item {
    private static final String SHIPMENT_ID = "ShipmentId";
    private static final String FURNITURE = "Furniture";
    private static final String ITEM_COUNT = "ItemCount";
    private static final String PACKER = "Packer";
    private static final String TURN = "Turn";
    private static final String CONTENTS = "Contents";
    private static final int LISTED_ITEMS = 4;

    public PackedCrateItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Shipment shipment) {
        ItemStack stack = new ItemStack(MovingCrates.PACKED_CRATE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(SHIPMENT_ID, shipment.id);
        tag.putString(FURNITURE, shipment.furnitureName);
        tag.putInt(ITEM_COUNT, shipment.itemCount);
        tag.putString(PACKER, shipment.packerName);
        tag.put(CONTENTS, summary(shipment));
        return stack;
    }

    private static net.minecraft.nbt.ListTag summary(Shipment shipment) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        if (shipment.footprint != null) {
            for (dev.wwwrld.movingcrates.shipment.Footprint.Part part : shipment.footprint.parts()) {
                if (!part.hasBlockEntity()) {
                    continue;
                }
                net.minecraft.nbt.ListTag items = part.blockEntityTag().getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag item = items.getCompound(i);
                    counts.merge(item.getString("id"), Math.max(1, item.getByte("Count")), Integer::sum);
                }
            }
        }
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(LISTED_ITEMS).forEach(entry -> {
            CompoundTag line = new CompoundTag();
            line.putString("id", entry.getKey());
            line.putInt("Count", entry.getValue());
            list.add(line);
        });
        return list;
    }

    public static int turn(ItemStack stack) {
        return stack.hasTag() ? Math.floorMod(stack.getTag().getInt(TURN), 4) : 0;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (shipmentId(stack) == null || !player.isShiftKeyDown()) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            int next = (turn(stack) + 1) % 4;
            stack.getOrCreateTag().putInt(TURN, next);
            player.displayClientMessage(Component.translatable("item.movingcrates.packed_crate.turn." + next), true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static UUID shipmentId(ItemStack stack) {
        return stack.is(MovingCrates.PACKED_CRATE.get()) && stack.hasTag() && stack.getTag().hasUUID(SHIPMENT_ID)
                ? stack.getTag().getUUID(SHIPMENT_ID) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() instanceof ServerPlayer player) {
            BlockPos clicked = context.getClickedPos();
            BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
            CrateService.Result result = CrateService.unpack(player, context.getHand(), target, context.getClickedFace());
            player.displayClientMessage(result.message(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return shipmentId(stack) == null
                ? super.getName(stack)
                : Component.translatable("item.movingcrates.packed_crate.named", stack.getTag().getString(FURNITURE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        if (shipmentId(stack) == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        lines.add(Component.translatable("item.movingcrates.packed_crate.contents", tag.getInt(ITEM_COUNT)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.movingcrates.packed_crate.packer", tag.getString(PACKER)).withStyle(ChatFormatting.GRAY));
        net.minecraft.nbt.ListTag contents = tag.getList(CONTENTS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < contents.size(); i++) {
            CompoundTag line = contents.getCompound(i);
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(line.getString("id")));
            if (item != null) {
                lines.add(Component.translatable("item.movingcrates.packed_crate.line", line.getInt("Count"), item.getDescription())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.translatable("item.movingcrates.packed_crate.turn." + turn(stack)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.movingcrates.packed_crate.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
