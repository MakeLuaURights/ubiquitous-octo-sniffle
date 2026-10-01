package dev.entitygrid;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;

/**
 * Generic spatial hash grid over axis-aligned boxes (no Minecraft types, so it is unit-testable).
 *
 * <p>Every item is registered in all cells its box overlaps. A query returns every item registered in a cell
 * overlapped by the query box, i.e. a superset of the items whose box intersects the query box; the caller does the
 * exact test. Items with huge boxes live in a separate "large" list that every query includes.
 */
public final class BoxGrid<T> {
    public static final int MAX_CELLS_PER_ITEM = 64;

    /** Handle for one item; owned by the caller (e.g. stored on the entity). */
    public static final class Node<T> {
        public final T item;
        public Object owner;               // the grid (or facade) this node is currently inserted into
        public int sx, sy, sz;             // free payload: chunk section the item was filed under
        double minX, minY, minZ, maxX, maxY, maxZ;
        int cx0, cy0, cz0, cx1, cy1, cz1;
        boolean large;
        boolean inserted;
        int stamp;

        public Node(T item) { this.item = item; }
        public boolean isInserted() { return inserted; }
    }

    private final Long2ObjectOpenHashMap<ObjectArrayList<Node<T>>> cells = new Long2ObjectOpenHashMap<>();
    private final ObjectArrayList<Node<T>> large = new ObjectArrayList<>();
    private final ObjectOpenHashSet<Node<T>> all = new ObjectOpenHashSet<>();
    private double cellSize;
    private double inv;
    private int stamp;
    private final int maxQueryCells;

    public BoxGrid(double cellSize, int maxQueryCells) {
        this.maxQueryCells = maxQueryCells;
        setCellSize(cellSize);
    }

    public double cellSize() { return cellSize; }
    public int size() { return all.size(); }

    private void setCellSize(double size) {
        this.cellSize = Math.max(0.25, size);
        this.inv = 1.0 / this.cellSize;
    }

    /** Changes the cell size and re-files every item. */
    public void rebuild(double newCellSize) {
        List<Node<T>> nodes = new ObjectArrayList<>(all);
        cells.clear();
        large.clear();
        all.clear();
        setCellSize(newCellSize);
        for (Node<T> n : nodes) {
            n.inserted = false;
            insert(n, n.minX, n.minY, n.minZ, n.maxX, n.maxY, n.maxZ);
        }
    }

    public void clear() {
        for (Node<T> n : all) n.inserted = false;
        cells.clear();
        large.clear();
        all.clear();
    }

    public void insert(Node<T> n, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (n.inserted) remove(n);
        n.minX = minX; n.minY = minY; n.minZ = minZ;
        n.maxX = maxX; n.maxY = maxY; n.maxZ = maxZ;
        file(n);
        n.inserted = true;
        all.add(n);
    }

    public void remove(Node<T> n) {
        if (!n.inserted) return;
        unfile(n);
        all.remove(n);
        n.inserted = false;
    }

    /** Moves an item after its box changed. Cheap no-op when it stays in the same cells. */
    public void update(Node<T> n, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!n.inserted) return;
        if (n.minX == minX && n.minY == minY && n.minZ == minZ && n.maxX == maxX && n.maxY == maxY && n.maxZ == maxZ) return;
        int x0 = cell(minX), y0 = cell(minY), z0 = cell(minZ), x1 = cell(maxX), y1 = cell(maxY), z1 = cell(maxZ);
        n.minX = minX; n.minY = minY; n.minZ = minZ; n.maxX = maxX; n.maxY = maxY; n.maxZ = maxZ;
        if (!n.large && x0 == n.cx0 && y0 == n.cy0 && z0 == n.cz0 && x1 == n.cx1 && y1 == n.cy1 && z1 == n.cz1) return;
        unfile(n);
        file(n);
    }

    private static boolean finite(double v) { return v > -3.0E7 && v < 3.0E7; }

    private int cell(double v) { return (int) Math.floor(v * inv); }

    private void file(Node<T> n) {
        if (!(finite(n.minX) && finite(n.minY) && finite(n.minZ) && finite(n.maxX) && finite(n.maxY) && finite(n.maxZ))) {
            n.large = true;
            large.add(n);
            return;
        }
        n.cx0 = cell(n.minX); n.cy0 = cell(n.minY); n.cz0 = cell(n.minZ);
        n.cx1 = cell(n.maxX); n.cy1 = cell(n.maxY); n.cz1 = cell(n.maxZ);
        long count = (long) (n.cx1 - n.cx0 + 1) * (n.cy1 - n.cy0 + 1) * (n.cz1 - n.cz0 + 1);
        if (count > MAX_CELLS_PER_ITEM) {
            n.large = true;
            large.add(n);
            return;
        }
        n.large = false;
        for (int x = n.cx0; x <= n.cx1; x++)
            for (int z = n.cz0; z <= n.cz1; z++)
                for (int y = n.cy0; y <= n.cy1; y++) {
                    long key = key(x, y, z);
                    ObjectArrayList<Node<T>> list = cells.get(key);
                    if (list == null) cells.put(key, list = new ObjectArrayList<>(4));
                    list.add(n);
                }
    }

    private void unfile(Node<T> n) {
        if (n.large) {
            large.remove(n);
            n.large = false;
            return;
        }
        for (int x = n.cx0; x <= n.cx1; x++)
            for (int z = n.cz0; z <= n.cz1; z++)
                for (int y = n.cy0; y <= n.cy1; y++) {
                    long key = key(x, y, z);
                    ObjectArrayList<Node<T>> list = cells.get(key);
                    if (list == null) continue;
                    list.remove(n);
                    if (list.isEmpty()) cells.remove(key);
                }
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    /**
     * Collects distinct candidate nodes for the query box into {@code out}.
     *
     * @return number of candidates, or -1 if the query box is too big to be worth it (use the vanilla path)
     */
    public int query(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, List<Node<T>> out) {
        if (!(finite(minX) && finite(minY) && finite(minZ) && finite(maxX) && finite(maxY) && finite(maxZ))) return -1;
        int x0 = cell(minX), y0 = cell(minY), z0 = cell(minZ), x1 = cell(maxX), y1 = cell(maxY), z1 = cell(maxZ);
        long count = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        if (count > maxQueryCells) return -1;
        int s = ++stamp;
        if (s == Integer.MAX_VALUE) { stamp = 0; for (Node<T> n : all) n.stamp = 0; s = ++stamp; }
        int before = out.size();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = y0; y <= y1; y++) {
                    ObjectArrayList<Node<T>> list = cells.get(key(x, y, z));
                    if (list == null) continue;
                    for (int i = 0, len = list.size(); i < len; i++) {
                        Node<T> n = list.get(i);
                        if (n.stamp != s) { n.stamp = s; out.add(n); }
                    }
                }
        for (int i = 0, len = large.size(); i < len; i++) {
            Node<T> n = large.get(i);
            if (n.stamp != s) { n.stamp = s; out.add(n); }
        }
        return out.size() - before;
    }
}
