/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import junit.framework.AssertionFailedError;

public class ConfigurationStorageHelperTest {
    @Test
    public void testStoreLoadCycle() throws IOException {
        File configFile = new File("build", "testData");
        ConfigurationDictionary testData = setupTestData();
        String bundleLocation = "the location of the bundle";
        Set<String> testVars = setupUniqueVars();
        Set<ConfigID> testConfigID = setupConfigID();
        ConfigurationStorageHelper.store(configFile, testData, bundleLocation, testConfigID, testVars);

        Set<String> loadedUniqueVars = new HashSet<>();
        Set<ConfigID> loadedConfigIDs = new HashSet<>();
        ConfigurationDictionary loadedConfig = new ConfigurationDictionary();

        String loadedLocation = ConfigurationStorageHelper.load(configFile, loadedUniqueVars, loadedConfigIDs, loadedConfig);

        assertEquals("Location corrupted", bundleLocation, loadedLocation);
        assertEquals("unique variables corrupted", testVars, loadedUniqueVars);
        assertEquals("config ids corrupted", testConfigID, loadedConfigIDs);
        assertConfigDictEquals(testData, loadedConfig);
    }

    @Test
    public void testStoreLoadCycleWithNulls() throws IOException {
        File configFile = new File("build", "testData");
        ConfigurationDictionary testData = setupTestData();
        ConfigurationStorageHelper.store(configFile, testData, null, null, null);

        Set<String> loadedUniqueVars = new HashSet<>();
        Set<ConfigID> loadedConfigIDs = new HashSet<>();
        ConfigurationDictionary loadedConfig = new ConfigurationDictionary();

        String loadedLocation = ConfigurationStorageHelper.load(configFile, loadedUniqueVars, loadedConfigIDs, loadedConfig);

        assertNull("Location should be null", loadedLocation);
        assertTrue("unique variables should be empty", loadedUniqueVars.isEmpty());
        assertTrue("config ids should be empty", loadedConfigIDs.isEmpty());
        assertConfigDictEquals(testData, loadedConfig);
    }

    @Test
    public void testLoadExistingFile() throws IOException {
        File configFile = new File("test-resources/test_config_cache", "testCache");

        Set<String> loadedUniqueVars = new HashSet<>();
        Set<ConfigID> loadedConfigIDs = new HashSet<>();
        ConfigurationDictionary loadedConfig = new ConfigurationDictionary();

        String loadedLocation = ConfigurationStorageHelper.load(configFile, loadedUniqueVars, loadedConfigIDs, loadedConfig);

        ConfigurationDictionary expectedData = setupTestData();
        String expectedLocation = "the location of the bundle";
        Set<String> expectedVars = setupUniqueVars();
        Set<ConfigID> expectedConfigID = setupConfigID();

        assertEquals("Location corrupted", expectedLocation, loadedLocation);
        assertEquals("unique variables corrupted", expectedVars, loadedUniqueVars);
        assertEquals("config ids corrupted", expectedConfigID, loadedConfigIDs);
        assertConfigDictEquals(expectedData, loadedConfig);
    }

    public static void assertConfigDictEquals(ConfigurationDictionary expected, ConfigurationDictionary actual) {
        Set<String> expectedKeys = new HashSet<>();
        Enumeration<String> keys = expected.keys();
        while (keys.hasMoreElements()) {
            expectedKeys.add(keys.nextElement());
        }
        Set<String> actualKeys = new HashSet<>();
        keys = actual.keys();
        while (keys.hasMoreElements()) {
            actualKeys.add(keys.nextElement());
        }
        assertEquals("Config dictory is different size", expected.size(), actual.size());
        assertEquals("Config dictionary has different keys", expectedKeys, actualKeys);
        for (String key : actualKeys) {
            Object expectedValue = expected.get(key);
            Object actualValue = actual.get(key);

            assertEquals("value bound to " + key + " are different types", expectedValue.getClass(), actualValue.getClass());

            if (expectedValue.getClass().isArray()) {
                if (expectedValue instanceof byte[]) {
                    assertArrayEquals("byte array for key " + key + " are different", (byte[]) expectedValue, (byte[]) actualValue);
                } else if (expectedValue instanceof short[]) {
                    assertArrayEquals("short array for key " + key + " are different", (short[]) expectedValue, (short[]) actualValue);
                } else if (expectedValue instanceof int[]) {
                    assertArrayEquals("int array for key " + key + " are different", (int[]) expectedValue, (int[]) actualValue);
                } else if (expectedValue instanceof long[]) {
                    assertArrayEquals("long array for key " + key + " are different", (long[]) expectedValue, (long[]) actualValue);
                } else if (expectedValue instanceof float[]) {
                    assertArrayEquals("float array for key " + key + " are different", (float[]) expectedValue, (float[]) actualValue, 0.2f);
                } else if (expectedValue instanceof double[]) {
                    assertArrayEquals("double array for key " + key + " are different", (double[]) expectedValue, (double[]) actualValue, 0.2);
                } else if (expectedValue instanceof boolean[]) {
                    assertBooleanArrayEquals("boolean array for key " + key + " are different", (boolean[]) expectedValue, (boolean[]) actualValue);
                } else if (expectedValue instanceof char[]) {
                    assertArrayEquals("char array for key " + key + " are different", (char[]) expectedValue, (char[]) actualValue);
                } else if (expectedValue instanceof String[]) {
                    assertArrayEquals("String array for key " + key + " are different", (String[]) expectedValue, (String[]) actualValue);
                } else if (expectedValue instanceof Object[]) {
                    assertArrayEquals("Object array for key " + key + " are different", (Object[]) expectedValue, (Object[]) actualValue);
                }
            } else {
                assertEquals("value bound to " + key + " are different", expected.get(key), actual.get(key));
            }
        }
    }

    public static void assertBooleanArrayEquals(String message, boolean[] expecteds, boolean[] actuals) {
        if (expecteds.length == actuals.length) {
            for (int i = 0; i < expecteds.length; i++) {
                if (expecteds[i] != actuals[i]) {
                    throw new AssertionFailedError("The boolean arrays are different at position " + i + ". Expected: " + Arrays.toString(expecteds) + " actual: "
                                                   + Arrays.toString(actuals));
                }
            }
        } else {
            throw new AssertionFailedError("The boolean arrays are different lengths. Expected: " + Arrays.toString(expecteds) + "[" + expecteds.length + "] actual: "
                                           + Arrays.toString(actuals) + "[" + actuals.length + "]");
        }
    }

    public static Set<String> setupUniqueVars() {
        Set<String> uniqueVars = new HashSet<>();
        uniqueVars.add("wlp.install.dir");
        uniqueVars.add("one");
        uniqueVars.add("two");

        return uniqueVars;
    }

    public static Set<ConfigID> setupConfigID() {
        Set<ConfigID> configIDs = new HashSet<>();

        configIDs.add(new ConfigID(null, "this.is.a.pid", "thisId", "attribute"));
        configIDs.add(new ConfigID(null, "this.is.a.pid2", "thisId2", "attribute2"));
        configIDs.add(new ConfigID("only.pid"));

        ConfigID grandParent = new ConfigID(null, "grandParent.pid", "grandParent.Id", "grandParent.Attribute");
        ConfigID parentOne = new ConfigID(grandParent, "parentOne.pid", "parentOne.Id", "parentOne.Attribute");
        ConfigID parentTwo = new ConfigID(grandParent, "parentTwo.pid", "parentTwo.Id", "parentTwo.Attribute");

        configIDs.add(new ConfigID(parentOne, "cousinOne.pid", "cousinOne.Id", "cousinOne.Attribute"));
        configIDs.add(new ConfigID(parentTwo, "cousinTwo.pid", "cousinTwo.Id", "cousinTwo.Attribute"));

        return configIDs;
    }

    public static ConfigurationDictionary setupTestData() {
        ConfigurationDictionary dict = new ConfigurationDictionary();
        dict.put("byte", (byte) 5);
        dict.put("short", (short) 55);
        dict.put("char", 'c');
        dict.put("int", 555);
        dict.put("long", (long) 5555);
        dict.put("float", (float) 5.5);
        dict.put("double", 55.55);
        dict.put("boolean", true);
        dict.put("string", "value");
        dict.put("password", new SerializableProtectedString("secret".toCharArray()));

        dict.put("map", setupSubMap());
        dict.put("byte array", new byte[] { 1, 2, 3, 4, 5 });
        dict.put("short array", new short[] { 11, 22, 33, 44, 55 });
        dict.put("int array", new int[] { 111, 222, 333, 444, 555 });
        dict.put("long array", new long[] { 1111, 2222, 3333, 4444, 5555 });
        dict.put("char array", new char[] { 'a', 'b', 'c', 'd', 'e' });
        dict.put("float array", new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        dict.put("double array", new double[] { 11.11, 22.22, 33.33, 44.44, 55.55 });
        dict.put("boolean array", new boolean[] { true, false, false, true, true });
        dict.put("string array", new String[] { "abc", "def", "ghi", "jkl", "mno" });

        dict.put("Byte array", new Byte[] { 1, 2, 3, 4, 5 });
        dict.put("Short array", new Short[] { 11, 22, 33, 44, 55 });
        dict.put("Integer array", new Integer[] { 111, 222, 333, 444, 555 });
        dict.put("Long array", new Long[] { 1111l, 2222l, 3333l, 4444l, 5555l });
        dict.put("Character array", new Character[] { 'a', 'b', 'c', 'd', 'e' });
        dict.put("Float array", new Float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        dict.put("Double array", new Double[] { 11.11, 22.22, 33.33, 44.44, 55.55 });
        dict.put("Boolean array", new Boolean[] { true, false, false, true, true });

        dict.put("Byte collection", asList(new Byte[] { 1, 2, 3, 4, 5 }));
        dict.put("Short collection", asList(new Short[] { 11, 22, 33, 44, 55 }));
        dict.put("Integer collection", asList(new Integer[] { 111, 222, 333, 444, 555 }));
        dict.put("Long collection", asList(new Long[] { 1111l, 2222l, 3333l, 4444l, 5555l }));
        dict.put("Character collection", asList(new Character[] { 'a', 'b', 'c', 'd', 'e' }));
        dict.put("Float collection", asList(new Float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }));
        dict.put("Double collection", asList(new Double[] { 11.11, 22.22, 33.33, 44.44, 55.55 }));
        dict.put("Boolean collection", asList(new Boolean[] { true, false, false, true, true }));
        dict.put("string collection", asList(new String[] { "abc", "def", "ghi", "jkl", "mno" }));

        return dict;
    }

    public static Collection<?> asList(Object[] array) {
        List<Object> list = new ArrayList<>();
        for (Object obj : array) {
            list.add(obj);
        }
        return list;
    }

    public static Map<String, Object> setupSubMap() {
        Map<String, Object> result = new HashMap<>();

        result.put("one", 1);
        result.put("string", "sub-string");

        return result;
    }
}