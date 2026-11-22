package dev.wwwrld.movingcrates.shipment;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class Shipment {
    public enum Status { PACKED, UNPACKED, VOIDED }

    public final UUID id;
    public final UUID packerId;
    public final String packerName;
    public final String furnitureName;
    public final int itemCount;
    public final GlobalPos origin;
    public final long packedAt;
    public Footprint footprint;
    public Status status = Status.PACKED;
    public GlobalPos destination;
    public String closedBy = "";
    public long closedAt;

    public Shipment(UUID id, UUID packerId, String packerName, String furnitureName, int itemCount, GlobalPos origin,
                    long packedAt, Footprint footprint) {
        this.id = id;
        this.packerId = packerId;
        this.packerName = packerName;
        this.furnitureName = furnitureName;
        this.itemCount = itemCount;
        this.origin = origin;
        this.packedAt = packedAt;
        this.footprint = footprint;
    }

    public boolean isPacked() {
        return status == Status.PACKED;
    }

    public void close(Status outcome, GlobalPos where, String who) {
        status = outcome;
        destination = where;
        closedBy = who;
        closedAt = System.currentTimeMillis();
    }

    public void reopen() {
        status = Status.PACKED;
        destination = null;
        closedBy = "";
    }

    public boolean canBeReopened() {
        return !isPacked() && footprint != null;
    }

    public void dropFootprintIfOlderThan(long cutoff) {
        if (!isPacked() && closedAt < cutoff) {
            footprint = null;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("PackerId", packerId);
        tag.putString("PackerName", packerName);
        tag.putString("FurnitureName", furnitureName);
        tag.putInt("ItemCount", itemCount);
        tag.put("Origin", encode(origin));
        tag.putLong("PackedAt", packedAt);
        tag.putString("Status", status.name());
        tag.putString("ClosedBy", closedBy);
        tag.putLong("ClosedAt", closedAt);
        if (destination != null) {
            tag.put("Destination", encode(destination));
        }
        if (footprint != null) {
            tag.put("Footprint", footprint.save());
        }
        return tag;
    }

    public static Shipment load(CompoundTag tag) {
        Footprint footprint = tag.contains("Footprint", Tag.TAG_LIST) ? Footprint.load(tag.getList("Footprint", Tag.TAG_COMPOUND)) : null;
        Shipment shipment = new Shipment(tag.getUUID("Id"), tag.getUUID("PackerId"), tag.getString("PackerName"),
                tag.getString("FurnitureName"), tag.getInt("ItemCount"), decode(tag.get("Origin")), tag.getLong("PackedAt"), footprint);
        shipment.status = Status.valueOf(tag.getString("Status"));
        shipment.closedBy = tag.getString("ClosedBy");
        shipment.closedAt = tag.getLong("ClosedAt");
        shipment.destination = tag.contains("Destination") ? decode(tag.get("Destination")) : null;
        return shipment;
    }

    private static Tag encode(GlobalPos pos) {
        return GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().orElseThrow();
    }

    private static GlobalPos decode(Tag tag) {
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
    }
}
