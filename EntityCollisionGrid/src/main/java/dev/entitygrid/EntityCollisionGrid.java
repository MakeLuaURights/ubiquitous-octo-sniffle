package dev.entitygrid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityCollisionGrid implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("EntityCollisionGrid");

    @Override
    public void onInitialize() {
        GridConfig cfg = GridConfig.get();
        FabricLoader loader = FabricLoader.getInstance();
        for (String id : new String[]{"moonrise", "lithium", "c2me", "vmp", "canary"}) {
            if (loader.isModLoaded(id)) {
                LOG.warn("Mod '{}' is installed. It may optimise entity lookups/collisions as well, so the optimisation can be duplicated "
                        + "(no crash expected: this mod only falls back to vanilla logic when it can't answer a query).", id);
            }
        }
        LOG.info("Entity Collision Grid active (cell size {} blocks, debug log {})", cfg.cellSize, cfg.debugLog ? "on" : "off");
        if (cfg.debugLog) GridBenchmark.runAndLog();
    }
}
