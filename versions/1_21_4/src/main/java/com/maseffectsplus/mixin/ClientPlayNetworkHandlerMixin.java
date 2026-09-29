package com.maseffectsplus.mixin;

import com.maseffectsplus.combat.CombatTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatusHandle(EntityStatusS2CPacket packet, CallbackInfo ci) {
        // Packet handlers run twice: first on the network thread (which then re-queues the packet), then on the
        // main thread. Only handle the main-thread pass, otherwise every pop / shield break is counted twice.
        if (!net.minecraft.client.MinecraftClient.getInstance().isOnThread()) return;
        if (this.world == null) return;
        Entity entity = packet.getEntity(this.world);
        if (entity == null) return;

        byte status = packet.getStatus();
        if (status == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            CombatTracker.onTotemPop(entity);
        } else if (status == 30) { // 30 is shield disable / break in vanilla
            CombatTracker.onShieldDisabled(entity.getId());
        } else if (status == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) {
            CombatTracker.onEntityDeath(entity);
        }
    }
}
