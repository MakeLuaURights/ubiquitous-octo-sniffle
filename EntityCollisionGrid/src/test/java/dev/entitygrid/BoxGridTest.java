package dev.entitygrid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoxGridTest {
    @Test
    void candidatesAlwaysContainEveryIntersectingBox_afterRandomMovesAndRemovals() {
        Random rnd = new Random(1);
        for (double cell : new double[]{1, 2, 3, 8}) {
            BoxGrid<Integer> grid = new BoxGrid<>(cell, 100000);
            int n = 300;
            double[][] boxes = new double[n][];
            BoxGrid.Node<Integer>[] nodes = new BoxGrid.Node[n];
            boolean[] alive = new boolean[n];
            for (int i = 0; i < n; i++) {
                nodes[i] = new BoxGrid.Node<>(i);
                boxes[i] = randomBox(rnd);
                grid.insert(nodes[i], boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], boxes[i][4], boxes[i][5]);
                alive[i] = true;
            }
            for (int step = 0; step < 4000; step++) {
                int i = rnd.nextInt(n);
                switch (rnd.nextInt(4)) {
                    case 0 -> { boxes[i] = randomBox(rnd); if (alive[i]) grid.update(nodes[i], boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], boxes[i][4], boxes[i][5]); }
                    case 1 -> { if (alive[i]) { grid.remove(nodes[i]); alive[i] = false; } }
                    case 2 -> { if (!alive[i]) { boxes[i] = randomBox(rnd); grid.insert(nodes[i], boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], boxes[i][4], boxes[i][5]); alive[i] = true; } }
                    default -> {
                        double[] q = randomBox(rnd);
                        ArrayList<BoxGrid.Node<Integer>> out = new ArrayList<>();
                        int c = grid.query(q[0], q[1], q[2], q[3], q[4], q[5], out);
                        assertEquals(out.size(), c);
                        Set<Integer> found = new HashSet<>();
                        for (var node : out) assertTrue(found.add(node.item), "duplicate candidate");
                        for (int j = 0; j < n; j++) {
                            boolean hit = alive[j] && boxes[j][0] < q[3] && boxes[j][3] > q[0] && boxes[j][1] < q[4] && boxes[j][4] > q[1] && boxes[j][2] < q[5] && boxes[j][5] > q[2];
                            if (hit) assertTrue(found.contains(j), "missed intersecting item " + j + " (cell " + cell + ")");
                            if (!alive[j]) assertFalse(found.contains(j), "removed item returned");
                        }
                    }
                }
            }
        }
    }

    @Test
    void rebuildKeepsEverything() {
        BoxGrid<Integer> grid = new BoxGrid<>(2, 1000);
        BoxGrid.Node<Integer> a = new BoxGrid.Node<>(1), b = new BoxGrid.Node<>(2);
        grid.insert(a, 0, 0, 0, 1, 1, 1);
        grid.insert(b, 100, 0, 100, 101, 2, 101);
        grid.rebuild(5);
        ArrayList<BoxGrid.Node<Integer>> out = new ArrayList<>();
        grid.query(0.5, 0.5, 0.5, 0.6, 0.6, 0.6, out);
        assertEquals(1, out.size());
        assertEquals(1, out.get(0).item);
    }

    @Test
    void hugeBoxesAndHugeQueries() {
        BoxGrid<Integer> grid = new BoxGrid<>(2, 100);
        BoxGrid.Node<Integer> big = new BoxGrid.Node<>(7);
        grid.insert(big, -500, 0, -500, 500, 300, 500);   // far more than MAX_CELLS_PER_ITEM -> "large" list
        ArrayList<BoxGrid.Node<Integer>> out = new ArrayList<>();
        assertEquals(1, grid.query(0, 0, 0, 1, 1, 1, out));
        assertEquals(-1, grid.query(0, 0, 0, 400, 100, 400, new ArrayList<>()));   // too many cells -> caller falls back
    }

    @Test
    void benchmarkMeetsPerformanceTarget() {
        for (int count : new int[]{50, 100, 400}) {
            GridBenchmark.Result r = GridBenchmark.run(count, 14.0, 2.0, 42L);
            System.out.println("BENCHMARK " + r);
            assertTrue(r.reductionPercent() >= 40.0, "expected >= 40% fewer checks, got " + r);
        }
    }

    private static double[] randomBox(Random rnd) {
        double x = rnd.nextDouble() * 60 - 30, y = rnd.nextDouble() * 40, z = rnd.nextDouble() * 60 - 30;
        double w = rnd.nextBoolean() ? 0.6 : rnd.nextDouble() * 6, h = rnd.nextBoolean() ? 1.8 : rnd.nextDouble() * 5;
        return new double[]{x, y, z, x + w, y + h, z + w};
    }
}
