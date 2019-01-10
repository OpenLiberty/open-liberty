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
package com.ibm.ws.microprofile.config.classloader.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.test.AbstractConfigTest;

public class ConfigCacheTest extends AbstractConfigTest {

    @Test
    public void testConfigCache() {
        Config configA = null;
        Config configB = null;
        try {
            configA = ConfigProviderResolver.instance().getConfig();
            configB = ConfigProviderResolver.instance().getConfig();
            assertTrue(configA == configB);
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
            if (configB != null && configB != configA) {
                ConfigProviderResolver.instance().releaseConfig(configB);
            }
        }
    }

    @Test
    public void testSetConfig() {
        Config configA = null;
        Config configB = null;
        try {
            configA = ConfigProviderResolver.instance().getBuilder().build();
            ConfigProviderResolver.instance().registerConfig(configA, Thread.currentThread().getContextClassLoader());
            configB = ConfigProviderResolver.instance().getConfig();
            assertEquals(configA, configB);
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
            if (configB != null && configB != configA) {
                ConfigProviderResolver.instance().releaseConfig(configB);
            }
        }
    }

    @Test
    public void testReleaseConfig() {
        Config configA = null;
        Config configB = null;
        try {
            configA = ConfigProviderResolver.instance().getBuilder().build();
            ConfigProviderResolver.instance().registerConfig(configA, Thread.currentThread().getContextClassLoader());
            ConfigProviderResolver.instance().releaseConfig(configA);
            configB = ConfigProviderResolver.instance().getConfig();
            assertNotSame(configA, configB);
            try {
                configA.getPropertyNames();
                fail("Released config not closed");
            } catch (IllegalStateException e) {
                // Expected
            }
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
            if (configB != null && configB != configA) {
                ConfigProviderResolver.instance().releaseConfig(configB);
            }
        }
    }

    @Test
    public void testReleaseUnregisteredConfig() {
        Config configA = null;
        try {
            configA = ConfigProviderResolver.instance().getBuilder().build();
            ConfigProviderResolver.instance().releaseConfig(configA);
            try {
                configA.getPropertyNames();
                fail("Released config not closed");
            } catch (IllegalStateException e) {
                // Expected
            }
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
        }
    }

    @Test
    public void testSetConfigException() {
        Config configA = null;
        Config configB = null;
        try {
            configA = ConfigProviderResolver.instance().getConfig();
            configB = ConfigProviderResolver.instance().getBuilder().build();
            ConfigProviderResolver.instance().registerConfig(configB, Thread.currentThread().getContextClassLoader());
            fail("Exception not thrown");
        } catch (IllegalStateException e) {
            //expected
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
            if (configB != null && configB != configA) {
                ConfigProviderResolver.instance().releaseConfig(configB);
            }
        }
    }

    @Test
    public void testConfigClassLoader() {
        Config configA = null;
        Config configB = null;
        try {
            configA = ConfigProviderResolver.instance().getConfig();

            ClassLoader cl = new MyClassLoader(Thread.currentThread().getContextClassLoader());
            configB = ConfigProviderResolver.instance().getConfig(cl);

            assertFalse(configA == configB);
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
            if (configB != null) {
                ConfigProviderResolver.instance().releaseConfig(configB);
            }
        }
    }

    private static class MyClassLoader extends ClassLoader {
        public MyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
