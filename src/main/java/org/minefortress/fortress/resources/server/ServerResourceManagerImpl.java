package org.minefortress.fortress.resources.server;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import org.minefortress.fortress.resources.ItemInfo;
import org.minefortress.fortress.resources.client.FortressItemStack;
import org.minefortress.network.ClientboundSyncItemsPacket;
import org.minefortress.network.helpers.FortressChannelNames;
import org.minefortress.network.helpers.FortressServerNetworkHelper;

import java.util.*;

public class ServerResourceManagerImpl implements ServerResourceManager {

    private final Synchronizer synchronizer = new Synchronizer();

    private final ItemStacksManager resources = new ItemStacksManager();
    private final Map<UUID, ItemStacksManager> reservedResources = new HashMap<>();

    public ServerResourceManagerImpl() {
        resources.getStack(Items.OAK_PLANKS).increaseBy(12);
        this.syncAll();
    }

    @Override
    public void increaseItemAmount(Item item, int amount) {
        final var stack = resources.getStack(item);
        stack.increaseBy(amount);

        synchronizer.syncItem(item, stack.getAmount());
    }

    @Override
    public void reserveItems(UUID taskId, List<ItemInfo> infos) {
        if(!hasItems(infos)) throw new IllegalStateException("Not enough resources");

        final var manager = this.getManagerFromTaskId(taskId);
        final var infosToSync = new ArrayList<ItemInfo>();
        for(ItemInfo info : infos) {
            final var item = info.item();
            final var amount = info.amount();
            final var stack = resources.getStack(item);
            final var reservedStack = manager.getStack(item);

            stack.decreaseBy(amount);
            reservedStack.increaseBy(amount);

            infosToSync.add(new ItemInfo(item, stack.getAmount()));
        }

        synchronizer.syncAll(infosToSync);
    }

    @Override
    public void removeReservedItem(UUID taskId, Item item) {
        if(!(item instanceof BlockItem)) return;
        if(!hasReservedItem(taskId, item)) throw new IllegalStateException("Item not reserved " + item.getName().asString());
        this.getManagerFromTaskId(taskId).getStack(item).decrease();
    }

    @Override
    public void returnReservedItems(UUID taskId) {
        if(!reservedResources.containsKey(taskId)) return;
        final var manager = this.getManagerFromTaskId(taskId);

        final var infosToSync = new ArrayList<ItemInfo>();
        for(ItemInfo info: manager.getAll()) {
            final var item = info.item();
            final var stack = this.resources.getStack(item);
            final var amount = info.amount();
            stack.increaseBy(amount);

            final var infoToSync = new ItemInfo(item, stack.getAmount());
            infosToSync.add(infoToSync);
        }

        synchronizer.syncAll(infosToSync);
        this.reservedResources.remove(taskId);
    }

    @Override
    public void write(NbtCompound tag) {
        final var stacks = new NbtList();
        for(ItemInfo info : resources.getAll()) {
            final var item = info.item();
            final var amount = info.amount();

            final var stack = new NbtCompound();
            final var rawId = Item.getRawId(item);
            stack.putInt("id", rawId);
            stack.putInt("amount", amount);

            stacks.add(stack);
        }

        tag.put("resources", stacks);
//        this.resources.clear();
    }

    @Override
    public void read(NbtCompound tag) {
        if(tag.contains("resources")) {
            this.resources.clear();
            final var resourcesTags = tag.getList("resources", NbtList.COMPOUND_TYPE);
            final var size = resourcesTags.size();
            for(int i = 0; i < size; i++) {
                final var resourceTag = resourcesTags.getCompound(i);
                final var id = resourceTag.getInt("id");
                final var amount = resourceTag.getInt("amount");
                final var item = Item.byRawId(id);

                this.resources.getStack(item).increaseBy(amount);
            }
            this.syncAll();
        }
    }

    @Override
    public void tick(ServerPlayerEntity player) {
        synchronizer.sync(player);
    }

    @Override
    public List<ItemStack> getAllItems() {
        return resources.getAll().stream()
                .filter(info -> info.amount() > 0)
                .map(it -> (ItemStack)new FortressItemStack(it.item(), it.amount()))
                .toList();
    }

    private boolean hasItems(List<ItemInfo> infos) {
        for(ItemInfo info : infos) {
            final var item = info.item();
            if(item == Items.FLINT_AND_STEEL || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET) continue;
            final var amount = info.amount();
            final var stack = resources.getStack(item);
            if(!stack.hasEnough(amount)) return false;
        }

        return true;
    }

    private boolean hasReservedItem(UUID taskId, Item item) {
        final var manager = this.getManagerFromTaskId(taskId);
        return manager.getStack(item).hasEnough(1);
    }
    
    private ItemStacksManager getManagerFromTaskId(UUID taskId) {
        return reservedResources.computeIfAbsent(taskId, k -> new ItemStacksManager());
    }

    private void syncAll() {
        this.synchronizer.reset();
        this.synchronizer.syncAll(resources.getAll());
    }

    private static class Synchronizer {

        private final List<ItemInfo> infosToSync = new ArrayList<>();
        private boolean needReset = false;

        void reset() {
            this.infosToSync.clear();
            this.needReset = true;
        }

        void sync(ServerPlayerEntity player) {
            if(infosToSync.isEmpty() && !needReset) return;
            final var packet = new ClientboundSyncItemsPacket(infosToSync, needReset);
            FortressServerNetworkHelper.send(player, FortressChannelNames.FORTRESS_RESOURCES_SYNC, packet);
            infosToSync.clear();
            this.needReset = false;
        }

        void syncItem(Item item, int amount) {
            infosToSync.add(new ItemInfo(item, amount));
        }

        void syncAll(List<ItemInfo> items) {
            infosToSync.addAll(items);
        }

    }

}
