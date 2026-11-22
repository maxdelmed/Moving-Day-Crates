package dev.wwwrld.movingcrates.shipment;

import dev.wwwrld.movingcrates.MovingCrates;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class ShipmentLedger extends SavedData {
    private static final String FILE_NAME = "movingcrates_shipments";

    private final Map<UUID, Shipment> shipments = new LinkedHashMap<>();

    public static ShipmentLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ShipmentLedger::load, ShipmentLedger::new, FILE_NAME);
    }

    public Shipment get(UUID id) {
        return id == null ? null : shipments.get(id);
    }

    public Collection<Shipment> all() {
        return shipments.values();
    }

    public void add(Shipment shipment) {
        shipments.put(shipment.id, shipment);
    }

    public void writeNow(MinecraftServer server) {
        long cutoff = System.currentTimeMillis() - MovingCrates.RECOVERY_DAYS.get() * 86_400_000L;
        shipments.values().forEach(shipment -> shipment.dropFootprintIfOlderThan(cutoff));
        setDirty();
        server.overworld().getDataStorage().save();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Shipment shipment : shipments.values()) {
            list.add(shipment.save());
        }
        tag.put("Shipments", list);
        return tag;
    }

    public static ShipmentLedger load(CompoundTag tag) {
        ShipmentLedger ledger = new ShipmentLedger();
        ListTag list = tag.getList("Shipments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Shipment shipment = Shipment.load(list.getCompound(i));
            ledger.shipments.put(shipment.id, shipment);
        }
        return ledger;
    }
}
