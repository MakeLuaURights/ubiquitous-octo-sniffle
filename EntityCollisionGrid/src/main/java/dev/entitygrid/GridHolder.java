package dev.entitygrid;

/** Duck interface implemented by World (via mixin). */
public interface GridHolder {
    SpatialHashGrid entitygrid$getGrid(boolean create);
}
