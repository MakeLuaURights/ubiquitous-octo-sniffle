package dev.entitygrid;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import dev.entitygrid.mixin.SimpleEntityLookupAccessor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * Per-world spatial hash grid for entities. It mirrors the contents of the world's entity sections
 * (insert/remove are driven by {@code EntityTrackingSection.add/remove}, box changes by {@code Entity.setBoundingBox})
 * and answers {@code World.getOtherEntities} queries with the same result set as vanilla.
 */
public final class SpatialHashGrid {
    private final World world;
    private final BoxGrid<Entity> grid;
    private final Thread owner = Thread.currentThread();
    private final Long2IntOpenHashMap sectionCounts = new Long2IntOpenHashMap();
    private final ArrayList<BoxGrid.Node<Entity>> scratch = new ArrayList<>();
    private boolean busy;
    private volatile boolean poisoned;

    // debug counters
    private long queries, gridChecks, vanillaChecks, results, mismatches;
    private long lastFlush = System.nanoTime();

    public SpatialHashGrid(World world) {
        this.world = world;
        this.grid = new BoxGrid<>(GridConfig.get().cellSize, GridConfig.get().maxQueryCells);
    }

    // ---- maintenance ----------------------------------------------------------------------------------------

    private boolean onOwnerThread() {
        if (Thread.currentThread() == owner) return true;
        if (!poisoned) {
            poisoned = true;
            EntityCollisionGrid.LOG.warn("Entity grid of {} was touched from another thread; disabling it for this world", world.getRegistryKey().getValue());
        }
        return false;
    }

    private void syncCellSize() {
        int wanted = GridConfig.get().cellSize;
        if (grid.cellSize() != wanted) grid.rebuild(wanted);
    }

    public void insert(Entity e) {
        if (!onOwnerThread()) return;
        NodeHolder h = (NodeHolder) e;
        BoxGrid.Node<Entity> n = h.entitygrid$getNode();
        if (n == null) h.entitygrid$setNode(n = new BoxGrid.Node<>(e));
        if (n.owner instanceof SpatialHashGrid other && other != this) other.remove(e);
        else if (n.isInserted()) uncount(n);
        syncCellSize();
        n.owner = this;
        n.sx = MathHelper.floor(e.getX()) >> 4;
        n.sy = MathHelper.floor(e.getY()) >> 4;
        n.sz = MathHelper.floor(e.getZ()) >> 4;
        sectionCounts.addTo(ChunkSectionPos.asLong(n.sx, n.sy, n.sz), 1);
        Box b = e.getBoundingBox();
        grid.insert(n, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    public void remove(Entity e) {
        BoxGrid.Node<Entity> n = ((NodeHolder) e).entitygrid$getNode();
        if (n == null || n.owner != this || !n.isInserted()) return;
        if (!onOwnerThread()) return;
        uncount(n);
        grid.remove(n);
        n.owner = null;
    }

    private void uncount(BoxGrid.Node<Entity> n) {
        long key = ChunkSectionPos.asLong(n.sx, n.sy, n.sz);
        if (sectionCounts.addTo(key, -1) <= 1) sectionCounts.remove(key);
    }

    /** Called after the entity's bounding box changed. */
    public void update(Entity e) {
        BoxGrid.Node<Entity> n = ((NodeHolder) e).entitygrid$getNode();
        if (n == null || n.owner != this || !n.isInserted()) return;
        if (!onOwnerThread()) return;
        Box b = e.getBoundingBox();
        grid.update(n, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    public void clear() {
        grid.clear();
        sectionCounts.clear();
        scratch.clear();
    }

    public int size() { return grid.size(); }

    // ---- query ----------------------------------------------------------------------------------------------

    /**
     * Same contract as the entity part of vanilla {@code World.getOtherEntities}, or {@code null} if the grid cannot
     * answer (wrong thread, unknown lookup implementation, oversized query, re-entrant call): use vanilla then.
     */
    public List<Entity> getOtherEntities(EntityLookup<Entity> lookup, Entity except, Box box, Predicate<? super Entity> predicate) {
        if (poisoned || busy || Thread.currentThread() != owner) return null;
        if (!(lookup instanceof SimpleEntityLookup<Entity> simple)) return null;
        syncCellSize();

        scratch.clear();
        int candidates = grid.query(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, scratch);
        if (candidates < 0) return null;

        SectionedEntityCache<Entity> cache = ((SimpleEntityLookupAccessor) simple).entitygrid$getCache();
        // the section window vanilla scans (SectionedEntityCache.forEachInBox)
        int i = ChunkSectionPos.getSectionCoord(box.minX - 2.0);
        int j = ChunkSectionPos.getSectionCoord(box.minY - 4.0);
        int k = ChunkSectionPos.getSectionCoord(box.minZ - 2.0);
        int l = ChunkSectionPos.getSectionCoord(box.maxX + 2.0);
        int m = ChunkSectionPos.getSectionCoord(box.maxY + 0.0);
        int n = ChunkSectionPos.getSectionCoord(box.maxZ + 2.0);

        List<Entity> out = new ArrayList<>();
        busy = true;
        try {
            long lastKey = Long.MIN_VALUE;
            boolean lastTracked = false;
            for (int idx = 0, len = scratch.size(); idx < len; idx++) {
                BoxGrid.Node<Entity> node = scratch.get(idx);
                if (node.sx < i || node.sx > l || node.sy < j || node.sy > m || node.sz < k || node.sz > n) continue;
                long key = ChunkSectionPos.asLong(node.sx, node.sy, node.sz);
                if (key != lastKey) {
                    EntityTrackingSection<Entity> section = cache.findTrackingSection(key);
                    lastTracked = section != null && !section.isEmpty() && section.getStatus().shouldTrack();
                    lastKey = key;
                }
                if (!lastTracked) continue;
                Entity e = node.item;
                if (!e.getBoundingBox().intersects(box)) continue;
                if (e != except && predicate.test(e)) out.add(e);
            }
        } finally {
            busy = false;
        }

        GridConfig cfg = GridConfig.get();
        if (cfg.debugLog || cfg.debugVerify) record(lookup, except, box, predicate, out, candidates, i, j, k, l, m, n, cfg);
        return out;
    }

    // ---- debug ----------------------------------------------------------------------------------------------

    private void record(EntityLookup<Entity> lookup, Entity except, Box box, Predicate<? super Entity> predicate,
                        List<Entity> out, int candidates, int i, int j, int k, int l, int m, int n, GridConfig cfg) {
        queries++;
        gridChecks += candidates;
        results += out.size();
        // vanilla scans every entity of every section in its window
        long volume = (long) (l - i + 1) * (m - j + 1) * (n - k + 1);
        if (volume <= 512) {
            long sum = 0;
            for (int x = i; x <= l; x++)
                for (int y = j; y <= m; y++)
                    for (int z = k; z <= n; z++) sum += sectionCounts.get(ChunkSectionPos.asLong(x, y, z));
            vanillaChecks += sum;
        } else {
            vanillaChecks += candidates; // unknown, don't claim savings
        }

        if (cfg.debugVerify) {
            IdentityHashMap<Entity, Boolean> ref = new IdentityHashMap<>();
            lookup.forEachIntersects(box, e -> { if (e != except && predicate.test(e)) ref.put(e, Boolean.TRUE); });
            boolean same = ref.size() == out.size();
            if (same) for (Entity e : out) if (!ref.containsKey(e)) { same = false; break; }
            if (!same) {
                // entities of other dimensions' dragon parts etc. are not in the lookup; only report real set differences
                mismatches++;
                if (mismatches <= 5) {
                    EntityCollisionGrid.LOG.warn("Grid/vanilla mismatch in {}: grid={} vanilla={} box={}", world.getRegistryKey().getValue(), out.size(), ref.size(), box);
                }
            }
        }

        long now = System.nanoTime();
        if (now - lastFlush >= cfg.debugIntervalSeconds * 1_000_000_000L) {
            lastFlush = now;
            if (queries > 0) {
                double saved = vanillaChecks == 0 ? 0 : 100.0 * (vanillaChecks - gridChecks) / vanillaChecks;
                EntityCollisionGrid.LOG.info("[{}{}] queries={} entityChecks: vanilla~{} grid={} (-{}%) results={} tracked={}{}",
                        world.isClient() ? "client:" : "server:", world.getRegistryKey().getValue(), queries,
                        vanillaChecks, gridChecks, String.format("%.1f", saved), results, grid.size(),
                        cfg.debugVerify ? " mismatches=" + mismatches : "");
            }
            queries = gridChecks = vanillaChecks = results = 0;
        }
    }
}
