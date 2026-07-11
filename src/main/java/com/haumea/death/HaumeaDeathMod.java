package com.haumea.death;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HaumeaDeathMod implements ModInitializer {
    public static final String MOD_ID = "haumeadeath";
    public static final Logger LOGGER = LoggerFactory.getLogger("HaumeaDeath");

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(HaumeaDeathMod::onAllowDeath);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
        LOGGER.info("HaumeaDeath carregado — coordenadas + inventário da morte");
    }

    private static boolean onAllowDeath(net.minecraft.entity.LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }
        // Snapshot BEFORE vanilla drops the inventory
        PlayerInventory inv = player.getInventory();
        NbtList nbt = DeathRecord.encodeInventory(inv);
        BlockPos pos = player.getBlockPos();
        DeathRecord record = new DeathRecord(
                player.getUuid(),
                player.getGameProfile().getName(),
                pos,
                player.getWorld().getRegistryKey(),
                nbt
        );
        DeathStorage.put(record);
        sendDeathMessage(player, record);
        LOGGER.info(
                "Morte de {} em {} ({}) — {} itens salvos",
                record.playerName,
                formatPos(record.pos),
                record.dimension.getValue(),
                record.countItems()
        );
        return true;
    }

    private static void sendDeathMessage(ServerPlayerEntity player, DeathRecord record) {
        String dim = record.dimension.getValue().toString();
        MutableText header = Text.literal("")
                .append(Text.literal("HaumeaDeath").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("Você morreu em ").formatted(Formatting.GRAY))
                .append(Text.literal(formatPos(record.pos)).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(" [" + shortDim(dim) + "]").formatted(Formatting.DARK_AQUA));

        MutableText viewBtn = Text.literal(" [Ver inventário] ")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/haumeadeath view"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Abre o inventário logo antes da morte (somente visualização)")
                                        .formatted(Formatting.GRAY)
                        )));

        MutableText restoreBtn = Text.literal("[Copiar inventário]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/haumeadeath restore"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Abre confirmação para recuperar o inventário da morte")
                                        .formatted(Formatting.GRAY)
                                        .append(Text.literal("\nSomente você pode usar isso.")
                                                .formatted(Formatting.DARK_GRAY))
                        )));

        MutableText buttons = Text.literal("")
                .append(Text.literal("Ações: ").formatted(Formatting.DARK_GRAY))
                .append(viewBtn)
                .append(Text.literal(" "))
                .append(restoreBtn);

        player.sendMessage(header, false);
        player.sendMessage(buttons, false);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("haumeadeath")
                        .then(CommandManager.literal("view")
                                .executes(ctx -> viewInventory(ctx.getSource())))
                        .then(CommandManager.literal("restore")
                                .executes(ctx -> openRestoreConfirm(ctx.getSource())))
        );
    }

    private static int viewInventory(ServerCommandSource source) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Apenas jogadores."));
            return 0;
        }

        var opt = DeathStorage.get(player.getUuid());
        if (opt.isEmpty()) {
            player.sendMessage(prefix().append(Text.literal("Nenhuma morte salva.").formatted(Formatting.RED)), false);
            return 0;
        }
        DeathRecord record = opt.get();
        if (!record.playerId.equals(player.getUuid())) {
            player.sendMessage(prefix().append(Text.literal("Você só pode ver o seu inventário.").formatted(Formatting.RED)), false);
            return 0;
        }

        var inv = ViewOnlyScreenHandler.fromRecord(record);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new ViewOnlyScreenHandler(syncId, playerInv, inv),
                Text.literal("Inventário da morte").formatted(Formatting.DARK_PURPLE)
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static int openRestoreConfirm(ServerCommandSource source) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Apenas jogadores."));
            return 0;
        }

        var opt = DeathStorage.get(player.getUuid());
        if (opt.isEmpty()) {
            player.sendMessage(prefix().append(Text.literal("Nenhuma morte salva.").formatted(Formatting.RED)), false);
            return 0;
        }
        DeathRecord record = opt.get();
        if (!record.playerId.equals(player.getUuid())) {
            player.sendMessage(prefix().append(Text.literal("Você só pode recuperar o seu inventário.").formatted(Formatting.RED)), false);
            return 0;
        }
        if (record.claimed) {
            player.sendMessage(prefix().append(Text.literal("Este inventário já foi recuperado.").formatted(Formatting.RED)), false);
            return 0;
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new ConfirmRestoreScreenHandler(
                        syncId,
                        playerInv,
                        new ConfirmRestoreScreenHandler.UUIDOwner(player.getUuid())
                ),
                Text.literal("Confirmar restauração").formatted(Formatting.DARK_GREEN)
        ));
        return Command.SINGLE_SUCCESS;
    }

    public static void restoreInventory(ServerPlayerEntity player) {
        var opt = DeathStorage.get(player.getUuid());
        if (opt.isEmpty()) {
            player.sendMessage(prefix().append(Text.literal("Nenhuma morte salva.").formatted(Formatting.RED)), false);
            return;
        }
        DeathRecord record = opt.get();
        if (!record.playerId.equals(player.getUuid())) {
            player.sendMessage(prefix().append(Text.literal("Você só pode recuperar o seu inventário.").formatted(Formatting.RED)), false);
            return;
        }
        if (record.claimed) {
            player.sendMessage(prefix().append(Text.literal("Este inventário já foi recuperado.").formatted(Formatting.RED)), false);
            return;
        }

        // Drop current items at feet so nothing is silently destroyed
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, true, false);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }

        record.applyTo(inv);
        record.claimed = true;
        inv.markDirty();
        player.currentScreenHandler.sendContentUpdates();

        player.sendMessage(
                prefix().append(Text.literal("Inventário da morte restaurado!").formatted(Formatting.GREEN, Formatting.BOLD)),
                false
        );
        player.sendMessage(
                prefix().append(Text.literal("Local da morte: ").formatted(Formatting.GRAY))
                        .append(Text.literal(formatPos(record.pos)).formatted(Formatting.YELLOW))
                        .append(Text.literal(" [" + shortDim(record.dimension.getValue().toString()) + "]")
                                .formatted(Formatting.DARK_AQUA)),
                false
        );
        LOGGER.info("Inventário restaurado para {} ({} itens)", record.playerName, record.countItems());
    }

    private static MutableText prefix() {
        return Text.literal("")
                .append(Text.literal("HaumeaDeath").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String shortDim(String dim) {
        if (dim.endsWith("overworld")) return "Overworld";
        if (dim.endsWith("the_nether")) return "Nether";
        if (dim.endsWith("the_end")) return "End";
        return dim;
    }
}
