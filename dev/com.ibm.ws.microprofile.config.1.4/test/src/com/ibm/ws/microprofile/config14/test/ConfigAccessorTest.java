/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.ConfigSnapshot;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import com.ibm.ws.microprofile.config14.impl.ConfigAccessorBuilderImpl;

/**
 *
 */
public class ConfigAccessorTest extends AbstractConfigTest {

    private static final long CACHE_TIME = 500;//ms

    @Test
    public void testCacheFor() throws InterruptedException {
        System.setProperty("testCacheFor", "value1");
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("testCacheFor", String.class);
        builder.cacheFor(Duration.ofMillis(CACHE_TIME));
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correct", "value1", value);
        System.setProperty("testCacheFor", "value2");
        value = accessor.getValue();
        assertEquals("Value not correct", "value1", value);
        Thread.sleep(CACHE_TIME); //long enough for the cache to expire
        value = accessor.getValue();
        assertEquals("Value not correct", "value2", value);
    }

    @Test
    public void testCacheInvalidation() throws InterruptedException {
        ConfigBuilder configBuilder = ConfigProviderResolver.instance().getBuilder();
        configBuilder.addDefaultSources();
        ChangeSupportConfigSource mySource = new ChangeSupportConfigSource();
        mySource.put("testCacheInvalidation", "value1");
        configBuilder.withSources(mySource);
        Config config = configBuilder.build();

        ConfigAccessorBuilder<String> builder = config.access("testCacheInvalidation", String.class);
        builder.cacheFor(Duration.ofMillis(CACHE_TIME * 100)); //50 seconds
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correct", "value1", value);

        mySource.put("testCacheInvalidation", "value2");
        value = accessor.getValue();
        assertEquals("Value not correct", "value2", value);
    }

    @Test
    public void testOptionalCacheFor() throws InterruptedException {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("testOptionalCacheFor", String.class);
        builder.cacheFor(Duration.ofMillis(CACHE_TIME));
        ConfigAccessor<String> accessor = builder.build();
        try {
            accessor.getValue();
            fail("NoSuchElementException not thrown");
        } catch (NoSuchElementException e) {
            //expected
        }
        System.setProperty("testOptionalCacheFor", "value1");
        try {
            accessor.getValue();
            fail("NoSuchElementException not thrown");
        } catch (NoSuchElementException e) {
            //expected
        }
        Optional<String> optional = accessor.getOptionalValue();
        assertFalse("Optional should not be present", optional.isPresent());
        Thread.sleep(CACHE_TIME); //long enough for the cache to expire
        optional = accessor.getOptionalValue();
        assertTrue("Optional should be present", optional.isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroCacheFor() {
        System.setProperty("testCacheFor", "value1");
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("testZeroCacheFor", String.class);
        builder.cacheFor(Duration.ofMillis(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCacheFor() {
        System.setProperty("testCacheFor", "value1");
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("testNegativeCacheFor", String.class);
        builder.cacheFor(Duration.ofMillis(-1));
    }

    @Test
    public void testSnapshot() {
        System.setProperty("testSnapshot1", "value1");
        System.setProperty("testSnapshot2", "value2");
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder1 = config.access("testSnapshot1", String.class);
        ConfigAccessor<String> accessor1 = builder1.build();

        ConfigAccessorBuilder<String> builder2 = config.access("testSnapshot2", String.class);
        ConfigAccessor<String> accessor2 = builder2.build();

        ConfigSnapshot snapshot = config.snapshotFor(accessor1, accessor2);
        String value1 = accessor1.getValue(snapshot);
        assertEquals("Value not correct", "value1", value1);
        String value2 = accessor2.getValue(snapshot);
        assertEquals("Value not correct", "value2", value2);

        System.setProperty("testSnapshot1", "changed_value1");
        System.setProperty("testSnapshot2", "changed_value2");

        value1 = accessor1.getValue(snapshot);
        assertEquals("Value not correct", "value1", value1);
        value2 = accessor2.getValue(snapshot);
        assertEquals("Value not correct", "value2", value2);

        value1 = accessor1.getValue();
        assertEquals("Value not correct", "changed_value1", value1);
        value2 = accessor2.getValue();
        assertEquals("Value not correct", "changed_value2", value2);
    }

    @Test
    public void testPropertyNameGeneration() {
        List<String> expectedPropertyNames = new ArrayList<>();
        expectedPropertyNames.add("BASE.ONE.TWO.THREE.FOUR");
        expectedPropertyNames.add("BASE.ONE.TWO.THREE");
        expectedPropertyNames.add("BASE.ONE.TWO.FOUR");
        expectedPropertyNames.add("BASE.ONE.TWO");
        expectedPropertyNames.add("BASE.ONE.THREE.FOUR");
        expectedPropertyNames.add("BASE.ONE.THREE");
        expectedPropertyNames.add("BASE.ONE.FOUR");
        expectedPropertyNames.add("BASE.ONE");
        expectedPropertyNames.add("BASE.TWO.THREE.FOUR");
        expectedPropertyNames.add("BASE.TWO.THREE");
        expectedPropertyNames.add("BASE.TWO.FOUR");
        expectedPropertyNames.add("BASE.TWO");
        expectedPropertyNames.add("BASE.THREE.FOUR");
        expectedPropertyNames.add("BASE.THREE");
        expectedPropertyNames.add("BASE.FOUR");
        expectedPropertyNames.add("BASE");

        List<String> suffixValues = new ArrayList<>();
        suffixValues.add("ONE");
        suffixValues.add("TWO");
        suffixValues.add("THREE");
        suffixValues.add("FOUR");
        List<String> propertyNames = ConfigAccessorBuilderImpl.generatePropertyNameList("BASE", suffixValues);
        assertEquals("Wrong number of property names", expectedPropertyNames.size(), propertyNames.size());
        for (int i = 0; i < propertyNames.size(); i++) {
            String propertyName = propertyNames.get(i);
            String expectedName = expectedPropertyNames.get(i);
            System.out.println(propertyName);
            assertEquals("Generated name did not match", expectedName, propertyName);
        }
    }

//    @Test
//    public void testLookupSuffix() {
//        System.setProperty("testLookupSuffix", "WRONG");
//        System.setProperty("testLookupSuffix.TWO", "RIGHT");
//        System.setProperty("testLookupSuffix.THREE", "WRONG");
//
//        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
//        ConfigAccessorBuilder<String> builder1 = config.access("testLookupSuffix", String.class);
//        builder1.addLookupSuffix("ONE");
//        builder1.addLookupSuffix("TWO");
//        builder1.addLookupSuffix("THREE");
//        builder1.addLookupSuffix("FOUR");
//        ConfigAccessor<String> accessor1 = builder1.build();
//
//        String value = accessor1.getValue();
//        assertEquals("Value not correct", "RIGHT", value);
//
//        String resolvedPropertyName = accessor1.getResolvedPropertyName();
//        assertEquals("testLookupSuffix.TWO", resolvedPropertyName);
//    }
//
//    @Test
//    public void testAsList() {
//        System.setProperty("testAsList", "value1,value2,value3");
//        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
//        ConfigAccessorBuilder<String> builder = config.access("testAsList", String.class);
//        ConfigAccessorBuilder<List<String>> listBuilder = builder.asList();
//        ConfigAccessor<List<String>> accessor = listBuilder.build();
//        List<String> value = accessor.getValue();
//        assertEquals("Value not correct", "value1", value.get(0));
//        assertEquals("Value not correct", "value2", value.get(1));
//        assertEquals("Value not correct", "value3", value.get(2));
//    }

    @Test
    public void testUseConverter() {
        System.setProperty("testWithConverter", "value1");
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<MyTestObject> builder = config.access("testWithConverter", MyTestObject.class);
        builder.useConverter(new MyTestObjectConverter());
        ConfigAccessor<MyTestObject> accessor = builder.build();
        MyTestObject value = accessor.getValue();
        assertEquals("Value not correct", "MyTestObject(value1)", value.toString());
    }

//    @Test
//    public void testAsListWithConverter() {
//        System.setProperty("testAsListWithConverter", "value1,value2,value3");
//        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
//        ConfigAccessorBuilder<MyTestObject> builder = config.access("testAsListWithConverter", MyTestObject.class);
//        ConfigAccessorBuilder<List<MyTestObject>> listBuilder = builder.asList();
//        listBuilder.withConverter(new MyTestObjectListConverter());
//        ConfigAccessor<List<MyTestObject>> accessor = listBuilder.build();
//        List<MyTestObject> value = accessor.getValue();
//        assertEquals("Value not correct", "MyTestObject(value1)", value.get(0).toString());
//        assertEquals("Value not correct", "MyTestObject(value2)", value.get(1).toString());
//        assertEquals("Value not correct", "MyTestObject(value3)", value.get(2).toString());
//        assertEquals("List is wrong type", LinkedList.class, value.getClass());
//    }
//
//    @Test
//    public void testAsListWithExistingDefaultValue() {
//        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
//        ConfigAccessorBuilder<MyTestObject> builder = config.access("testAsListWithExistingStuff", MyTestObject.class); //property does not exist, will use default
//
//        MyTestObject defaultValue = new MyTestObject();
//        defaultValue.setRawString("DEFAULT");
//        builder.withDefault(defaultValue);
//
//        ConfigAccessorBuilder<List<MyTestObject>> listBuilder = builder.asList();
//        ConfigAccessor<List<MyTestObject>> accessor = listBuilder.build();
//        List<MyTestObject> value = accessor.getValue();
//
//        assertEquals("List should be a singleton", 1, value.size());
//        assertEquals("Value not correct", "MyTestObject(DEFAULT)", value.get(0).toString());
//    }

//    @Test
//    public void testAsListWithExistingDefaultString() {
//        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
//        ConfigAccessorBuilder<MyTestObject> builder = config.access("testAsListWithExistingDefaultString", MyTestObject.class); //property does not exist, will use default
//
//        builder.withStringDefault("value1,value2,value3");
//        ConfigAccessorBuilder<List<MyTestObject>> listBuilder = builder.asList();
//        listBuilder.withConverter(new MyTestObjectListConverter());
//
//        ConfigAccessor<List<MyTestObject>> accessor = listBuilder.build();
//        List<MyTestObject> value = accessor.getValue();
//
//        assertEquals("Value not correct", "MyTestObject(value1)", value.get(0).toString());
//        assertEquals("Value not correct", "MyTestObject(value2)", value.get(1).toString());
//        assertEquals("Value not correct", "MyTestObject(value3)", value.get(2).toString());
//    }

    @Test
    public void testGetDefaultValue() {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessor<Integer> accessor1 = config.access("testGetDefaultValue", Integer.class).withDefault(123).build();
        ConfigAccessor<Integer> accessor2 = config.access("testGetDefaultValue", Integer.class).withStringDefault("123").build();
        Integer int1 = accessor1.getDefaultValue();
        Integer int2 = accessor2.getDefaultValue();
        assertEquals(int1, int2);
        assertEquals(new Integer(123), int1);
    }

    @Test
    public void testGetDefaultValue2() {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessor<Integer> accessor1 = config.access("testGetDefaultValue", Integer.class).withDefault(123).withStringDefault("456").build();
        ConfigAccessor<Integer> accessor2 = config.access("testGetDefaultValue", Integer.class).withStringDefault("123").withDefault(456).build();
        Integer int1 = accessor1.getDefaultValue();
        Integer int2 = accessor2.getDefaultValue();
        assertEquals(int1, int2);
        assertEquals(new Integer(456), int1);
    }

    @Test
    public void testGetDefaultValueNull() {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessor<Integer> accessor1 = config.access("testGetDefaultValue", Integer.class).withDefault(null).build();
        Integer int1 = accessor1.getDefaultValue();
        assertNull(int1);
    }

    public static class MyTestObject {
        private String rawString;

        public void setRawString(String string) {
            this.rawString = string;
        }

        @Override
        public String toString() {
            return "MyTestObject(" + rawString + ")";
        }
    }

    public static class MyTestObjectConverter implements Converter<MyTestObject> {

        /** {@inheritDoc} */
        @Override
        public MyTestObject convert(String value) {
            MyTestObject obj = new MyTestObject();
            obj.setRawString(value);
            return obj;
        }

    }

    public static class MyTestObjectListConverter implements Converter<List<MyTestObject>> {

        /** {@inheritDoc} */
        @Override
        public List<MyTestObject> convert(String value) {
            List<MyTestObject> list = new LinkedList<>();
            String[] values = value.split(",");
            for (String v : values) {
                MyTestObject obj = new MyTestObject();
                obj.setRawString(v);
                list.add(obj);
            }
            return list;
        }

    }
}
