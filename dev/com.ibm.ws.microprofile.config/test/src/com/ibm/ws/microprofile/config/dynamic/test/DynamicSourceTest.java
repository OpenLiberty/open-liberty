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
package com.ibm.ws.microprofile.config.dynamic.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class DynamicSourceTest {

    @Test
    public void testDynamicSource() {

        long refreshInterval = 1000; //milliseconds
        long extraInterval = 1000; //milliseconds

        String key1 = "key1";
        String value1 = "value1";
        String key2 = "key2";
        String value2 = "value2";

        //create a config with a single entry and a polling interval of [refreshInterval] seconds
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + refreshInterval);
        TestDynamicConfigSource configSource = new TestDynamicConfigSource();
        configSource.put(key1, value1);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(configSource);
        Config config = builder.build();

        //check that initially that is all we can see
        Iterable<String> keys = config.getPropertyNames();
        TestUtils.assertContains(keys, key1);
        TestUtils.assertNotContains(keys, key2);

        String value = config.getValue(key1, String.class);
        assertEquals(value1, value);

        //add a second entry
        configSource.put(key2, value2);

        //... [refreshInterval] seconds won't have elapsed yet so it shouldn't show up in the config yet
        keys = config.getPropertyNames();
        TestUtils.assertContains(keys, key1);
        TestUtils.assertNotContains(keys, key2);

        //now wait [refreshInterval] seconds
        try {
            Thread.sleep(refreshInterval + extraInterval);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //now the new key should show up
        keys = config.getPropertyNames();
        TestUtils.assertContains(keys, key1);
        TestUtils.assertContains(keys, key2);

        value = config.getValue(key1, String.class);
        assertEquals(value1, value);

        value = config.getValue(key2, String.class);
        assertEquals(value2, value);

        //now remove the first key again and wait another [pollingInterval]
        configSource.remove(key1);
        try {
            Thread.sleep(refreshInterval + extraInterval);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //now only the second key should show up
        keys = config.getPropertyNames();
        TestUtils.assertNotContains(keys, key1);
        TestUtils.assertContains(keys, key2);

        value = config.getValue(key2, String.class);
        assertEquals(value2, value);
    }

    @After
    public void resetRefresh() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "");
    }
}
