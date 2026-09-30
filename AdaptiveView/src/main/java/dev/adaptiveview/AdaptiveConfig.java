package dev.adaptiveview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config: config/adaptive_view.json. Edited in-game through the Sodium video settings. */
public final class AdaptiveConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AdaptiveConfig instance;

    public boolean enabled = true;
    public boolean showOverlay = false;

    /** Hard limits (chunks). The adaptive distance never goes below min / above max (your own setting always wins if outside). */
    public int minDistance = 4;
    public int maxDistance = 24;

    public boolean elytraEnabled = true;
    public int elytraBonus = 4;            // +chunks while gliding

    public boolean forestEnabled = true;
    public int forestPenalty = 3;          // -chunks in forests / jungles / taiga / dense leaves
    public int forestLeafPercent = 8;      // % of sampled blocks around you that must be leaves

    public boolean mountainEnabled = true;
    public int mountainPenalty = 3;        // -chunks in mountains / hills

    public boolean combatEnabled = true;
    public int combatPenalty = 4;          // -chunks after YOU hit something
    public int combatSeconds = 8;

    public boolean lowFpsEnabled = false;
    public int lowFpsThreshold = 30;
    public int lowFpsPenalty = 2;

    public int raiseDelay = 4;             // seconds a "better" state must last before render distance goes up
    public int lowerDelay = 1;             // seconds before it goes down
    public int stepSize = 1;               // chunks per change
    public int stepInterval = 2;           // seconds between changes

    // crash recovery: remembers the user's own value while an adapted one is applied
    public int recoverBase = 0;
    public int recoverApplied = 0;

    public static synchronized AdaptiveConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("adaptive_view.json");
    }

    private static AdaptiveConfig load() {
        Path p = path();
        AdaptiveConfig c = null;
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                c = GSON.fromJson(r, AdaptiveConfig.class);
            } catch (Exception e) {
                AdaptiveView.LOG.warn("Could not read config, using defaults", e);
            }
        }
        if (c == null) c = new AdaptiveConfig();
        c.sanitize();
        c.save();
        return c;
    }

    public void sanitize() {
        minDistance = clamp(minDistance, 2, 32);
        maxDistance = clamp(maxDistance, minDistance, 64);
        elytraBonus = clamp(elytraBonus, 0, 32);
        forestPenalty = clamp(forestPenalty, 0, 32);
        forestLeafPercent = clamp(forestLeafPercent, 1, 50);
        mountainPenalty = clamp(mountainPenalty, 0, 32);
        combatPenalty = clamp(combatPenalty, 0, 32);
        combatSeconds = clamp(combatSeconds, 1, 120);
        lowFpsThreshold = clamp(lowFpsThreshold, 5, 240);
        lowFpsPenalty = clamp(lowFpsPenalty, 0, 32);
        raiseDelay = clamp(raiseDelay, 0, 60);
        lowerDelay = clamp(lowerDelay, 0, 60);
        stepSize = clamp(stepSize, 1, 8);
        stepInterval = clamp(stepInterval, 1, 30);
    }

    public synchronized void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            AdaptiveView.LOG.warn("Could not write config", e);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
