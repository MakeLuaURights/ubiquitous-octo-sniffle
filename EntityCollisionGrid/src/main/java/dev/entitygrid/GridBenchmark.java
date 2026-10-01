package dev.entitygrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Synthetic benchmark: N mobs crammed into an area, each one asks "who touches my box?" (like tickCramming).
 * Compares the number of exact AABB checks of vanilla's section scan against the grid's candidates.
 */
public final class GridBenchmark {
    public record Result(int entities, long vanillaChecks, long gridChecks, long hits) {
        public double reductionPercent() { return vanillaChecks == 0 ? 0 : 100.0 * (vanillaChecks - gridChecks) / vanillaChecks; }
        @Override public String toString() {
            return String.format("entities=%d vanillaChecks=%d gridChecks=%d hits=%d reduction=%.1f%%", entities, vanillaChecks, gridChecks, hits, reductionPercent());
        }
    }

    private GridBenchmark() {}

    public static Result run(int count, double areaBlocks, double cellSize, long seed) {
        Random rnd = new Random(seed);
        double w = 0.6, h = 1.8;
        double[][] boxes = new double[count][];
        BoxGrid<Integer> grid = new BoxGrid<>(cellSize, 512);
        BoxGrid.Node<Integer>[] nodes = newNodes(count);
        Map<Long, Integer> sections = new HashMap<>();
        int[][] sec = new int[count][];
        for (int i = 0; i < count; i++) {
            double x = rnd.nextDouble() * areaBlocks, z = rnd.nextDouble() * areaBlocks, y = 64;
            boxes[i] = new double[]{x - w / 2, y, z - w / 2, x + w / 2, y + h, z + w / 2};
            nodes[i] = new BoxGrid.Node<>(i);
            grid.insert(nodes[i], boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], boxes[i][4], boxes[i][5]);
            sec[i] = new int[]{Math.floorDiv((int) Math.floor(x), 16), Math.floorDiv(64, 16), Math.floorDiv((int) Math.floor(z), 16)};
            sections.merge(secKey(sec[i][0], sec[i][1], sec[i][2]), 1, Integer::sum);
        }
        long vanilla = 0, gridChecks = 0, hits = 0;
        ArrayList<BoxGrid.Node<Integer>> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] b = boxes[i];
            // vanilla: every entity in every section of the scan window
            int i0 = Math.floorDiv((int) Math.floor(b[0] - 2.0), 16), j0 = Math.floorDiv((int) Math.floor(b[1] - 4.0), 16), k0 = Math.floorDiv((int) Math.floor(b[2] - 2.0), 16);
            int i1 = Math.floorDiv((int) Math.floor(b[3] + 2.0), 16), j1 = Math.floorDiv((int) Math.floor(b[4]), 16), k1 = Math.floorDiv((int) Math.floor(b[5] + 2.0), 16);
            for (int x = i0; x <= i1; x++) for (int y = j0; y <= j1; y++) for (int z = k0; z <= k1; z++) vanilla += sections.getOrDefault(secKey(x, y, z), 0);
            out.clear();
            gridChecks += grid.query(b[0], b[1], b[2], b[3], b[4], b[5], out);
            for (BoxGrid.Node<Integer> n : out) {
                double[] o = boxes[n.item];
                if (n.item != i && o[0] < b[3] && o[3] > b[0] && o[1] < b[4] && o[4] > b[1] && o[2] < b[5] && o[5] > b[2]) hits++;
            }
        }
        return new Result(count, vanilla, gridChecks, hits);
    }

    @SuppressWarnings("unchecked")
    private static BoxGrid.Node<Integer>[] newNodes(int n) { return new BoxGrid.Node[n]; }

    private static long secKey(int x, int y, int z) { return ((long) x << 40) ^ ((long) y << 20) ^ z; }

    public static void runAndLog() {
        for (int n : new int[]{50, 100, 400}) {
            EntityCollisionGrid.LOG.info("Benchmark (cell={}): {}", GridConfig.get().cellSize, run(n, 14.0, GridConfig.get().cellSize, 42L));
        }
    }
}
