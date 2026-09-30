package com.maseffectsplus.mixin;

import com.maseffectsplus.combat.CombatTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void maseffectsplus$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        // Packet handlers run twice: first on the network thread (which then re-queues the packet), then on the
        // main thread. Only handle the main-thread pass, otherwise every pop / shield break is counted twice.
        if (!Minecraft.getInstance().isSameThread()) return;
        if (this.level == null) return;
        Entity entity = packet.getEntity(this.level);
        if (entity == null) return;

        byte status = packet.getEventId();
        if (status == EntityEvent.PROTECTED_FROM_DEATH) { // totem of undying
            CombatTracker.onTotemPop(entity);
        } else if (status == 30) { // shield disabled / broken
            CombatTracker.onShieldDisabled(entity.getId());
        } else if (status == EntityEvent.DEATH) {
            CombatTracker.onEntityDeath(entity);
        }
    }
}
