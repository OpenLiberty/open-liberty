/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning.packages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.TestConstants;
import test.common.SharedOutputManager;
import test.common.TestFile;

import com.ibm.ws.kernel.provisioning.packages.PackageIndex.Filter;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex.NodeIndex;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex.NodeIterator;
import com.ibm.ws.logging.internal.impl.LoggingConstants;

/**
 *
 */
public class PackageIndexTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);
    static File testLogDir;

    @Rule
    public TestRule outputRule = outputMgr;

    @Test
    public void test() throws Exception {
        File f = new File("../com.ibm.ws.logging/resources/liberty.ras.rawtracelist.properties");

        PackageIndex<String> index = new PackageIndex<String>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

            HashMap<String, String> packageFilters = new HashMap<String, String>(50) {};

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int pos = line.indexOf('=');
                if (pos > 0) {
                    String filter = line.substring(0, pos).trim();
                    String value = line.substring(pos + 1).trim();

                    try {
                        // add to the index
                        index.add(filter, value);

                        // add to the map we'll use to check our index
                        packageFilters.put(filter, value);
                    } catch (IllegalArgumentException e) {
                        // The unit test also validates that we have correct/usable/supported content in the
                        // liberty.ras.rawtracelist.properties file.
                        System.out.println("This test failure indicates incorrect content in " + f.getName());
                        System.out.println("Please read the exception message and correct the contents of the trace guard list:");
                        System.out.println("\t" + e.getMessage());
                        System.out.println(LoggingConstants.nl + "full path: " + f.getAbsolutePath());
                        throw e;
                    }
                }
            }

            String result = index.dump();
            System.out.println(result);
            // The dump string ends with:  ---> 202 elements
            // where that is a number that should be equal to the number of lines we read
            // from the file.

            int rpos1 = result.lastIndexOf("> ");
            int rpos2 = result.lastIndexOf(" elements");
            if (rpos1 > 0 && rpos2 > 0 && rpos1 < rpos2) {
                int numElements = Integer.parseInt(result.substring(rpos1 + 2, rpos2));
                Assert.assertEquals("The read loop should have processed the same number of elements as ended up in the index", packageFilters.size(), numElements);
            }

            // lets look for exact match: we should get back out everything we put in
            for (Map.Entry<String, String> entry : packageFilters.entrySet()) {
                Assert.assertEquals("Value retrieved should be the same as value added", entry.getValue(), index.find(entry.getKey()));
            }

            // Now look at glob pattern matches
            index.add("a.b", "A");
            Assert.assertFalse("Double add should return false", index.add("a.b", "A"));
            index.add("a.b.c", "B");
            index.add("a.b.*", "C");
            index.add("a.f", "F");

            // Test lookup matching:
            Assert.assertEquals("a.b should match a.b", "A", index.find("a.b"));
            Assert.assertEquals("a.b.c should match a.b.c", "B", index.find("a.b.c"));
            Assert.assertEquals("a.b.c.Q should match a.b.*", "C", index.find("a.b.c.Q"));
            Assert.assertEquals("a.b.Q should match a.b.*", "C", index.find("a.b.Q"));
            Assert.assertEquals("a.b.Q.R should match a.b.*", "C", index.find("a.b.Q.R"));
            Assert.assertEquals("a.f should match a.f", "F", index.find("a.f"));
            Assert.assertNull("a.f.Q should not match", index.find("a.f.Q"));
            Assert.assertNull("Q should not match", index.find("Q"));
            Assert.assertNull("a.Q should not match", index.find("a.Q"));
            Assert.assertNull("a.bQ should not match", index.find("a.bQ"));
        } finally {
            TestFile.tryToClose(br);
        }
    }

    @Test
    public void testIterators() {
        PackageIndex<String> index = new PackageIndex<String>();

        index.add("a", "1");
        index.add("a.b", "2");
        index.add("a.c", "3");
        index.add("a.c.d", "4");
        index.add("l.m.n.o.p", "5");
        index.add("w.x", "6");
        index.add("w.x.y", "7");
        index.add("z", "8");

        // See what nodeIndex returns first: store in linked hashMap which preserves order of insertion
        LinkedHashMap<String, String> nodeMap = new LinkedHashMap<String, String>();
        for (NodeIterator<String> nodeIndex = index.getNodeIterator(null); nodeIndex.hasNext();) {
            NodeIndex<String> idx = nodeIndex.next();
            nodeMap.put(idx.pkg, idx.node.getValue());
        }
        System.out.println(nodeMap);

        ArrayList<String> foundPackages = new ArrayList<String>();
        for (Iterator<String> packages = index.packageIterator(); packages.hasNext();) {
            foundPackages.add(packages.next());
        }
        System.out.println(foundPackages);

        ArrayList<String> foundValues = new ArrayList<String>();
        for (Iterator<String> values = index.iterator(); values.hasNext();) {
            foundValues.add(values.next());
        }
        System.out.println(foundValues);

        Assert.assertEquals("8 elements should be in the map", 8, nodeMap.size());
        Assert.assertEquals("Map and package list should be the same size", nodeMap.size(), foundPackages.size());
        Assert.assertEquals("Map and value list should be the same size", nodeMap.size(), foundValues.size());

        // Element by element, the iterators should have returned the same values
        // regardless of the iterator used to retrieve..
        Assert.assertArrayEquals("Map keys should be the same as the package list",
                                 nodeMap.keySet().toArray(new String[0]),
                                 foundPackages.toArray(new String[0]));

        Assert.assertArrayEquals("Map values should be the same as the value list",
                                 nodeMap.values().toArray(new String[0]),
                                 foundValues.toArray(new String[0]));
    }

    @Test
    public void testFilteredValue() {
        PackageIndex<String> index = new PackageIndex<String>();

        index.add("a", "1");
        index.add("a.b", "2");
        index.add("a.c", "3");
        index.add("a.c.d", "4");
        index.add("l.m.n.o.p", "5");
        index.add("w.x", "6");
        index.add("w.x.y", "7");
        index.add("z", "8");

        Filter<String> packageFilter = new Filter<String>() {
            @Override
            public boolean includeValue(String packageName, String value) {
                if (packageName.contains("a."))
                    return false;

                return true;
            }
        };

        ArrayList<String> foundPackages = new ArrayList<String>();
        for (Iterator<String> packages = index.packageIterator(packageFilter); packages.hasNext();) {
            foundPackages.add(packages.next());
        }
        System.out.println(foundPackages);

        Assert.assertTrue("found packages should contain 'a': " + foundPackages, foundPackages.contains("a"));
        Assert.assertFalse("found packages should not contain 'a.b': " + foundPackages, foundPackages.contains("a.b"));
        Assert.assertFalse("found packages should not contain 'a.c': " + foundPackages, foundPackages.contains("a.c"));
        Assert.assertFalse("found packages should not contain 'a.c.d': " + foundPackages, foundPackages.contains("a.c.d"));
        Assert.assertTrue("found packages should contain 'l.m.n.o.p': " + foundPackages, foundPackages.contains("l.m.n.o.p"));
        Assert.assertTrue("found packages should contain 'w.x': " + foundPackages, foundPackages.contains("w.x"));
        Assert.assertTrue("found packages should contain 'w.x.y': " + foundPackages, foundPackages.contains("w.x.y"));
        Assert.assertTrue("found packages should contain 'z': " + foundPackages, foundPackages.contains("z"));

        Filter<String> valueFilter = new Filter<String>() {
            @Override
            public boolean includeValue(String packageName, String value) {
                if (Integer.valueOf(value) > 4)
                    return false;

                return true;
            }
        };

        ArrayList<String> foundValues = new ArrayList<String>();
        for (Iterator<String> values = index.iterator(valueFilter); values.hasNext();) {
            foundValues.add(values.next());
        }
        System.out.println(foundValues);

        Assert.assertTrue("found values should contain '1': " + foundValues, foundValues.contains("1"));
        Assert.assertTrue("found values should contain '2': " + foundValues, foundValues.contains("2"));
        Assert.assertTrue("found values should contain '3': " + foundValues, foundValues.contains("3"));
        Assert.assertTrue("found values should contain '4': " + foundValues, foundValues.contains("4"));
        Assert.assertFalse("found values should not contain '5': " + foundValues, foundValues.contains("5"));
        Assert.assertFalse("found values should not contain '6': " + foundValues, foundValues.contains("6"));
        Assert.assertFalse("found values should not contain '7': " + foundValues, foundValues.contains("7"));
        Assert.assertFalse("found values should not contain '8': " + foundValues, foundValues.contains("8"));

    }
}
