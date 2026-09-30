package dev.elytrapreload;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps chunks along a fast-moving player's flight path loaded / generated ahead of time.
 * Runs on the logical server, so it works in singleplayer (integrated server) and on any
 * server that has the mod installed. Clients do not need it for multiplayer.
 */
public class ElytraPreload implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("ElytraPreload");

    static Config config;
    static ChunkTicketType ticketType;

    private final Map<UUID, Track> tracks = new HashMap<>();

    private static final class Track {
        ServerWorld world;
        double x, z;
        int tick;
        double vx, vz;   // smoothed horizontal velocity, blocks/tick
    }

    @Override
    public void onInitialize() {
        config = Config.load();
        if (!config.enabled) {
            LOG.info("Elytra Preload is disabled in config");
            return;
        }
        // Load-only ticket: chunks are loaded/generated to FULL but not simulated, and unrefreshed
        // requests expire on their own (also before loading if the player has already turned away).
        ticketType = new ChunkTicketType(config.ticketLifetime,
                ChunkTicketType.FOR_LOADING | ChunkTicketType.CAN_EXPIRE_BEFORE_LOAD);

        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        LOG.info("Elytra Preload active (lookahead {}s, up to {} extra chunks)", config.lookaheadSeconds, config.maxExtraChunks);
    }

    private void onTick(MinecraftServer server) {
        int now = server.getTicks();
        if (now % config.updateInterval != 0) return;
        if (server.getAverageTickTime() > config.pauseAboveTickMs) return;

        int viewDist = server.getPlayerManager().getViewDistance();
        if (tracks.size() > server.getPlayerManager().getPlayerList().size() + 8) {
            tracks.keySet().removeIf(id -> server.getPlayerManager().getPlayer(id) == null);
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getEntityWorld();
            Track t = tracks.computeIfAbsent(player.getUuid(), id -> new Track());
            double x = player.getX(), z = player.getZ();

            if (t.world != world || now - t.tick <= 0) {
                t.world = world; t.x = x; t.z = z; t.tick = now; t.vx = 0; t.vz = 0;
                continue;
            }
            double dt = now - t.tick;
            double vx = (x - t.x) / dt, vz = (z - t.z) / dt;
            t.x = x; t.z = z; t.tick = now;
            if (Math.hypot(vx, vz) > 8.0) {          // teleport, not flight
                t.vx = 0; t.vz = 0;
                continue;
            }
            t.vx = t.vx * 0.5 + vx * 0.5;
            t.vz = t.vz * 0.5 + vz * 0.5;

            double speed = Math.hypot(t.vx, t.vz);
            boolean trigger = player.isGliding() || (!config.onlyWhenGliding && speed >= config.minSpeed);
            if (!trigger || speed < 0.15) continue;

            requestAhead(world, x, z, t.vx, t.vz, viewDist);
        }
    }

    /** Requests chunks along (dx, dz) starting at the edge of the view distance. Returns requests made. */
    static int requestAhead(ServerWorld world, double x, double z, double dx, double dz, int viewDist) {
        double speed = Math.hypot(dx, dz);
        if (speed < 1e-6) return 0;
        double ux = dx / speed, uz = dz / speed;

        double aheadBlocks = speed * 20.0 * config.lookaheadSeconds;
        int extra = Math.max(1, Math.min(config.maxExtraChunks, (int) Math.ceil(aheadBlocks / 16.0)));
        double startD = Math.max(1, viewDist - 1);
        double endD = viewDist + extra;

        ServerChunkManager cm = world.getChunkManager();
        LongOpenHashSet seen = new LongOpenHashSet();
        double px = x / 16.0, pz = z / 16.0;
        int budget = config.maxRequestsPerUpdate;
        int w = config.lateralWidth;

        outer:
        for (double d = startD; d <= endD; d += 0.5) {
            for (int l = -w; l <= w; l++) {
                int cx = (int) Math.floor(px + ux * d - uz * l);
                int cz = (int) Math.floor(pz + uz * d + ux * l);
                if (!seen.add(ChunkPos.toLong(cx, cz))) continue;
                cm.addTicket(ticketType, new ChunkPos(cx, cz), 0);
                if (--budget <= 0) break outer;
            }
        }
        return config.maxRequestsPerUpdate - budget;
    }
}
