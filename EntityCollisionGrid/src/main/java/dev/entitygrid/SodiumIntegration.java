package dev.entitygrid;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Adds an "Entity Collision Grid" page to the Sodium video settings (Sodium 0.8+ config API, same as Iris uses). */
public class SodiumIntegration implements ConfigEntryPoint {
    // changes are applied lazily by the grids (they compare their cell size with the config), so saving is enough
    private static final StorageEventHandler SAVE = () -> GridConfig.get().save();

    @Override
    public void registerConfigLate(ConfigBuilder b) {
        GridConfig c = GridConfig.get();
        ModOptionsBuilder mod = b.registerOwnModOptions().setName("Entity Collision Grid");
        OptionPageBuilder page = b.createOptionPage().setName(Text.translatable("entity_collision_grid.page"));

        OptionGroupBuilder general = b.createOptionGroup().setName(Text.translatable("entity_collision_grid.group.general"));
        general.addOption(b.createBooleanOption(id("enabled"))
                .setName(Text.translatable("entity_collision_grid.option.enabled"))
                .setTooltip(Text.translatable("entity_collision_grid.option.enabled.tooltip"))
                .setStorageHandler(SAVE)
                .setBinding(v -> c.enabled = v, () -> c.enabled)
                .setDefaultValue(true));
        general.addOption(b.createIntegerOption(id("cell_size"))
                .setName(Text.translatable("entity_collision_grid.option.cell_size"))
                .setTooltip(Text.translatable("entity_collision_grid.option.cell_size.tooltip"))
                .setStorageHandler(SAVE)
                .setImpact(OptionImpact.LOW)
                .setRange(1, 8, 1)
                .setBinding(v -> c.cellSize = v, () -> c.cellSize)
                .setDefaultValue(2)
                .setValueFormatter(v -> Text.literal(v + (v == 1 ? " block" : " blocks"))));

        OptionGroupBuilder debug = b.createOptionGroup().setName(Text.translatable("entity_collision_grid.group.debug"));
        debug.addOption(b.createBooleanOption(id("debug_log"))
                .setName(Text.translatable("entity_collision_grid.option.debug_log"))
                .setTooltip(Text.translatable("entity_collision_grid.option.debug_log.tooltip"))
                .setStorageHandler(SAVE)
                .setBinding(v -> c.debugLog = v, () -> c.debugLog)
                .setDefaultValue(false));
        debug.addOption(b.createBooleanOption(id("debug_verify"))
                .setName(Text.translatable("entity_collision_grid.option.debug_verify"))
                .setTooltip(Text.translatable("entity_collision_grid.option.debug_verify.tooltip"))
                .setStorageHandler(SAVE)
                .setImpact(OptionImpact.HIGH)
                .setBinding(v -> c.debugVerify = v, () -> c.debugVerify)
                .setDefaultValue(false));

        page.addOptionGroup(general);
        page.addOptionGroup(debug);
        mod.addPage(page);
    }

    private static Identifier id(String path) { return Identifier.of("entity_collision_grid", path); }
}
