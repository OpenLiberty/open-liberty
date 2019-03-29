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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        long refreshInterval = 500; //milliseconds
        long maxWait = 10000; //milliseconds

        String key1 = "key1";
        String value1 = "value1";

        //create a config with a single entry and a polling interval of [refreshInterval] seconds
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + refreshInterval);
        TestDynamicConfigSource configSource = new TestDynamicConfigSource();
        configSource.put(key1, value1);
        configSource.getProperties();
        WeakReference<TestDynamicConfigSource> weakSource = new WeakReference<>(configSource);

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(configSource);
        configSource = null;

        Config config = builder.build();
        //hold on to a weak reference
        WeakReference<Config> weakConfig = new WeakReference<>(config);

        long lastGetPropertiesTime = TestDynamicConfigSource.getPropertiesLastCalled;
        long start = System.nanoTime();
        System.out.println("start time (nanoTime)                                     : " + start);
        long timeoutNanoTime = start + millisToNanos(maxWait);
        // Ensure that the source is being refreshed
        System.out.println("lastGetPropertiesTime is (nanoTime)                       : " + lastGetPropertiesTime);
        System.out.println("timeout will be (nanoTime)                                : " + timeoutNanoTime + " (" + (timeoutNanoTime - start) + ")");
        while (TestDynamicConfigSource.getPropertiesLastCalled == lastGetPropertiesTime && nanoTimeRemaining(timeoutNanoTime)) {
            Thread.sleep(refreshInterval);
        }
        long end = System.nanoTime();
        System.out.println("end time (nanoTime)                                       : " + end + " (" + (timeoutNanoTime - end) + ")");
        long currentGetPropertiesTime = TestDynamicConfigSource.getPropertiesLastCalled;
        System.out.println("lastGetPropertiesTime is now (nanoTime)                   : " + currentGetPropertiesTime);
        assertTrue("Config source was not refreshed", currentGetPropertiesTime != lastGetPropertiesTime);

        // Remove references to the config and wait for it to be garbage collected
        builder = null;
        config = null;

        waitForGC(weakConfig, maxWait);
        assertNull("Config object was not garbage collected, likely memory leak", weakConfig.get());

        //once the Config has been GC'd, the Refresher should stop, releasing it and the config source to be GC'd
        waitForGC(weakSource, maxWait);
        assertNull("Config Source object was not garbage collected, likely memory leak", weakSource.get());

        // Check that once the config and the source hava been GC'd, the config source is no longer being periodically updated
        // note that this shouldn't actually be possible since you can't get an update from something which isn't there!
        lastGetPropertiesTime = TestDynamicConfigSource.getPropertiesLastCalled;
        Thread.sleep(refreshInterval * 2);
        assertTrue("Config source was refreshed after config was GC'd", TestDynamicConfigSource.getPropertiesLastCalled == lastGetPropertiesTime);

    }

    private static final long millisToNanos(long millis) {
        return millis * 1000 * 1000;
    }

    private static final boolean nanoTimeRemaining(long maxNanos) {
        return (maxNanos - System.nanoTime()) > 0;
    }

    private static final void waitForGC(WeakReference<?> weakObjectRef, long maxWaitMillis) {
        long startGcTime = System.nanoTime();
        long maxNanos = startGcTime + millisToNanos(maxWaitMillis);
        while (weakObjectRef.get() != null && nanoTimeRemaining(maxNanos)) {
            System.runFinalization();
            System.gc();
        }
    }
}
