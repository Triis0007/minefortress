package org.minefortress.mixins.entity.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.util.collection.DefaultedList;
import org.minefortress.interfaces.FortressMinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class FortressPlayerInventoryMixin {

    @Shadow @Final public DefaultedList<ItemStack> main;

    @Inject(method = "populateRecipeFinder", at = @At("HEAD"), cancellable = true)
    void populateFinder(RecipeMatcher finder, CallbackInfo ci) {
        final var fortressClient = getFortressClient();
        final var fortressClientManager = fortressClient.getFortressClientManager();
        if(fortressClient.isFortressGamemode() && fortressClientManager.isSurvival()) {
            final var resourceManager = fortressClientManager.getResourceManager();
            final var allStacks = resourceManager.getAllStacks();
            finder.clear();
            allStacks.forEach(it -> finder.addInput(it, Integer.MAX_VALUE));
            ci.cancel();
        }
    }

    private FortressMinecraftClient getFortressClient() {
        return (FortressMinecraftClient) MinecraftClient.getInstance();
    }

}
