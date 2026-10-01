package dev.entitygrid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/entity_collision_grid.json (also editable in the Sodium video settings). */
public final class GridConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile GridConfig instance;

    /** Use the grid for entity queries (the grid itself is always kept up to date). */
    public boolean enabled = true;
    /** Grid cell size in blocks (1-8). */
    public int cellSize = 2;
    /** Periodically log how many entity checks the grid saved. */
    public boolean debugLog = false;
    /** Debug: also run the vanilla query and compare results (slow! only for testing). */
    public boolean debugVerify = false;
    public int debugIntervalSeconds = 15;
    /** Query boxes covering more cells than this fall back to the vanilla lookup. */
    public int maxQueryCells = 512;

    public static GridConfig get() {
        GridConfig c = instance;
        if (c == null) {
            synchronized (GridConfig.class) {
                if (instance == null) instance = load();
                c = instance;
            }
        }
        return c;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("entity_collision_grid.json");
    }

    private static GridConfig load() {
        GridConfig c = null;
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                c = GSON.fromJson(r, GridConfig.class);
            } catch (Exception e) {
                EntityCollisionGrid.LOG.warn("Could not read config, using defaults", e);
            }
        }
        if (c == null) c = new GridConfig();
        c.sanitize();
        c.save();
        return c;
    }

    public void sanitize() {
        cellSize = Math.max(1, Math.min(8, cellSize));
        debugIntervalSeconds = Math.max(2, Math.min(600, debugIntervalSeconds));
        maxQueryCells = Math.max(8, Math.min(100000, maxQueryCells));
    }

    public synchronized void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            EntityCollisionGrid.LOG.warn("Could not write config", e);
        }
    }
}
