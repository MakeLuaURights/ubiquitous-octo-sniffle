package dev.adaptiveview;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Decides the render distance and applies it by changing the vanilla "render distance" option at runtime.
 * The user's own value is remembered as "base"; changing the slider yourself always updates the base.
 */
final class Controller {
    private static final int EVAL_EVERY = 10;       // ticks
    private static final int LEAF_SAMPLE_EVERY = 20; // ticks
    private static final int LEAF_SAMPLES = 48;

    private long ticks;
    private int base = -1;      // the user's own render distance
    private int current = -1;   // what we last applied
    private int desired = -1;

    private long pendingSince = -1;
    private int pendingDir = 0;
    private long lastStep = -100000;

    private long combatUntil = -1;
    private boolean leafForest;
    private String reasons = "";

    void onPlayerAttack() {
        AdaptiveConfig c = AdaptiveConfig.get();
        if (c.enabled && c.combatEnabled) combatUntil = ticks + c.combatSeconds * 20L;
    }

    /** After a crash the adapted value may have been saved as the user's setting: put the real one back. */
    void recoverFromCrash(MinecraftClient mc) {
        AdaptiveConfig c = AdaptiveConfig.get();
        if (c.recoverApplied > 0 && c.recoverBase > 0) {
            SimpleOption<Integer> opt = mc.options.getViewDistance();
            if (opt.getValue() == c.recoverApplied) opt.setValue(c.recoverBase);
            c.recoverApplied = 0;
            c.recoverBase = 0;
            c.save();
        }
    }

    void restore(MinecraftClient mc) {
        if (base > 0 && mc.options.getViewDistance().getValue() != base) {
            mc.options.getViewDistance().setValue(base);
        }
        clearRecovery();
        base = -1;
        current = -1;
        desired = -1;
        pendingSince = -1;
        pendingDir = 0;
    }

    String overlayText() {
        if (base < 0) return "Adaptive View: -";
        return "View: " + current + " (base " + base + ")" + (reasons.isEmpty() ? "" : "  " + reasons);
    }

    void tick(MinecraftClient mc) {
        ticks++;
        AdaptiveConfig c = AdaptiveConfig.get();

        if (mc.player == null || mc.world == null) {
            if (base > 0) restore(mc);
            return;
        }
        if (!c.enabled) {
            if (base > 0) restore(mc);
            return;
        }

        SimpleOption<Integer> opt = mc.options.getViewDistance();
        int cur = opt.getValue();
        if (base < 0 || cur != current) {          // first tick, or the user changed the slider
            base = cur;
            current = cur;
            desired = cur;
            pendingSince = -1;
            pendingDir = 0;
            clearRecovery();
        }
        if (mc.currentScreen != null) return;       // don't fight menus (incl. the video settings)

        if (ticks % LEAF_SAMPLE_EVERY == 0 && c.forestEnabled) sampleLeaves(mc, c);
        if (ticks % EVAL_EVERY != 0) return;

        desired = computeDesired(mc, c);

        if (desired == current) {
            pendingSince = -1;
            pendingDir = 0;
            return;
        }
        int dir = desired > current ? 1 : -1;
        if (dir != pendingDir) {
            pendingDir = dir;
            pendingSince = ticks;
        }
        long delay = (dir > 0 ? c.raiseDelay : c.lowerDelay) * 20L;
        if (ticks - pendingSince < delay) return;
        if (ticks - lastStep < c.stepInterval * 20L) return;

        int next = dir > 0 ? Math.min(current + c.stepSize, desired) : Math.max(current - c.stepSize, desired);
        apply(mc, opt, next, c);
    }

    private int computeDesired(MinecraftClient mc, AdaptiveConfig c) {
        boolean gliding = mc.player.isGliding();
        int delta = 0;
        StringBuilder r = new StringBuilder();

        if (c.elytraEnabled && gliding) {
            delta += c.elytraBonus;
            r.append("[elytra +").append(c.elytraBonus).append("] ");
        }
        // terrain penalties don't apply while gliding: you want to see far from the air
        if (!gliding) {
            RegistryEntry<Biome> biome = mc.world.getBiome(mc.player.getBlockPos());
            if (c.mountainEnabled && (biome.isIn(BiomeTags.IS_MOUNTAIN) || biome.isIn(BiomeTags.IS_HILL))) {
                delta -= c.mountainPenalty;
                r.append("[mountain -").append(c.mountainPenalty).append("] ");
            }
            if (c.forestEnabled && (leafForest || biome.isIn(BiomeTags.IS_FOREST)
                    || biome.isIn(BiomeTags.IS_JUNGLE) || biome.isIn(BiomeTags.IS_TAIGA))) {
                delta -= c.forestPenalty;
                r.append("[forest -").append(c.forestPenalty).append("] ");
            }
        }
        if (c.combatEnabled && ticks < combatUntil) {
            delta -= c.combatPenalty;
            r.append("[combat -").append(c.combatPenalty).append("] ");
        }
        if (c.lowFpsEnabled && mc.getCurrentFps() < c.lowFpsThreshold) {
            delta -= c.lowFpsPenalty;
            r.append("[fps -").append(c.lowFpsPenalty).append("] ");
        }
        reasons = r.toString().trim();

        // limits never override the user's own value in the "wrong" direction
        int lo = Math.min(c.minDistance, base);
        int hi = Math.max(c.maxDistance, base);
        return Math.max(lo, Math.min(hi, base + delta));
    }

    private void apply(MinecraftClient mc, SimpleOption<Integer> opt, int value, AdaptiveConfig c) {
        opt.setValue(value);
        current = value;
        lastStep = ticks;
        if (value != base) {
            c.recoverBase = base;
            c.recoverApplied = value;
            c.save();
        } else {
            clearRecovery();
        }
        // multiplayer: tell the server about the new distance (the integrated server follows the option by itself)
        if (mc.getNetworkHandler() != null && !mc.isInSingleplayer()) mc.options.sendClientSettings();
    }

    private void clearRecovery() {
        AdaptiveConfig c = AdaptiveConfig.get();
        if (c.recoverApplied != 0 || c.recoverBase != 0) {
            c.recoverApplied = 0;
            c.recoverBase = 0;
            c.save();
        }
    }

    /** Cheap "am I inside dense leaves?" check: random blocks in a box around the player. */
    private void sampleLeaves(MinecraftClient mc, AdaptiveConfig c) {
        ClientWorld w = mc.world;
        BlockPos p = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int hits = 0;
        for (int i = 0; i < LEAF_SAMPLES; i++) {
            m.set(p.getX() + rnd.nextInt(17) - 8, p.getY() + rnd.nextInt(14) - 2, p.getZ() + rnd.nextInt(17) - 8);
            BlockState s = w.getBlockState(m);
            if (s.isIn(BlockTags.LEAVES)) hits++;
        }
        leafForest = hits * 100 >= c.forestLeafPercent * LEAF_SAMPLES;
    }
}
