package dev.entitygrid.mixin;

import dev.entitygrid.GridHolder;
import dev.entitygrid.SpatialHashGrid;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors the membership of the world's entity sections into the per-world grid. */
@Mixin(value = EntityTrackingSection.class, priority = 1500)
public abstract class EntityTrackingSectionMixin {
    @Inject(method = "add", at = @At("HEAD"))
    private void entitygrid$add(EntityLike entityLike, CallbackInfo ci) {
        if (entityLike instanceof Entity entity && entity.getEntityWorld() instanceof GridHolder holder) {
            holder.entitygrid$getGrid(true).insert(entity);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void entitygrid$remove(EntityLike entityLike, CallbackInfoReturnable<Boolean> cir) {
        if (entityLike instanceof Entity entity && entity.getEntityWorld() instanceof GridHolder) {
            // the node knows which grid it lives in (the entity may already point at another world)
            var node = ((dev.entitygrid.NodeHolder) entity).entitygrid$getNode();
            if (node != null && node.owner instanceof SpatialHashGrid grid) grid.remove(entity);
        }
    }
}
