/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.classLoaders.test.libertyTests;

import java.net.URL;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.classLoaders.test.CustomClassLoader;
import com.ibm.ws.microprofile.appConfig.classLoaders.test.CustomClassLoaderError;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/libertyClassLoadersTestServlet")
public class LibertyClassLoadersTestServlet extends FATServlet {

    /**
     * MP Config 2.0 does not throw exceptions for calling methods on released Configs
     */
    @Test
    public void testUserClassLoaders() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDefaultSources();
        Config config = b.build();
        try {
            //customLocation should not be found in the default sources using the TCCL
            TestUtils.assertNotContains(config, "customLocation");
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
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config2);
        }

        try {
            config2.getPropertyNames();
            TestUtils.fail("Exception not thrown");
        } catch (IllegalStateException e) {
            //expected
            TestUtils.assertEquals("CWMCG0001E: Config is closed.", e.getMessage());
        }
    }

    /**
     * MP Config 2.0+ throws different error message. Test covered with testUserLoaderErrorsConfig20()
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.util.ServiceConfigurationError" })
    public void testUserLoaderErrors() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDiscoveredConverters();

        URL[] urls = new URL[1];
        urls[0] = (new java.io.File("/")).toURI().toURL();

        ClassLoader cl = new CustomClassLoaderError(urls);
        b.forClassLoader(cl);
        try {
            b.build();
            TestUtils.fail("Exception not thrown");
        } catch (RuntimeException e) {
            //expected
            TestUtils.assertEquals("CWMCG0012E: Unable to discover Converters. The exception is: java.util.ServiceConfigurationError: org.eclipse.microprofile.config.spi.Converter: Error locating configuration files.",
                                   e.getMessage());
        }
    }
}