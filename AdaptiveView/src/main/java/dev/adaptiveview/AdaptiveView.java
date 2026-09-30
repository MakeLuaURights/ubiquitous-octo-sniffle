package dev.adaptiveview;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveView implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("AdaptiveView");
    static final Controller CONTROLLER = new Controller();

    @Override
    public void onInitializeClient() {
        AdaptiveConfig.get();

        ClientLifecycleEvents.CLIENT_STARTED.register(CONTROLLER::recoverFromCrash);
        ClientTickEvents.END_CLIENT_TICK.register(CONTROLLER::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            CONTROLLER.restore(mc);
            mc.options.write();
        });

        // Only hits made by the local player count (not hits on the player).
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) CONTROLLER.onPlayerAttack();
            return ActionResult.PASS;
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("adaptive_view", "overlay"), (ctx, tick) -> {
                    AdaptiveConfig c = AdaptiveConfig.get();
                    if (!c.enabled || !c.showOverlay) return;
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options.hudHidden || mc.player == null) return;
                    ctx.drawTextWithShadow(mc.textRenderer, CONTROLLER.overlayText(), 4, 4, 0xFFFFFFFF);
                });

        LOG.info("Adaptive View loaded");
    }
}
