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
package com.ibm.ws.microprofile.config.statik.test;

import static com.ibm.ws.microprofile.config.TestUtils.assertContains;
import static com.ibm.ws.microprofile.config.TestUtils.assertNotContains;
import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.config.dynamic.test.TestDynamicConfigSource;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class StaticSourceTest {

    @Test
    public void testStaticSource() {

        String key1 = "key1";
        String value1 = "value1";
        String key2 = "key2";
        String value2 = "value2";

        //create a config with non-refreshed source
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource configSource = new TestDynamicConfigSource();
        configSource.put(key1, value1);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(configSource);
        Config config = builder.build();

        //check that initially that is all we can see
        assertContent(config, key1, value1, key2);

        //add a second entry
        configSource.put(key2, value2);

        //should not show up
        assertContent(config, key1, value1, key2);

        //remove the first entry
        configSource.remove(key1);

        //still should not have changed
        assertContent(config, key1, value1, key2);

    }

    private void assertContent(Config config, String key1, String value1, String key2) {
        Iterable<String> keys = config.getPropertyNames();

        assertContains(keys, key1);

        String value = config.getValue(key1, String.class);
        assertEquals(value1, value);

        assertNotContains(keys, key2);
    }

    @After
    public void resetRefresh() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "");
    }
}
