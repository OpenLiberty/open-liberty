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
package com.ibm.ws.microprofile.config.extended.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.basic.test.HashMapConfigSource;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class WebSphereConfigTest {

    public static class TestTypes {
        public Optional<String> optString;
        public Optional<URL> optURL;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOptionalString() throws NoSuchFieldException, SecurityException {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Type type = TestTypes.class.getField("optString").getAnnotatedType().getType();

        Object value = config.getValue("key1", type, true);
        assertTrue(value instanceof Optional);
        Optional<String> optValue = (Optional<String>) value;
        assertTrue(optValue.isPresent());
        assertEquals("value1", optValue.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOptionalURL() throws NoSuchFieldException, SecurityException {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");

        source100.put("key1", "http://www.ibm.com/");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Type type = TestTypes.class.getField("optURL").getAnnotatedType().getType();

        Object value = config.getValue("key1", type, true);
        assertTrue(value instanceof Optional);
        Optional<URL> optValue = (Optional<URL>) value;
        assertTrue(optValue.isPresent());
        assertEquals("http://www.ibm.com/", optValue.get().toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOptionalMissing() throws NoSuchFieldException, SecurityException {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Type type = TestTypes.class.getField("optString").getAnnotatedType().getType();

        Object value = config.getValue("key5", type, true);
        assertTrue(value instanceof Optional);
        Optional<String> optValue = (Optional<String>) value;
        assertFalse(optValue.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOptionalDefault() throws NoSuchFieldException, SecurityException {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Type type = TestTypes.class.getField("optString").getAnnotatedType().getType();

        Object value = config.getValue("key5", type, "defaultValue");
        assertTrue(value instanceof Optional);
        Optional<String> optValue = (Optional<String>) value;
        assertTrue(optValue.isPresent());
        assertEquals("defaultValue", optValue.get());
    }
}
