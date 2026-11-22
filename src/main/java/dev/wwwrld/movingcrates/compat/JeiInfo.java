package dev.wwwrld.movingcrates.compat;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class JeiInfo implements IModPlugin {
    private static final String NAMESPACE = "movingcrates";
    private static final List<String> ITEMS = List.of("moving_crate", "packed_crate");

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "info");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (String id : ITEMS) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(NAMESPACE, id));
            if (item != null && item != Items.AIR) {
                registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK,
                        Component.translatable(NAMESPACE + ".jei." + id));
            }
        }
    }
}
