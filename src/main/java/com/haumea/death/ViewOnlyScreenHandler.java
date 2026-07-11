package com.haumea.death;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 6-row chest UI to preview death inventory. Slots are locked (view only).
 */
public final class ViewOnlyScreenHandler extends GenericContainerScreenHandler {
    public ViewOnlyScreenHandler(int syncId, PlayerInventory playerInventory, Inventory deathInv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, deathInv, 6);
        lockContainerSlots();
    }

    private void lockContainerSlots() {
        // Top 54 slots belong to the death inventory — replace with locked slots
        for (int i = 0; i < 54; i++) {
            Slot old = this.slots.get(i);
            this.slots.set(i, new LockedSlot(old.inventory, old.getIndex(), old.x, old.y));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Block all interaction with container slots; allow only player-inv cosmetic clicks that do nothing useful
        if (slotIndex >= 0 && slotIndex < 54) {
            return;
        }
        if (actionType == SlotActionType.THROW
                || actionType == SlotActionType.CLONE
                || actionType == SlotActionType.PICKUP_ALL
                || actionType == SlotActionType.SWAP) {
            return;
        }
        // Still block pulling from locked area via number keys etc.
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public static SimpleInventory fromRecord(DeathRecord record) {
        SimpleInventory inv = new SimpleInventory(54);
        // Layout: main hotbar+inv (36) then armor/offhand for visibility
        var stacks = record.decodeStacks();
        int mainIndex = 0;
        for (int i = 0; i < record.inventoryNbt.size(); i++) {
            var tag = record.inventoryNbt.getCompound(i);
            int slot = tag.getByte("Slot") & 0xFF;
            ItemStack stack = ItemStack.fromNbt(tag);
            if (stack.isEmpty()) continue;
            if (slot < 36) {
                inv.setStack(slot, stack.copy());
                mainIndex = Math.max(mainIndex, slot + 1);
            } else if (slot >= 100 && slot < 104) {
                // armor → row below main (slots 36-39)
                inv.setStack(36 + (slot - 100), stack.copy());
            } else if (slot >= 150) {
                inv.setStack(40, stack.copy());
            }
        }
        return inv;
    }

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
