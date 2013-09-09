/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License, Version
 * 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html). Initial Developer: H2 Group
 */
package org.h2.test.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.h2.mvstore.MVStore;
import org.h2.test.TestBase;
import org.h2.util.New;

/**
 * Tests the performance and memory usage claims in the documentation.
 */
public class TestMVStoreBenchmark extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase test = TestBase.createCaller().init();
        test.config.traceTest = true;
        test.config.big = true;
        test.test();
    }

    @Override
    public void test() throws Exception {
        if (!config.big) {
            return;
        }
        if (config.coverage || config.codeCoverage) {
            // run only when _not_ using a code coverage tool,
            // because the tool might instrument our code but not
            // java.util.*
            return;
        }
        
        testPerformanceComparison();
        testMemoryUsageComparison();
    }

    private void testMemoryUsageComparison() {
        long[] mem;
        long hash, tree, mv;
        String msg;

        mem = getMemoryUsed(10000, 10);
        hash = mem[0];
        tree = mem[1];
        mv = mem[2];
        msg = Arrays.toString(mem);
        assertTrue(msg, hash < mv);
        assertTrue(msg, tree < mv);

        mem = getMemoryUsed(10000, 30);
        hash = mem[0];
        tree = mem[1];
        mv = mem[2];
        msg = Arrays.toString(mem);
        assertTrue(msg, mv < hash);
        assertTrue(msg, mv < tree);

    }

    private static long[] getMemoryUsed(int count, int size) {
        long hash, tree, mv;
        ArrayList<Map<Integer, String>> mapList;
        long mem;

        mapList = New.arrayList();
        mem = getMemory();
        for (int i = 0; i < count; i++) {
            mapList.add(new HashMap<Integer, String>(size));
        }
        addEntries(mapList, size);
        hash = getMemory() - mem;
        mapList.size();

        mapList = New.arrayList();
        mem = getMemory();
        for (int i = 0; i < count; i++) {
            mapList.add(new TreeMap<Integer, String>());
        }
        addEntries(mapList, size);
        tree = getMemory() - mem;
        mapList.size();

        mapList = New.arrayList();
        mem = getMemory();
        MVStore store = MVStore.open(null);
        for (int i = 0; i < count; i++) {
            Map<Integer, String> map = store.openMap("t" + i);
            mapList.add(map);
        }
        addEntries(mapList, size);
        mv = getMemory() - mem;
        mapList.size();

        return new long[]{hash, tree, mv};
    }

    private static void addEntries(List<Map<Integer, String>> mapList, int size) {
        for (Map<Integer, String> map : mapList) {
            for (int i = 0; i < size; i++) {
                map.put(i, "Hello World");
            }
        }
    }

    static long getMemory() {
        try {
            LinkedList<byte[]> list = new LinkedList<byte[]>();
            while (true) {
                list.add(new byte[1024]);
            }
        } catch (OutOfMemoryError e) {
            // ok
        }
        for (int i = 0; i < 16; i++) {
            System.gc();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return getMemoryUsedBytes();
    }

    private void testPerformanceComparison() {
        if (!config.big) {
            return;
        }
        int size = 1000000;
        Map<Integer, String> map;
        map = new HashMap<Integer, String>(size);
        long hash = testPerformance(map, size);
        map = new TreeMap<Integer, String>();
        long tree = testPerformance(map, size);
        MVStore store = MVStore.open(null);
        map = store.openMap("test");
        long mv = testPerformance(map, size);
        String msg = "mv " + mv + " tree " + tree + " hash " + hash;
        assertTrue(msg, hash < tree);
        assertTrue(msg, hash < mv);
        assertTrue(msg, mv < tree);
    }

    private long testPerformance(Map<Integer, String> map, int size) {
        System.gc();
        long time = 0;
        for (int t = 0; t < 3; t++) {
            time = System.currentTimeMillis();
            for (int b = 0; b < 3; b++) {
                for (int i = 0; i < size; i++) {
                    map.put(i, "Hello World");
                }
                for (int a = 0; a < 5; a++) {
                    for (int i = 0; i < size; i++) {
                        String x = map.get(i);
                        assertTrue(x != null);
                    }
                }
                for (int i = 0; i < size; i++) {
                    map.remove(i);
                }
                assertEquals(0, map.size());
            }
            time = System.currentTimeMillis() - time;
        }
        // System.out.println(map.getClass().getName() + ": " + time);
        return time;
    }

}