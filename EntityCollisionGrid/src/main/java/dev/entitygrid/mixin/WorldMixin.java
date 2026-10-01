package dev.entitygrid.mixin;

import dev.entitygrid.GridConfig;
import dev.entitygrid.GridHolder;
import dev.entitygrid.SpatialHashGrid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.util.math.Box;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Replaces the entity scan of {@code World.getOtherEntities(Entity, Box, Predicate)} (used by entity collisions,
 * pushing and cramming) with a spatial hash grid lookup. Priority is intentionally not 1000.
 */
@Mixin(value = World.class, priority = 1500)
public abstract class WorldMixin implements GridHolder {
    @Unique private SpatialHashGrid entitygrid$grid;

    @Shadow protected abstract EntityLookup<Entity> getEntityLookup();
    @Shadow public abstract Collection<EnderDragonPart> getEnderDragonParts();

    @Override
    public SpatialHashGrid entitygrid$getGrid(boolean create) {
        if (entitygrid$grid == null && create) entitygrid$grid = new SpatialHashGrid((World) (Object) this);
        return entitygrid$grid;
    }

    @Inject(method = "getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private void entitygrid$getOtherEntities(Entity except, Box box, Predicate<? super Entity> predicate,
                                             CallbackInfoReturnable<List<Entity>> cir) {
        SpatialHashGrid grid = this.entitygrid$grid;
        if (grid == null || !GridConfig.get().enabled) return;

        Profilers.get().visit("getEntities");
        List<Entity> list = grid.getOtherEntities(this.getEntityLookup(), except, box, predicate);
        if (list == null) return;   // let vanilla handle it

        for (EnderDragonPart part : this.getEnderDragonParts()) {
            if (part != except && part.owner != except && predicate.test(part) && box.intersects(part.getBoundingBox())) {
                list.add(part);
            }
        }
        cir.setReturnValue(list);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void entitygrid$close(CallbackInfo ci) {
        if (entitygrid$grid != null) {
            entitygrid$grid.clear();
            entitygrid$grid = null;
        }
    }
}
