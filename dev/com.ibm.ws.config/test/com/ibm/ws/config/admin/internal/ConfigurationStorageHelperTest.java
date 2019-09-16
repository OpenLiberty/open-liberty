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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.ConfigStorageConsumer;
import com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.MapIterable;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import junit.framework.AssertionFailedError;

public class ConfigurationStorageHelperTest {

    public static class TestConfiguration implements ExtendedConfiguration {
        private final String location;
        private final ConfigurationDictionary props;
        private final Set<ConfigID> references;
        private final Set<String> uniqueVars;

        public TestConfiguration(String location, ConfigurationDictionary props, Set<ConfigID> references, Set<String> uniqueVars) {
            this.location = location;
            this.props = props;
            this.references = references;
            this.uniqueVars = uniqueVars;
        }

        @Override
        public void delete() throws IOException {
            // nothing
        }

        @Override
        public String getBundleLocation() {
            return location;
        }

        @Override
        public long getChangeCount() {
            return 0;
        }

        @Override
        public String getFactoryPid() {
            return null;
        }

        @Override
        public String getPid() {
            return null;
        }

        @Override
        public ConfigurationDictionary getProperties() {
            return props;
        }

        @Override
        public void setBundleLocation(String arg0) {
            // nothing
        }

        @Override
        public void update() throws IOException {
            // nothing
        }

        @Override
        public void update(Dictionary<String, ?> arg0) throws IOException {
            // nothing
        }

        @Override
        public void lock() {
            // nothing
        }

        @Override
        public void unlock() {
            // nothing
        }

        @Override
        public void fireConfigurationDeleted(Collection<Future<?>> futureList) {
            // nothing
        }

        @Override
        public void fireConfigurationUpdated(Collection<Future<?>> futureList) {
            // nothing
        }

        @Override
        public void delete(boolean fireNotifications) {
            // nothing
        }

        @Override
        public Object getProperty(String key) {
            return null;
        }

        @Override
        public Dictionary<String, Object> getReadOnlyProperties() {
            return props;
        }

        @Override
        public void updateCache(Dictionary<String, Object> properties, Set<ConfigID> references, Set<String> newUniques) throws IOException {
            // nothing
        }

        @Override
        public void updateProperties(Dictionary<String, Object> properties) throws IOException {
            // nothing
        }

        @Override
        public Set<ConfigID> getReferences() {
            return references;
        }

        @Override
        public void setInOverridesFile(boolean inOverridesFile) {
            // nothing
        }

        @Override
        public boolean isInOverridesFile() {
            return false;
        }

        @Override
        public Set<String> getUniqueVariables() {
            return uniqueVars;
        }

        @Override
        public void setFullId(ConfigID id) {
            // nothing
        }

        @Override
        public ConfigID getFullId() {
            return null;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public Set<ConfigurationAttribute> getAttributes() {
            return null;
        }

        @Override
        public void addAttributes(ConfigurationAttribute... attrs) throws IOException {
            // nothing
        }

        @Override
        public void removeAttributes(ConfigurationAttribute... attrs) throws IOException {
            // nothing
        }

        @Override
        public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
            return false;
        }

        @Override
        public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
            return null;
        }
    }

    @Test
    public void testStoreLoadCycle() throws IOException {
        File configFile = new File("build", "testData");
        ConfigurationDictionary testData = setupTestData(0);
        String bundleLocation = "the location of the bundle";
        Set<String> testVars = setupUniqueVars();
        Set<ConfigID> testConfigID = setupConfigID();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(configFile))) {
            ConfigurationStorageHelper.store(dos, testData, bundleLocation, testConfigID, testVars);
        }

        Set<String> loadedUniqueVars = new HashSet<>();
        Set<ConfigID> loadedConfigIDs = new HashSet<>();
        ConfigurationDictionary loadedConfig = new ConfigurationDictionary();

        String loadedLocation = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(configFile))) {
            loadedLocation = ConfigurationStorageHelper.load(dis, loadedUniqueVars, loadedConfigIDs, loadedConfig);
        }

        assertEquals("Location corrupted", bundleLocation, loadedLocation);
        assertEquals("unique variables corrupted", testVars, loadedUniqueVars);
        assertEquals("config ids corrupted", testConfigID, loadedConfigIDs);
        assertConfigDictEquals(testData, loadedConfig);
    }

    @Test
    public void testStoreLoadCycleWithNulls() throws IOException {
        File configFile = new File("build", "testData");
        ConfigurationDictionary testData = setupTestData(0);
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(configFile))) {
            ConfigurationStorageHelper.store(dos, testData, null, null, null);
        }
        Set<String> loadedUniqueVars = new HashSet<>();
        Set<ConfigID> loadedConfigIDs = new HashSet<>();
        ConfigurationDictionary loadedConfig = new ConfigurationDictionary();

        String loadedLocation = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(configFile))) {
            loadedLocation = ConfigurationStorageHelper.load(dis, loadedUniqueVars, loadedConfigIDs, loadedConfig);
        }
        assertNull("Location should be null", loadedLocation);
        assertTrue("unique variables should be empty", loadedUniqueVars.isEmpty());
        assertTrue("config ids should be empty", loadedConfigIDs.isEmpty());
        assertConfigDictEquals(testData, loadedConfig);
    }

    @Test
    public void testMultiConfigStore() throws IOException {
        File configFile = new File("build", "testData");
        List<TestConfiguration> expectedConfigs = setupTestConfigurations();
        ConfigurationStorageHelper.store(configFile, expectedConfigs);

        ConfigStorageConsumer<Integer, TestConfiguration> consumer = new ConfigStorageConsumer<Integer, TestConfiguration>() {
            @Override
            public TestConfiguration consumeConfigData(String location, Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict) {
                return new TestConfiguration(location, dict, references, uniqueVars);
            }

            @Override
            public Integer getKey(TestConfiguration configuration) {
                return (Integer) configuration.getProperties().get("test id");
            }
        };

        Map<Integer, TestConfiguration> loadedConfigs = ConfigurationStorageHelper.load(configFile, consumer);
        assertEquals("Wrong number of loaded configs.", expectedConfigs.size(), loadedConfigs.size());

        // sort so we can easily test expected configs in order
        loadedConfigs = new TreeMap<>(loadedConfigs);

        int i = 0;
        for (Entry<Integer, TestConfiguration> loadedConfig : loadedConfigs.entrySet()) {
            TestConfiguration expectedConfig = expectedConfigs.get(i);
            assertEquals("Wrong id.", i, loadedConfig.getKey().intValue());
            assertTestConfigurationEquals(expectedConfig, loadedConfig.getValue());
            i++;
        }
    }

    @Test
    public void testMapStore() throws IOException {
        File configFile = new File("build", "testData");
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("null", null);
        expectedMap.put("byte", (byte) 5);
        expectedMap.put("short", (short) 55);
        expectedMap.put("char", 'c');
        expectedMap.put("int", 555);
        expectedMap.put("long", (long) 5555);
        expectedMap.put("float", (float) 5.5);
        expectedMap.put("double", 55.55);
        expectedMap.put("boolean", true);
        expectedMap.put("string", "value");
        expectedMap.put("password", new SerializableProtectedString("secret".toCharArray()));
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(configFile))) {
            ConfigurationStorageHelper.writeMap(dos, ConfigurationStorageHelper.toMapOrDictionary(expectedMap));
        }
        Map<String, Object> loadedMap = new HashMap<>();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(configFile))) {
            ConfigurationStorageHelper.readMap(dis, ConfigurationStorageHelper.toMapOrDictionary(loadedMap));
        }
        assertMapEquals(expectedMap, loadedMap);
    }

    /**
     * @return
     */
    private List<TestConfiguration> setupTestConfigurations() {
        List<TestConfiguration> testConfigs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String location = i % 2 == 0 ? null : "location " + i;
            ConfigurationDictionary props = setupTestData(i);
            Set<String> testVars = setupUniqueVars();
            Set<ConfigID> testConfigID = setupConfigID();
            testConfigs.add(new TestConfiguration(location, props, testConfigID, testVars));
        }
        return testConfigs;
    }

    private void assertTestConfigurationEquals(TestConfiguration expectedConfig, TestConfiguration loadedConfig) {
        assertEquals("Location corrupted", expectedConfig.getBundleLocation(), loadedConfig.getBundleLocation());
        assertEquals("unique variables corrupted", expectedConfig.getUniqueVariables(), loadedConfig.getUniqueVariables());
        assertEquals("config ids corrupted", expectedConfig.getReferences(), loadedConfig.getReferences());
        assertConfigDictEquals(expectedConfig.getProperties(), loadedConfig.getProperties());
    }

    public static void assertConfigDictEquals(ConfigurationDictionary expected, ConfigurationDictionary actual) {
        assertMapsEqualsInternal(ConfigurationStorageHelper.toMapOrDictionary(expected), ConfigurationStorageHelper.toMapOrDictionary(actual));
    }

    public static void assertMapEquals(Map<String, Object> expected, Map<String, Object> actual) {
        assertMapsEqualsInternal(ConfigurationStorageHelper.toMapOrDictionary(expected), ConfigurationStorageHelper.toMapOrDictionary(actual));
    }

    private static void assertMapsEqualsInternal(MapIterable expected, MapIterable actual) {
        Set<String> expectedKeys = new HashSet<>();
        Map<String, Object> expectedValues = new HashMap<>();
        for (Entry<String, Object> entry : expected) {
            expectedKeys.add(entry.getKey());
            expectedValues.put(entry.getKey(), entry.getValue());
        }
        Set<String> actualKeys = new HashSet<>();
        Map<String, Object> actualValues = new HashMap<>();
        for (Entry<String, Object> entry : expected) {
            actualKeys.add(entry.getKey());
            actualValues.put(entry.getKey(), entry.getValue());
        }
        assertEquals("Config dictory is different size", expected.size(), actual.size());
        assertEquals("Config dictionary has different keys", expectedKeys, actualKeys);
        for (String key : actualKeys) {
            Object expectedValue = expectedValues.get(key);
            Object actualValue = actualValues.get(key);
            if (expectedValue == null) {
                assertNull("Expected null value.", actualValue);
            } else {
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
                    assertEquals("value bound to " + key + " are different", expectedValues.get(key), actualValues.get(key));
                }
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

    public static ConfigurationDictionary setupTestData(int testId) {
        ConfigurationDictionary dict = new ConfigurationDictionary();
        dict.put("test id", testId);
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
        dict.put("Byte null array", new Byte[] { 1, 2, null, 4, 5 });
        dict.put("Short array", new Short[] { 11, 22, 33, 44, 55 });
        dict.put("Short null array", new Short[] { 11, 22, null, 44, 55 });
        dict.put("Integer array", new Integer[] { 111, 222, 333, 444, 555 });
        dict.put("Integer null array", new Integer[] { 111, 222, null, 444, 555 });
        dict.put("Long array", new Long[] { 1111l, 2222l, 3333l, 4444l, 5555l });
        dict.put("Long null array", new Long[] { 1111l, 2222l, null, 4444l, 5555l });
        dict.put("Character array", new Character[] { 'a', 'b', 'c', 'd', 'e' });
        dict.put("Character null array", new Character[] { 'a', 'b', null, 'd', 'e' });
        dict.put("Float array", new Float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        dict.put("Float null array", new Float[] { 1.1f, 2.2f, null, 4.4f, 5.5f });
        dict.put("Double array", new Double[] { 11.11, 22.22, 33.33, 44.44, 55.55 });
        dict.put("Double null array", new Double[] { 11.11, 22.22, null, 44.44, 55.55 });
        dict.put("Boolean array", new Boolean[] { true, false, false, true, true });
        dict.put("Boolean null array", new Boolean[] { true, false, null, true, true });

        dict.put("Byte collection", asList(new Byte[] { 1, 2, 3, 4, 5 }));
        dict.put("Short collection", asList(new Short[] { 11, 22, 33, 44, 55 }));
        dict.put("Integer collection", asList(new Integer[] { 111, 222, 333, 444, 555 }));
        dict.put("Long collection", asList(new Long[] { 1111l, 2222l, 3333l, 4444l, 5555l }));
        dict.put("Character collection", asList(new Character[] { 'a', 'b', 'c', 'd', 'e' }));
        dict.put("Float collection", asList(new Float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }));
        dict.put("Double collection", asList(new Double[] { 11.11, 22.22, 33.33, 44.44, 55.55 }));
        dict.put("Boolean collection", asList(new Boolean[] { true, false, false, true, true }));
        dict.put("string collection", asList(new String[] { "abc", "def", "ghi", "jkl", "mno" }));
        dict.put("Mixed collection", asList(new Object[] {
                                                           Byte.valueOf((byte) 1),
                                                           Short.valueOf((short) 1),
                                                           Integer.valueOf(1),
                                                           Long.valueOf(1),
                                                           Character.valueOf('a'),
                                                           Float.valueOf(1.1f),
                                                           Double.valueOf(1.1),
                                                           "string" }));

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