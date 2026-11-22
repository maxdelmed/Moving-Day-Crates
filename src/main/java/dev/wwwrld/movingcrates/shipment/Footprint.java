package dev.wwwrld.movingcrates.shipment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public record Footprint(List<Part> parts) {
    public record Part(BlockPos offset, BlockState state, CompoundTag blockEntityTag) {
        public boolean hasBlockEntity() {
            return blockEntityTag != null;
        }
    }

    public static Footprint capture(ServerLevel level, BlockPos pos) {
        BlockPos mainPos = mainPosition(level, pos);
        BlockState state = level.getBlockState(mainPos);
        List<Part> parts = new ArrayList<>();
        parts.add(partAt(level, mainPos, BlockPos.ZERO));
        BlockPos other = otherHalf(state, mainPos);
        if (other == null) {
            other = pairedPart(level, state, mainPos);
        }
        if (other != null && level.getBlockState(other).is(state.getBlock())) {
            parts.add(partAt(level, other, other.subtract(mainPos)));
        }
        return new Footprint(parts);
    }

    private static net.minecraft.world.level.block.state.properties.Property<?> partProperty(BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals("part")) {
                return property;
            }
        }
        return null;
    }

    private static BlockPos pairedPart(ServerLevel level, BlockState state, BlockPos pos) {
        net.minecraft.world.level.block.state.properties.Property<?> part = partProperty(state);
        if (part == null) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(state.getBlock()) && !neighbor.getValue(part).equals(state.getValue(part))) {
                return pos.relative(direction);
            }
        }
        return null;
    }

    public static BlockPos mainPosition(ServerLevel level, BlockPos clicked) {
        BlockState state = level.getBlockState(clicked);
        net.minecraft.world.level.block.state.properties.Property<?> part = partProperty(state);
        if (part != null) {
            BlockPos other = pairedPart(level, state, clicked);
            if (other != null && level.getBlockState(other).getValue(part).toString().compareTo(state.getValue(part).toString()) < 0) {
                return other;
            }
            return clicked;
        }
        return isUpperHalf(state) && level.getBlockState(clicked.below()).is(state.getBlock()) ? clicked.below() : clicked;
    }

    private static boolean isUpperHalf(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
    }

    private static Part partAt(ServerLevel level, BlockPos pos, BlockPos offset) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return new Part(offset, portableState(level.getBlockState(pos)), blockEntity == null ? null : blockEntity.saveWithoutMetadata());
    }

    private static BlockState portableState(BlockState state) {
        BlockState portable = state;
        if (portable.hasProperty(BlockStateProperties.CHEST_TYPE)) {
            portable = portable.setValue(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE);
        }
        if (portable.hasProperty(BlockStateProperties.WATERLOGGED)) {
            portable = portable.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return portable;
    }

    private static BlockPos otherHalf(BlockState state, BlockPos pos) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
        }
        return null;
    }

    public BlockState mainState() {
        return parts.get(0).state();
    }

    public Direction facing() {
        BlockState state = mainState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
    }

    public Footprint rotated(Rotation rotation) {
        List<Part> turned = new ArrayList<>();
        for (Part part : parts) {
            turned.add(new Part(part.offset().rotate(rotation), part.state().rotate(rotation), part.blockEntityTag()));
        }
        return new Footprint(turned);
    }

    public static Rotation rotationBetween(Direction from, Direction to) {
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(from) == to) {
                return rotation;
            }
        }
        return Rotation.NONE;
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (Part part : parts) {
            CompoundTag tag = new CompoundTag();
            tag.put("Offset", NbtUtils.writeBlockPos(part.offset()));
            tag.put("State", NbtUtils.writeBlockState(part.state()));
            if (part.hasBlockEntity()) {
                tag.put("BlockEntity", part.blockEntityTag());
            }
            list.add(tag);
        }
        return list;
    }

    public static Footprint load(ListTag list) {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            parts.add(new Part(NbtUtils.readBlockPos(tag.getCompound("Offset")),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("State")),
                    tag.contains("BlockEntity", Tag.TAG_COMPOUND) ? tag.getCompound("BlockEntity") : null));
        }
        return new Footprint(parts);
    }
}
