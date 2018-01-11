/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 * This is testing the loadResources aspect not the caching of Config
 * with ClassLoader as the key
 */
public class TestUserClassLoaders implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDefaultSources();
        Config config = b.build();
        try {
            //customLocation should not be found in the default sources using the TCCL
            TestUtils.assertNotContains(config, "customLocation");
        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }

        //this custom classloader inserts an extra resource in the results
        ClassLoader cl = new CustomClassLoader();
        b.forClassLoader(cl);
        b.addDefaultSources();
        Config config2 = b.build();

        try {
            TestUtils.assertContains(config2, "customLocation", "foundOK");
        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config2);
        }

        try {
            config2.getPropertyNames();
        } catch (IllegalStateException e) {
            //expected
            TestUtils.assertEquals("CWMCG0001E: Config is closed.", e.getMessage());
        } catch (Throwable t) {
            TestUtils.fail(t);
        }

        return "PASSED";
    }
}