/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.extended.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.basic.test.BasicConfigSource;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.test.AbstractConfigTest;

public class WebSphereConfigTest extends AbstractConfigTest {

    @Test
    public void testOptionalMissing() throws NoSuchFieldException, SecurityException {
        BasicConfigSource source100 = new BasicConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Object value = config.getValue("key5", String.class, true);
        assertNull("Value should have been null: " + value, value);
    }

    @Test
    public void testMissingException() throws NoSuchFieldException, SecurityException {
        BasicConfigSource source100 = new BasicConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        try {
            Object value = config.getValue("key5", String.class, false);
            assertNull("Value should have been null: " + value, value);
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {
            //expected
        }
    }

    @Test
    public void testOptionalDefault() throws NoSuchFieldException, SecurityException {
        BasicConfigSource source100 = new BasicConfigSource(100, "Basic Source 100");

        source100.put("key1", "value1");
        source100.put("key2", "value2");
        source100.put("key3", "value3");
        source100.put("key4", "value4");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source100);

        WebSphereConfig config = (WebSphereConfig) builder.build();

        Object value = config.getValue("key5", String.class, "defaultValue");
        assertEquals("defaultValue", value);
    }
}
