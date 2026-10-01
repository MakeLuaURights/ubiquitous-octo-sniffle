package dev.entitygrid.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleEntityLookup.class)
public interface SimpleEntityLookupAccessor {
    @Accessor("cache")
    <T extends net.minecraft.world.entity.EntityLike> SectionedEntityCache<T> entitygrid$getCache();
}
