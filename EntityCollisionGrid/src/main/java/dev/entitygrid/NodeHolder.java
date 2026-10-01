package dev.entitygrid;

import net.minecraft.entity.Entity;

/** Duck interface implemented by Entity (via mixin): stores the entity's grid node. */
public interface NodeHolder {
    BoxGrid.Node<Entity> entitygrid$getNode();
    void entitygrid$setNode(BoxGrid.Node<Entity> node);
}
