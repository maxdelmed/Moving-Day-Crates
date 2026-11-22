package dev.wwwrld.movingcrates.service;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.wwwrld.movingcrates.MovingCrates;
import dev.wwwrld.movingcrates.shipment.Shipment;
import dev.wwwrld.movingcrates.shipment.ShipmentLedger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod.EventBusSubscriber(modid = MovingCrates.MOD_ID)
public class CrateCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("movingcrates")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(CrateCommands::list))
                .then(Commands.literal("info")
                        .then(Commands.argument("shipment", UuidArgument.uuid()).executes(CrateCommands::info)))
                .then(Commands.literal("reissue")
                        .then(Commands.argument("shipment", UuidArgument.uuid()).executes(CrateCommands::reissue)))
                .then(Commands.literal("reopen")
                        .then(Commands.argument("shipment", UuidArgument.uuid()).executes(CrateCommands::reopen))));
    }

    private static Shipment shipmentOf(CommandContext<CommandSourceStack> context) {
        return ShipmentLedger.get(context.getSource().getServer()).get(UuidArgument.getUuid(context, "shipment"));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        int packed = 0;
        for (Shipment shipment : ShipmentLedger.get(context.getSource().getServer()).all()) {
            if (shipment.isPacked()) {
                packed++;
                context.getSource().sendSuccess(() -> describe(shipment), false);
            }
        }
        int total = packed;
        context.getSource().sendSuccess(() -> Component.translatable("movingcrates.command.list_total", total), false);
        return packed;
    }

    private static Component describe(Shipment shipment) {
        String id = shipment.id.toString();
        return Component.translatable("movingcrates.command.entry", shipment.furnitureName, shipment.itemCount,
                        shipment.packerName, place(shipment.origin))
                .append(Component.literal(" [" + id.substring(0, 8) + "]").withStyle(style ->
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/movingcrates info " + id))));
    }

    private static String place(GlobalPos pos) {
        return pos == null ? "-" : pos.pos().toShortString() + " " + pos.dimension().location();
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        Shipment shipment = shipmentOf(context);
        if (shipment == null) {
            context.getSource().sendFailure(Component.translatable("movingcrates.message.unknown_shipment"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("movingcrates.command.info", shipment.furnitureName,
                shipment.itemCount, shipment.packerName, shipment.status.name(), place(shipment.origin),
                place(shipment.destination), shipment.closedBy.isEmpty() ? "-" : shipment.closedBy), false);
        return 1;
    }

    private static int reissue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Shipment shipment = shipmentOf(context);
        if (shipment == null || !shipment.isPacked()) {
            context.getSource().sendFailure(Component.translatable("movingcrates.command.not_packed"));
            return 0;
        }
        ItemHandlerHelper.giveItemToPlayer(context.getSource().getPlayerOrException(), CrateService.reissue(shipment));
        context.getSource().sendSuccess(() -> Component.translatable("movingcrates.command.reissued", shipment.furnitureName), true);
        return 1;
    }

    private static int reopen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Shipment shipment = shipmentOf(context);
        if (shipment == null || !shipment.canBeReopened()) {
            context.getSource().sendFailure(Component.translatable("movingcrates.command.cannot_reopen"));
            return 0;
        }
        shipment.reopen();
        ShipmentLedger.get(context.getSource().getServer()).writeNow(context.getSource().getServer());
        ItemHandlerHelper.giveItemToPlayer(context.getSource().getPlayerOrException(), CrateService.reissue(shipment));
        context.getSource().sendSuccess(() -> Component.translatable("movingcrates.command.reopened", shipment.furnitureName), true);
        return 1;
    }
}
