package dev.wwwrld.movingcrates.service;

import dev.wwwrld.movingcrates.MovingCrates;
import dev.wwwrld.movingcrates.item.PackedCrateItem;
import dev.wwwrld.movingcrates.shipment.Footprint;
import dev.wwwrld.movingcrates.shipment.Shipment;
import dev.wwwrld.movingcrates.shipment.ShipmentLedger;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;

public class CrateService {
    public record Result(boolean success, Component message) {
        static Result ok(String key, Object... args) {
            return new Result(true, Component.translatable("movingcrates.message." + key, args));
        }

        static Result fail(String key, Object... args) {
            return new Result(false, Component.translatable("movingcrates.message." + key, args));
        }
    }

    private static final int SILENT_REMOVE = 2 | 16 | 32;
    private static final int REMOVE_AND_UPDATE = 3 | 32;
    private static final int SILENT_PLACE = 2 | 16;

    public static Result pack(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        ItemStack crates = player.getItemInHand(hand);
        if (!crates.is(MovingCrates.MOVING_CRATE.get()) || !level.getBlockState(pos).is(MovingCrates.PACKABLE)) {
            return Result.fail("not_packable");
        }
        Footprint footprint = Footprint.capture(level, pos);
        BlockPos mainPos = Footprint.mainPosition(level, pos);
        for (Footprint.Part part : footprint.parts()) {
            BlockPos partPos = mainPos.offset(part.offset());
            if (!mayBreak(player, level, partPos)) {
                return Result.fail("protected");
            }
            ItemStack nested = ContentRules.findPortableStorage(level.getBlockEntity(partPos));
            if (!nested.isEmpty()) {
                return Result.fail("nested_storage", nested.getHoverName());
            }
        }
        if (footprint.save().sizeInBytes() > MovingCrates.MAX_CONTENT_BYTES.get()) {
            return Result.fail("too_heavy");
        }

        Shipment shipment = new Shipment(UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(),
                footprint.mainState().getBlock().getName().getString(), countItems(level, mainPos, footprint),
                GlobalPos.of(level.dimension(), mainPos), System.currentTimeMillis(), footprint);
        ShipmentLedger ledger = ShipmentLedger.get(player.server);
        ledger.add(shipment);
        ledger.writeNow(player.server);

        removeOriginal(level, mainPos, footprint);
        crates.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(player, PackedCrateItem.create(shipment));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BARREL_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
        return Result.ok("packed", shipment.furnitureName);
    }

    private static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return player.mayInteract(level, pos)
                && ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) >= 0;
    }

    private static int countItems(ServerLevel level, BlockPos mainPos, Footprint footprint) {
        int count = 0;
        for (Footprint.Part part : footprint.parts()) {
            count += ContentRules.countItems(level.getBlockEntity(mainPos.offset(part.offset())));
        }
        return count;
    }

    private static void removeOriginal(ServerLevel level, BlockPos mainPos, Footprint footprint) {
        for (int index = footprint.parts().size() - 1; index >= 0; index--) {
            BlockPos partPos = mainPos.offset(footprint.parts().get(index).offset());
            Clearable.tryClear(level.getBlockEntity(partPos));
            level.removeBlockEntity(partPos);
            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), index == 0 ? REMOVE_AND_UPDATE : SILENT_REMOVE);
        }
    }

    public static Result unpack(ServerPlayer player, InteractionHand hand, BlockPos target, Direction face) {
        ServerLevel level = player.serverLevel();
        ItemStack crate = player.getItemInHand(hand);
        ShipmentLedger ledger = ShipmentLedger.get(player.server);
        Shipment shipment = ledger.get(PackedCrateItem.shipmentId(crate));
        if (shipment == null) {
            return Result.fail("unknown_shipment");
        }
        if (!shipment.isPacked() || shipment.footprint == null) {
            return Result.fail(shipment.status == Shipment.Status.VOIDED ? "voided" : "already_unpacked");
        }
        if (originalCameBack(player.server, shipment)) {
            shipment.close(Shipment.Status.VOIDED, shipment.origin, "restored original");
            ledger.writeNow(player.server);
            return Result.fail("original_restored", shipment.origin.pos().toShortString());
        }
        Footprint footprint = facePlayer(shipment.footprint, player, PackedCrateItem.turn(crate));
        for (Footprint.Part part : footprint.parts()) {
            BlockPos partPos = target.offset(part.offset());
            if (isBlocked(level, partPos, part.state())) {
                return Result.fail("blocked");
            }
            if (!mayPlace(player, level, partPos, face, crate)) {
                return Result.fail("protected");
            }
        }
        if (!footprint.mainState().canSurvive(level, target)) {
            return Result.fail("blocked");
        }

        place(level, target, footprint);
        shipment.close(Shipment.Status.UNPACKED, GlobalPos.of(level.dimension(), target), player.getGameProfile().getName());
        ledger.writeNow(player.server);
        crate.shrink(1);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BARREL_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
        return Result.ok("unpacked", shipment.furnitureName);
    }

    private static boolean originalCameBack(MinecraftServer server, Shipment shipment) {
        Footprint.Part main = shipment.footprint.parts().get(0);
        ServerLevel originLevel = server.getLevel(shipment.origin.dimension());
        if (shipment.itemCount == 0 || !main.hasBlockEntity() || originLevel == null) {
            return false;
        }
        BlockEntity present = originLevel.getBlockEntity(shipment.origin.pos());
        return present != null
                && originLevel.getBlockState(shipment.origin.pos()).is(main.state().getBlock())
                && present.saveWithoutMetadata().equals(main.blockEntityTag());
    }

    private static Footprint facePlayer(Footprint footprint, ServerPlayer player, int turn) {
        Direction stored = footprint.facing();
        if (stored == null) {
            return footprint;
        }
        Direction wanted = player.getDirection().getOpposite();
        for (int step = 0; step < turn; step++) {
            wanted = wanted.getClockWise();
        }
        Rotation rotation = Footprint.rotationBetween(stored, wanted);
        return footprint.rotated(rotation);
    }

    private static boolean isBlocked(ServerLevel level, BlockPos pos, BlockState state) {
        return level.isOutsideBuildHeight(pos)
                || !level.getBlockState(pos).canBeReplaced()
                || !level.isUnobstructed(state, pos, CollisionContext.empty());
    }

    private static boolean mayPlace(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack crate) {
        return player.mayInteract(level, pos)
                && player.mayUseItemAt(pos, face, crate)
                && !ForgeEventFactory.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), face);
    }

    private static void place(ServerLevel level, BlockPos target, Footprint footprint) {
        for (Footprint.Part part : footprint.parts()) {
            BlockPos partPos = target.offset(part.offset());
            level.setBlock(partPos, part.state(), SILENT_PLACE);
            BlockEntity blockEntity = level.getBlockEntity(partPos);
            if (blockEntity != null && part.hasBlockEntity()) {
                blockEntity.load(part.blockEntityTag().copy());
                blockEntity.setChanged();
            }
        }
        for (Footprint.Part part : footprint.parts()) {
            BlockPos partPos = target.offset(part.offset());
            level.sendBlockUpdated(partPos, Blocks.AIR.defaultBlockState(), part.state(), 3);
            level.updateNeighborsAt(partPos, part.state().getBlock());
        }
    }

    public static ItemStack reissue(Shipment shipment) {
        return PackedCrateItem.create(shipment);
    }
}
