package dev.wwwrld.movingcrates;

import dev.wwwrld.movingcrates.item.MovingCrateItem;
import dev.wwwrld.movingcrates.item.PackedCrateItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MovingCrates.MOD_ID)
public class MovingCrates {
    public static final String MOD_ID = "movingcrates";

    public static final TagKey<Block> PACKABLE = TagKey.create(Registries.BLOCK, id("packable"));
    public static final TagKey<Item> PORTABLE_STORAGE = TagKey.create(Registries.ITEM, id("portable_storage"));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> MOVING_CRATE = ITEMS.register("moving_crate",
            () -> new MovingCrateItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PACKED_CRATE = ITEMS.register("packed_crate",
            () -> new PackedCrateItem(new Item.Properties().stacksTo(1)));

    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.IntValue MAX_CONTENT_BYTES;
    public static final ForgeConfigSpec.IntValue RECOVERY_DAYS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAX_CONTENT_BYTES = builder
                .comment("Largest saved size, in bytes, of one packed piece of furniture with its contents.")
                .defineInRange("maxContentBytes", 262144, 1024, 2097152);
        RECOVERY_DAYS = builder
                .comment("Days a closed shipment keeps its furniture data so an operator can reopen it after a crash.")
                .defineInRange("recoveryDays", 7, 0, 365);
        CONFIG = builder.build();
    }

    public MovingCrates(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(MovingCrates::addToCreativeTab);
        context.registerConfig(ModConfig.Type.SERVER, CONFIG);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> dev.wwwrld.movingcrates.client.ConfigScreen.register(context, CONFIG));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MOVING_CRATE);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
