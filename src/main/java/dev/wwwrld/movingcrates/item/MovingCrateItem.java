package dev.wwwrld.movingcrates.item;

import dev.wwwrld.movingcrates.MovingCrates;
import dev.wwwrld.movingcrates.service.CrateService;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MovingCrateItem extends Item {
    public MovingCrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(MovingCrates.PACKABLE)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            CrateService.Result result = CrateService.pack(player, context.getHand(), context.getClickedPos());
            player.displayClientMessage(result.message(), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.movingcrates.moving_crate.hint").withStyle(ChatFormatting.GRAY));
    }
}
