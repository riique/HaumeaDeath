package com.haumea.death;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a player's inventory and death location.
 */
public final class DeathRecord {
    public final UUID playerId;
    public final String playerName;
    public final BlockPos pos;
    public final RegistryKey<World> dimension;
    public final long timestampMs;
    /** Main inventory (36) + armor (4) + offhand (1) = 41 stacks, NBT-encoded for safety. */
    public final NbtList inventoryNbt;
    public boolean claimed;

    public DeathRecord(
            UUID playerId,
            String playerName,
            BlockPos pos,
            RegistryKey<World> dimension,
            NbtList inventoryNbt
    ) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.pos = pos.toImmutable();
        this.dimension = dimension;
        this.timestampMs = System.currentTimeMillis();
        this.inventoryNbt = inventoryNbt;
        this.claimed = false;
    }

    public List<ItemStack> decodeStacks() {
        List<ItemStack> stacks = new ArrayList<>(inventoryNbt.size());
        for (int i = 0; i < inventoryNbt.size(); i++) {
            NbtCompound tag = inventoryNbt.getCompound(i);
            stacks.add(ItemStack.fromNbt(tag));
        }
        return stacks;
    }

    public static NbtList encodeInventory(net.minecraft.entity.player.PlayerInventory inv) {
        NbtList list = new NbtList();
        // main (0-35), armor (36-39), offhand (40) — same order as PlayerInventory.writeNbt
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack stack = inv.main.get(i);
            NbtCompound tag = new NbtCompound();
            tag.putByte("Slot", (byte) i);
            if (!stack.isEmpty()) {
                stack.writeNbt(tag);
            }
            list.add(tag);
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            ItemStack stack = inv.armor.get(i);
            NbtCompound tag = new NbtCompound();
            tag.putByte("Slot", (byte) (100 + i));
            if (!stack.isEmpty()) {
                stack.writeNbt(tag);
            }
            list.add(tag);
        }
        for (int i = 0; i < inv.offHand.size(); i++) {
            ItemStack stack = inv.offHand.get(i);
            NbtCompound tag = new NbtCompound();
            tag.putByte("Slot", (byte) (150 + i));
            if (!stack.isEmpty()) {
                stack.writeNbt(tag);
            }
            list.add(tag);
        }
        return list;
    }

    public void applyTo(net.minecraft.entity.player.PlayerInventory inv) {
        inv.clear();
        for (int i = 0; i < inventoryNbt.size(); i++) {
            NbtCompound tag = inventoryNbt.getCompound(i);
            int slot = tag.getByte("Slot") & 0xFF;
            ItemStack stack = ItemStack.fromNbt(tag);
            if (stack.isEmpty()) continue;
            if (slot >= 0 && slot < inv.main.size()) {
                inv.main.set(slot, stack);
            } else if (slot >= 100 && slot < 100 + inv.armor.size()) {
                inv.armor.set(slot - 100, stack);
            } else if (slot >= 150 && slot < 150 + inv.offHand.size()) {
                inv.offHand.set(slot - 150, stack);
            }
        }
        inv.markDirty();
    }

    public int countItems() {
        int count = 0;
        for (ItemStack stack : decodeStacks()) {
            if (!stack.isEmpty()) count += stack.getCount();
        }
        return count;
    }
}
