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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;

public class EmptyNullStringTest {

    @Test
    public void testEmptyString() {
        HashMapConfigSource source = new HashMapConfigSource("Basic Source");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source);

        source.put("key1", "value1");
        source.put("key2", "");
        source.put("key3", null);

        Config config = builder.build();

        TestUtils.assertContains(config.getPropertyNames(), "key1");
        TestUtils.assertContains(config.getPropertyNames(), "key2");
        TestUtils.assertContains(config.getPropertyNames(), "key3");
        TestUtils.assertNotContains(config.getPropertyNames(), "key4");
    }

    @Test
    public void testNoSuchElementException() {
        HashMapConfigSource source = new HashMapConfigSource("Basic Source");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source);

        source.put("key1", "value1");
        source.put("key2", "");
        source.put("key3", null);

        Config config = builder.build();

        String value1 = config.getValue("key1", String.class);
        assertEquals("value1", value1);

        String value2 = config.getValue("key2", String.class);
        assertEquals("", value2);

        String value3 = config.getValue("key3", String.class);
        assertNull(value3);

        try {
            config.getValue("key4", String.class);
            fail("NoSuchElementException not thrown");
        } catch (NoSuchElementException e) {
            //expected
        }
    }

    @Test
    public void testOptionals() {
        HashMapConfigSource source = new HashMapConfigSource("Basic Source");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source);

        source.put("key1", "value1");
        source.put("key2", "");
        source.put("key3", null);

        Config config = builder.build();

        Optional<String> value1 = config.getOptionalValue("key1", String.class);
        assertEquals("value1", value1.get());

        Optional<String> value2 = config.getOptionalValue("key2", String.class);
        assertEquals("", value2.get());

        Optional<String> value3 = config.getOptionalValue("key3", String.class);
        assertFalse(value3.isPresent());

        Optional<String> value4 = config.getOptionalValue("key4", String.class);
        assertFalse(value4.isPresent());
    }
}
