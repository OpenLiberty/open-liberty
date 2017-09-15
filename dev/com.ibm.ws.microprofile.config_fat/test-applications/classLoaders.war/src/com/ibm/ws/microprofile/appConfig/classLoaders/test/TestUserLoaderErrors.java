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
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 * This is testing the loadResources aspect not the caching of Config
 * with ClassLoader as the key
 */
public class TestUserLoaderErrors implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDiscoveredConverters();

        URL[] urls = new URL[1];
        try {
            urls[0] = (new java.io.File("/")).toURI().toURL();
        } catch (MalformedURLException e) {
            TestUtils.fail(e);
        }
        ClassLoader cl = new CustomClassLoaderError(urls);
        b.forClassLoader(cl);
        try {
            b.build();
            TestUtils.fail("Exception not thrown");
        } catch (RuntimeException e) {
            TestUtils.assertEquals("CWMCG0012E: Unable to discover Converters. The exception is: java.util.ServiceConfigurationError: org.eclipse.microprofile.config.spi.Converter: Error locating configuration files.",
                                   e.getMessage());
        }
        return "PASSED";
    }
}