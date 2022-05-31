package org.minefortress.professions;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.minefortress.entity.Colonist;
import org.minefortress.fortress.AbstractFortressManager;
import org.minefortress.fortress.FortressServerManager;
import org.minefortress.network.ClientboundProfessionSyncPacket;
import org.minefortress.network.helpers.FortressChannelNames;
import org.minefortress.network.helpers.FortressServerNetworkHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerProfessionManager extends ProfessionManager{

    private boolean needsUpdate = true;

    public ServerProfessionManager(Supplier<AbstractFortressManager> fortressManagerSupplier) {
        super(fortressManagerSupplier);
    }

    @Override
    public void increaseAmount(String professionId) {
        if(super.getFreeColonists() <= 0) return;
        final Profession profession = super.getProfession(professionId);
        if(profession == null) return;
        if(!super.isRequirementsFulfilled(profession, true)) return;

        profession.setAmount(profession.getAmount() + 1);
        scheduleSync();
    }

    @Override
    public void decreaseAmount(String professionId) {
        final Profession profession = super.getProfession(professionId);
        if(profession == null) return;
        if(profession.getAmount() <= 0) return;

        profession.setAmount(profession.getAmount() - 1);
        scheduleSync();
    }

    public void tick(ServerPlayerEntity player) {
        for(Profession prof : professions.values()) {
            if(prof.getAmount() > 0) {
                final boolean unlocked = this.isRequirementsFulfilled(prof);
                if(!unlocked) {
                    prof.setAmount(prof.getAmount() - 1);
                    this.scheduleSync();
                }
            }
        }

        tickRemoveFromProfession();
        if(needsUpdate) {
            ClientboundProfessionSyncPacket packet = new ClientboundProfessionSyncPacket(this.professions);
            FortressServerNetworkHelper.send(player, FortressChannelNames.FORTRESS_PROFESSION_SYNC, packet);
            needsUpdate = false;
        }
    }

    private void tickRemoveFromProfession() {
        for(Map.Entry<String, Profession> entry : professions.entrySet()) {
            final String professionId = entry.getKey();
            final Profession profession = entry.getValue();
            final List<Colonist> colonistsWithProfession = this.getColonistsWithProfession(professionId);
            final int redundantProfCount = colonistsWithProfession.size() - profession.getAmount();
            if(redundantProfCount <= 0) continue;

            final List<Colonist> colonistsToRemove = colonistsWithProfession.stream()
                    .limit(redundantProfCount)
                    .collect(Collectors.toList());
            colonistsToRemove.forEach(Colonist::resetProfession);
        }
    }

    public void scheduleSync() {
        needsUpdate = true;
    }

    public void writeToNbt(NbtCompound tag){
        professions.forEach((key, value) -> tag.put(key, value.toNbt()));
    }

    public void readFromNbt(NbtCompound tag){
        for(String key : tag.getKeys()){
            final Profession profession = super.getProfession(key);
            if(profession == null) continue;
            profession.readNbt(tag.getCompound(key));
            scheduleSync();
        }
    }

    public Optional<String> getProfessionsWithAvailablePlaces() {
        for(Map.Entry<String, Profession> entry : professions.entrySet()) {
            final String professionId = entry.getKey();
            final Profession profession = entry.getValue();
            if(profession.getAmount() > 0) {
                final long colonistsWithProfession = countColonistsWithProfession(professionId);
                if(colonistsWithProfession < profession.getAmount()) {
                    return Optional.of(professionId);
                }
            }
        }
        return Optional.empty();
    }

    private long countColonistsWithProfession(String professionId) {
        final FortressServerManager fortressServerManager = (FortressServerManager) super.fortressManagerSupplier.get();
        return fortressServerManager
                .getColonists()
                .stream()
                .filter(colonist -> colonist.getProfessionId().equals(professionId))
                .count();
    }

    private List<Colonist> getColonistsWithProfession(String professionId) {
        final FortressServerManager fortressServerManager = (FortressServerManager) super.fortressManagerSupplier.get();
        return fortressServerManager
                .getColonists()
                .stream()
                .filter(colonist -> colonist.getProfessionId().equals(professionId))
                .collect(Collectors.toList());
    }

}
