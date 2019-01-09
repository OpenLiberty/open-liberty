/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.dynamic.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;

import java.lang.ref.WeakReference;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.test.AbstractConfigTest;

public class DynamicSourceCleanupTest extends AbstractConfigTest {

    /**
     * Test that a dynamic source stops being refreshed after it is no longer referenced
     */
    @Test
    public void testDynamicSourceCleanup() throws InterruptedException {
        long refreshInterval = 1000; //milliseconds
        long extraInterval = 1000; //milliseconds

        String key1 = "key1";
        String value1 = "value1";

        //create a config with a single entry and a polling interval of [refreshInterval] seconds
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + refreshInterval);
        TestDynamicConfigSource configSource = new TestDynamicConfigSource();
        configSource.put(key1, value1);
        configSource.getProperties();

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(configSource);
        Config config = builder.build();

        long lastGetPropertiesTime = TestDynamicConfigSource.getPropertiesLastCalled;

        // Ensure that the source is being refreshed
        Thread.sleep(refreshInterval + extraInterval);
        assertThat("Config source was not refreshed", TestDynamicConfigSource.getPropertiesLastCalled, not(equalTo(lastGetPropertiesTime)));

        // Remove references to the config and wait for it to be garbage collected
        WeakReference<Config> weakConfig = new WeakReference<>(config);
        builder = null;
        config = null;
        long startGcTime = System.nanoTime();
        long timeToWait = 10 * 1000 * 1000 * 1000; // Wait up to 10 seconds
        while (weakConfig.get() != null && System.nanoTime() - startGcTime < timeToWait) {
            System.gc();
        }

        assertNull("Config object was not garbage collected, likely memory leak", weakConfig.get());

        // Check that once the config has been GC'd, the config source is no longer being periodically updated
        Thread.sleep(refreshInterval);
        lastGetPropertiesTime = TestDynamicConfigSource.getPropertiesLastCalled;

        Thread.sleep(refreshInterval + extraInterval);
        assertThat("Config source was refreshed after config was GC'd", TestDynamicConfigSource.getPropertiesLastCalled, equalTo(lastGetPropertiesTime));

    }

}
