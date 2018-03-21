/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.basic.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class ValueOnlyTest {

    @Test
    public void testContainsKey() {
        BasicConfigSource source1 = new BasicConfigSource(100, "Basic Source100");
        BasicConfigSource source2 = new BasicConfigSource(200, "Basic Source200");
        ValueOnlyConfigSource source3 = new ValueOnlyConfigSource(300, "Value Only Source300");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source1, source2, source3);

        source1.put("key1", "value1");
        source2.put("key2", "value2");
        source3.put("key3", "value3"); //will not be returned as a key even though source3 has a value for it

        Config config = builder.build();

        TestUtils.assertNotContainsKey(config, "key3");
    }

    @Test
    public void testCache() throws InterruptedException {
        BasicConfigSource source1 = new BasicConfigSource(100, "Basic Source100");
        BasicConfigSource source2 = new BasicConfigSource(200, "Basic Source200");
        ValueOnlyConfigSource source3 = new ValueOnlyConfigSource(300, "Value Only Source300");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source1, source2, source3);

        source1.put("key1", "value1");
        source2.put("key2", "value2");
        source3.put("key3", "value3"); //will not be returned as a key even though source3 has a value for it

        Config config = builder.build();

        assertEquals(0, source3.getValueCount);
        TestUtils.assertValue(config, "key3", "value3");
        assertEquals(1, source3.getValueCount);
        TestUtils.assertValue(config, "key3", "value3");
        assertEquals(1, source3.getValueCount); //result should be cached
        Thread.sleep(ConfigConstants.DEFAULT_DYNAMIC_REFRESH_INTERVAL * 2); //until the next update loop when the cache will be invalidated because the value doesn't exist in the full map
        TestUtils.assertValue(config, "key3", "value3");
        assertEquals(2, source3.getValueCount); //now should get from the source again
    }

    @Test
    public void testNullValue() {
        BasicConfigSource source1 = new BasicConfigSource(100, "Basic Source100");
        BasicConfigSource source2 = new BasicConfigSource(200, "Basic Source200");
        ValueOnlyConfigSource source3 = new ValueOnlyConfigSource(300, "Value Only Source300");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source1, source2, source3);

        source1.put("key1", "value1");
        source2.put("key1", "value2");
        source3.put("key1", null); //will not be returned as a value even though source3 has highest ordinal

        Config config = builder.build();

        String valueA = config.getValue("key1", String.class);
        assertEquals("value2", valueA);
    }

    @Test
    public void testNonNullValue() {
        BasicConfigSource source1 = new BasicConfigSource(100, "Basic Source100");
        BasicConfigSource source2 = new BasicConfigSource(200, "Basic Source200");
        ValueOnlyConfigSource source3 = new ValueOnlyConfigSource(300, "Value Only Source300");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source1, source2, source3);

        source1.put("key1", "value1");
        source2.put("key1", "value2");
        source3.put("key1", "value3"); //will be returned as a value event though it is not in the complete map

        Config config = builder.build();

        String valueA = config.getValue("key1", String.class);
        assertEquals("value3", valueA);
    }
}
