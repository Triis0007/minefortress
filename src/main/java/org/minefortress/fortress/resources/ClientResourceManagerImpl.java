package org.minefortress.fortress.resources;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import java.util.*;

public class ClientResourceManagerImpl implements ClientResourceManager {

    private final Map<ItemGroup, Map<Item, ItemStack>> resources = new HashMap<>();

    @Override
    public Set<ItemGroup> getGroups() {
        return resources.keySet();
    }

    @Override
    public boolean hasStacks(List<ItemStack> stacks) {
        final var count = resources.values()
                .stream()
                .flatMap(it -> it.values().stream())
                .filter(it -> !it.isEmpty())
                .filter(stacks::contains)
                .count();
        return count == stacks.size();
    }

    @Override
    public List<ItemStack> getStacks(ItemGroup group) {
        return new ArrayList<>(getStacksForGroup(group).values());
    }

    @Override
    public void setItemAmount(Item item, int amount) {
        new ItemStack(item, amount);
        for (ItemGroup group : ItemGroup.GROUPS) {
            if(item.isIn(group)) {
                final var stacksForGroup = getStacksForGroup(group);
                if(amount > 0) {
                    stacksForGroup.put(item, new ItemStack(item, amount));
                } else {
                    stacksForGroup.remove(item);
                }

                if(stacksForGroup.isEmpty()) {
                    resources.remove(group);
                }
                return;
            }
        }
    }

    private Map<Item, ItemStack> getStacksForGroup(ItemGroup group) {
        return resources.computeIfAbsent(group, k -> new HashMap<>());
    }

}
