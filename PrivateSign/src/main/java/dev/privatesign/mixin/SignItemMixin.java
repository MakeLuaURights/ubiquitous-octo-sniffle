package dev.privatesign.mixin;

import dev.privatesign.PrivateSign;
import dev.privatesign.SignConfig;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SignItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Server side: right after a player places any sign (standing, wall, hanging, every wood type) and just before the
 * vanilla text editor would be opened for them, the preset text is written onto the sign.
 */
@Mixin(SignItem.class)
public abstract class SignItemMixin {
    @Redirect(method = "postPlacement",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/AbstractSignBlock;openEditScreen(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/block/entity/SignBlockEntity;Z)V"))
    private void privatesign$fillThenOpen(AbstractSignBlock block, PlayerEntity player, SignBlockEntity sign, boolean front) {
        PrivateSign.fill(sign);
        if (!SignConfig.get().skipEditor) block.openEditScreen(player, sign, front);
    }
}
