package dev.entitygrid.mixin;

import dev.entitygrid.BoxGrid;
import dev.entitygrid.NodeHolder;
import dev.entitygrid.SpatialHashGrid;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the grid in sync when an entity's bounding box changes. {@code setBoundingBox} is the single place where
 * the box is assigned (movement, pose and size changes all go through it). No fluid/tag related methods are touched.
 */
@Mixin(value = Entity.class, priority = 1500)
public abstract class EntityMixin implements NodeHolder {
    @Unique private BoxGrid.Node<Entity> entitygrid$node;

    @Override public BoxGrid.Node<Entity> entitygrid$getNode() { return entitygrid$node; }
    @Override public void entitygrid$setNode(BoxGrid.Node<Entity> node) { this.entitygrid$node = node; }

    @Inject(method = "setBoundingBox", at = @At("TAIL"))
    private void entitygrid$boxChanged(Box box, CallbackInfo ci) {
        BoxGrid.Node<Entity> node = this.entitygrid$node;
        if (node != null && node.owner instanceof SpatialHashGrid grid) grid.update((Entity) (Object) this);
    }
}
