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
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import static org.junit.Assert.fail;

import java.net.URL;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig20EE8;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ClassLoadersTestServlet extends FATServlet {
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
        } catch (IllegalStateException e) {
            //expected
            TestUtils.assertEquals("CWMCG0001E: Config is closed.", e.getMessage());
        }
    }

    /**
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.util.ServiceConfigurationError" })
    @SkipForRepeat(RepeatConfig20EE8.ID) //temporarily disabled for MP Config 2.0
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
            TestUtils.assertEquals("CWMCG0012E: Unable to discover Converters. The exception is: java.util.ServiceConfigurationError: org.eclipse.microprofile.config.spi.Converter: Error locating configuration files.",
                                   e.getMessage());
        }
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testMultiUrlResources() throws Exception {
        // To test this we simply check that multiple
        // microprofile-config.properties are being
        // accessed.
        Config c = ConfigProvider.getConfig();

        boolean passed = true;

        boolean metainf = c.getValue("jar", String.class).equals("jarset");
        boolean webinf = c.getValue("web-inf.classes.meta-inf.property", String.class).equals("wiFound");

        passed = metainf && webinf;

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }
}