package com.haumea.death;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Small confirmation UI: green = restore death inventory, red = cancel.
 */
public final class ConfirmRestoreScreenHandler extends GenericContainerScreenHandler {
    public static final int CONFIRM_SLOT = 3;
    public static final int CANCEL_SLOT = 5;

    private final UUIDOwner owner;

    public ConfirmRestoreScreenHandler(int syncId, PlayerInventory playerInventory, UUIDOwner owner) {
        super(ScreenHandlerType.GENERIC_9X1, syncId, playerInventory, buildInventory(), 1);
        this.owner = owner;
        lockAllTopSlots();
    }

    private void lockAllTopSlots() {
        for (int i = 0; i < 9; i++) {
            Slot old = this.slots.get(i);
            this.slots.set(i, new LockedSlot(old.inventory, old.getIndex(), old.x, old.y));
        }
    }

    private static Inventory buildInventory() {
        SimpleInventory inv = new SimpleInventory(9);
        inv.setStack(CONFIRM_SLOT, confirmItem());
        inv.setStack(CANCEL_SLOT, cancelItem());
        // fillers
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.setCustomName(Text.literal(" ").formatted(Formatting.DARK_GRAY));
        for (int i = 0; i < 9; i++) {
            if (i != CONFIRM_SLOT && i != CANCEL_SLOT) {
                inv.setStack(i, glass.copy());
            }
        }
        return inv;
    }

    private static ItemStack confirmItem() {
        ItemStack stack = new ItemStack(Items.LIME_CONCRETE);
        stack.setCustomName(Text.literal("Confirmar restauração").formatted(Formatting.GREEN, Formatting.BOLD));
        NbtCompound display = stack.getOrCreateSubNbt("display");
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serializer.toJson(
                Text.literal("Devolve o inventário da última morte.").formatted(Formatting.GRAY))));
        lore.add(NbtString.of(Text.Serializer.toJson(
                Text.literal("Só você pode fazer isso.").formatted(Formatting.DARK_GRAY))));
        display.put("Lore", lore);
        return stack;
    }

    private static ItemStack cancelItem() {
        ItemStack stack = new ItemStack(Items.RED_CONCRETE);
        stack.setCustomName(Text.literal("Cancelar").formatted(Formatting.RED, Formatting.BOLD));
        return stack;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (slotIndex == CONFIRM_SLOT) {
            serverPlayer.closeHandledScreen();
            HaumeaDeathMod.restoreInventory(serverPlayer);
            return;
        }
        if (slotIndex == CANCEL_SLOT) {
            serverPlayer.closeHandledScreen();
            serverPlayer.sendMessage(
                    Text.literal("HaumeaDeath").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                            .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                            .append(Text.literal("Restauração cancelada.").formatted(Formatting.GRAY)),
                    false
            );
            return;
        }
        // ignore everything else
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return owner != null && owner.playerId().equals(player.getUuid());
    }

    public record UUIDOwner(java.util.UUID playerId) {}

    private static final class LockedSlot extends Slot {
        LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}
