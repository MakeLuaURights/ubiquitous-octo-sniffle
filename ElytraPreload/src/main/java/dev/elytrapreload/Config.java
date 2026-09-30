package dev.elytrapreload;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties-file config: config/elytra_preload.properties (created on first start). */
public final class Config {
    public boolean enabled = true;
    /** How many seconds of flight ahead to keep loaded. */
    public double lookaheadSeconds = 6.0;
    /** Max number of chunks beyond the server view distance to preload. */
    public int maxExtraChunks = 8;
    /** Chunks to each side of the flight line (1 = 3 chunks wide). */
    public int lateralWidth = 1;
    /** Horizontal speed (blocks/tick) above which non-gliding players are also preloaded. 0.6 b/t = 12 b/s. */
    public double minSpeed = 0.6;
    /** If true, only elytra gliding triggers preloading. */
    public boolean onlyWhenGliding = false;
    /** Safety cap of chunk requests per player per update. */
    public int maxRequestsPerUpdate = 40;
    /** Server ticks between updates. */
    public int updateInterval = 5;
    /** How long an unrefreshed request lives (ticks). */
    public int ticketLifetime = 100;
    /** Stop requesting new chunks while the average server tick is slower than this (ms). */
    public double pauseAboveTickMs = 40.0;

    public static Config load() {
        Config c = new Config();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("elytra_preload.properties");
        Properties p = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            } catch (IOException e) {
                ElytraPreload.LOG.warn("Could not read config, using defaults", e);
            }
        }
        c.enabled = bool(p, "enabled", c.enabled);
        c.lookaheadSeconds = clamp(num(p, "lookaheadSeconds", c.lookaheadSeconds), 1, 30);
        c.maxExtraChunks = (int) clamp(num(p, "maxExtraChunks", c.maxExtraChunks), 1, 32);
        c.lateralWidth = (int) clamp(num(p, "lateralWidth", c.lateralWidth), 0, 4);
        c.minSpeed = clamp(num(p, "minSpeed", c.minSpeed), 0.1, 10);
        c.onlyWhenGliding = bool(p, "onlyWhenGliding", c.onlyWhenGliding);
        c.maxRequestsPerUpdate = (int) clamp(num(p, "maxRequestsPerUpdate", c.maxRequestsPerUpdate), 4, 400);
        c.updateInterval = (int) clamp(num(p, "updateInterval", c.updateInterval), 1, 40);
        c.ticketLifetime = (int) clamp(num(p, "ticketLifetime", c.ticketLifetime), 20, 600);
        c.pauseAboveTickMs = clamp(num(p, "pauseAboveTickMs", c.pauseAboveTickMs), 10, 200);
        c.save(path);
        return c;
    }

    private void save(Path path) {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("lookaheadSeconds", String.valueOf(lookaheadSeconds));
        p.setProperty("maxExtraChunks", String.valueOf(maxExtraChunks));
        p.setProperty("lateralWidth", String.valueOf(lateralWidth));
        p.setProperty("minSpeed", String.valueOf(minSpeed));
        p.setProperty("onlyWhenGliding", String.valueOf(onlyWhenGliding));
        p.setProperty("maxRequestsPerUpdate", String.valueOf(maxRequestsPerUpdate));
        p.setProperty("updateInterval", String.valueOf(updateInterval));
        p.setProperty("ticketLifetime", String.valueOf(ticketLifetime));
        p.setProperty("pauseAboveTickMs", String.valueOf(pauseAboveTickMs));
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(out, "Elytra Preload - lower lookaheadSeconds/maxExtraChunks/lateralWidth on weak devices");
        } catch (IOException e) {
            ElytraPreload.LOG.warn("Could not write config", e);
        }
    }

    private static boolean bool(Properties p, String k, boolean d) {
        String v = p.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v.trim());
    }

    private static double num(Properties p, String k, double d) {
        try {
            String v = p.getProperty(k);
            return v == null ? d : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return d;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
