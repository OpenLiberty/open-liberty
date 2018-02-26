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

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class SourcedValueTest {

    @Test
    public void testSourceValues() {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");
        HashMapConfigSource source200 = new HashMapConfigSource(200, "Basic Source 200");
        HashMapConfigSource source300 = new HashMapConfigSource(300, "Basic Source 300");
        HashMapConfigSource source400 = new HashMapConfigSource(400, "Basic Source 400");

        source100.put("key1", "value1 (100)");
        source100.put("key2", "value2 (100)");
        source100.put("key3", "value3 (100)");
        source100.put("key4", "value4 (100)");

        source200.put("key2", "value2 (200)");
        source200.put("key3", "value3 (200)");
        source200.put("key4", "value4 (200)");

        source300.put("key3", "value3 (300)");
        source300.put("key4", "value4 (300)");

        source400.put("key4", "value4 (400)");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source300, source200, source400, source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        TestUtils.assertSource(config, "key1", "value1 (100)", "Basic Source 100");
        TestUtils.assertSource(config, "key2", "value2 (200)", "Basic Source 200");
        TestUtils.assertSource(config, "key3", "value3 (300)", "Basic Source 300");
        TestUtils.assertSource(config, "key4", "value4 (400)", "Basic Source 400");
    }

    @Test
    public void testDump() {
        HashMapConfigSource source100 = new HashMapConfigSource(100, "Basic Source 100");
        HashMapConfigSource source200 = new HashMapConfigSource(200, "Basic Source 200");
        HashMapConfigSource source300 = new HashMapConfigSource(300, "Basic Source 300");
        HashMapConfigSource source400 = new HashMapConfigSource(400, "Basic Source 400");

        source100.put("key1", "value1 (100)");
        source100.put("key2", "value2 (100)");
        source100.put("key3", "value3 (100)");
        source100.put("key4", "value4 (100)");

        source200.put("key2", "value2 (200)");
        source200.put("key3", "value3 (200)");
        source200.put("key4", "value4 (200)");

        source300.put("key3", "value3 (300)");
        source300.put("key4", "value4 (300)");

        source400.put("key4", "value4 (400)");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source300, source200, source400, source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        String dump = config.dump();

        String expected = "[Basic Source 100; class java.lang.String] key1=value1 (100)\n" +
                          "[Basic Source 200; class java.lang.String] key2=value2 (200)\n" +
                          "[Basic Source 300; class java.lang.String] key3=value3 (300)\n" +
                          "[Basic Source 400; class java.lang.String] key4=value4 (400)";

        assertEquals(expected, dump);

    }
}
