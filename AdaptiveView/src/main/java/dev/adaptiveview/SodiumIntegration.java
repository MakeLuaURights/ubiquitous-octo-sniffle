package dev.adaptiveview;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Adds an "Adaptive View" page to the Sodium video settings (same mechanism Iris uses). */
public class SodiumIntegration implements ConfigEntryPoint {
    private static final StorageEventHandler SAVE = () -> AdaptiveConfig.get().save();
    private ConfigBuilder b;

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        this.b = builder;
        AdaptiveConfig c = AdaptiveConfig.get();

        ModOptionsBuilder mod = builder.registerOwnModOptions().setName("Adaptive View");
        OptionPageBuilder page = builder.createOptionPage().setName(Text.translatable("adaptive_view.page"));

        page.addOptionGroup(group("general",
                bool("enabled", v -> c.enabled = v, () -> c.enabled, true),
                num("min_distance", 2, 32, 1, v -> c.minDistance = v, () -> c.minDistance, 4, chunks()),
                num("max_distance", 4, 64, 1, v -> c.maxDistance = v, () -> c.maxDistance, 24, chunks()),
                bool("show_overlay", v -> c.showOverlay = v, () -> c.showOverlay, false)));

        page.addOptionGroup(group("elytra",
                bool("elytra_enabled", v -> c.elytraEnabled = v, () -> c.elytraEnabled, true),
                num("elytra_bonus", 0, 16, 1, v -> c.elytraBonus = v, () -> c.elytraBonus, 4, plus())));

        page.addOptionGroup(group("forest",
                bool("forest_enabled", v -> c.forestEnabled = v, () -> c.forestEnabled, true),
                num("forest_penalty", 0, 16, 1, v -> c.forestPenalty = v, () -> c.forestPenalty, 3, minus()),
                num("forest_leaf_percent", 1, 30, 1, v -> c.forestLeafPercent = v, () -> c.forestLeafPercent, 8, v -> Text.literal(v + " %"))));

        page.addOptionGroup(group("mountain",
                bool("mountain_enabled", v -> c.mountainEnabled = v, () -> c.mountainEnabled, true),
                num("mountain_penalty", 0, 16, 1, v -> c.mountainPenalty = v, () -> c.mountainPenalty, 3, minus())));

        page.addOptionGroup(group("combat",
                bool("combat_enabled", v -> c.combatEnabled = v, () -> c.combatEnabled, true),
                num("combat_penalty", 0, 16, 1, v -> c.combatPenalty = v, () -> c.combatPenalty, 4, minus()),
                num("combat_seconds", 1, 60, 1, v -> c.combatSeconds = v, () -> c.combatSeconds, 8, secs())));

        page.addOptionGroup(group("low_fps",
                bool("low_fps_enabled", v -> c.lowFpsEnabled = v, () -> c.lowFpsEnabled, false),
                num("low_fps_threshold", 10, 120, 5, v -> c.lowFpsThreshold = v, () -> c.lowFpsThreshold, 30, v -> Text.literal(v + " FPS")),
                num("low_fps_penalty", 0, 16, 1, v -> c.lowFpsPenalty = v, () -> c.lowFpsPenalty, 2, minus())));

        page.addOptionGroup(group("timing",
                num("raise_delay", 0, 30, 1, v -> c.raiseDelay = v, () -> c.raiseDelay, 4, secs()),
                num("lower_delay", 0, 30, 1, v -> c.lowerDelay = v, () -> c.lowerDelay, 1, secs()),
                num("step_size", 1, 8, 1, v -> c.stepSize = v, () -> c.stepSize, 1, chunks()),
                num("step_interval", 1, 10, 1, v -> c.stepInterval = v, () -> c.stepInterval, 2, secs())));

        mod.addPage(page);
    }

    private OptionGroupBuilder group(String id, net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder... options) {
        OptionGroupBuilder g = b.createOptionGroup().setName(Text.translatable("adaptive_view.group." + id));
        for (var o : options) g.addOption(o);
        return g;
    }

    private BooleanOptionBuilder bool(String id, Consumer<Boolean> set, Supplier<Boolean> get, boolean def) {
        return b.createBooleanOption(Identifier.of("adaptive_view", id))
                .setName(Text.translatable("adaptive_view.option." + id))
                .setTooltip(Text.translatable("adaptive_view.option." + id + ".tooltip"))
                .setStorageHandler(SAVE)
                .setBinding(set, get)
                .setDefaultValue(def);
    }

    private IntegerOptionBuilder num(String id, int min, int max, int step, Consumer<Integer> set, Supplier<Integer> get,
                                     int def, net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter fmt) {
        return b.createIntegerOption(Identifier.of("adaptive_view", id))
                .setName(Text.translatable("adaptive_view.option." + id))
                .setTooltip(Text.translatable("adaptive_view.option." + id + ".tooltip"))
                .setStorageHandler(SAVE)
                .setImpact(OptionImpact.VARIES)
                .setRange(min, max, step)
                .setBinding(set, get)
                .setDefaultValue(def)
                .setValueFormatter(fmt);
    }

    private static net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter chunks() { return v -> Text.literal(v + " ch"); }
    private static net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter plus()   { return v -> Text.literal("+" + v + " ch"); }
    private static net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter minus()  { return v -> Text.literal("-" + v + " ch"); }
    private static net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter secs()   { return v -> Text.literal(v + " s"); }
}
