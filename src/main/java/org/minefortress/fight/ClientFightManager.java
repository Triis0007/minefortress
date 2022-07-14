package org.minefortress.fight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.minefortress.entity.Colonist;
import org.minefortress.fortress.FortressClientManager;
import org.minefortress.network.ServerboundSelectFightTargetPacket;
import org.minefortress.network.helpers.FortressChannelNames;
import org.minefortress.network.helpers.FortressClientNetworkHelper;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientFightManager {

    private final ClientFightSelectionManager selectionManager;
    private final Supplier<FortressClientManager> fortressClientManagerSupplier;

    public ClientFightManager(Supplier<FortressClientManager> fortressClientManagerSupplier) {
         selectionManager = new ClientFightSelectionManager(fortressClientManagerSupplier);
        this.fortressClientManagerSupplier = fortressClientManagerSupplier;
    }

    public ClientFightSelectionManager getSelectionManager() {
        return selectionManager;
    }

    public void setTarget(HitResult hitResult) {
        if(hitResult instanceof BlockHitResult blockHitResult) {
            final var blockPos = blockHitResult.getBlockPos();
            final var mainHandStack = MinecraftClient.getInstance().player.getMainHandStack();
            final var packet = new ServerboundSelectFightTargetPacket(blockPos, mainHandStack.getItem().equals(Items.FLINT_AND_STEEL), blockHitResult);
            FortressClientNetworkHelper.send(FortressChannelNames.FORTRESS_SELECT_FIGHT_TARGET, packet);
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            final var entity = entityHitResult.getEntity();
            if(!(entity instanceof LivingEntity livingEntity)) return;
            if(entity instanceof Colonist col) {
                final var colonistFortressId = col.getFortressId();
                if(colonistFortressId != null && colonistFortressId.equals(fortressClientManagerSupplier.get().getId()))
                    return;
            }
            final var packet = new ServerboundSelectFightTargetPacket(livingEntity);
            FortressClientNetworkHelper.send(FortressChannelNames.FORTRESS_SELECT_FIGHT_TARGET, packet);
        }
    }

    public void setTarget(Entity entity) {
        if(!(entity instanceof LivingEntity livingEntity)) return;
        if(entity instanceof Colonist col) {
            final var colonistFortressId = col.getFortressId();
            if(colonistFortressId != null && colonistFortressId.equals(fortressClientManagerSupplier.get().getId()))
                return;
        }
        final var packet = new ServerboundSelectFightTargetPacket(livingEntity);
        FortressClientNetworkHelper.send(FortressChannelNames.FORTRESS_SELECT_FIGHT_TARGET, packet);
    }
}
