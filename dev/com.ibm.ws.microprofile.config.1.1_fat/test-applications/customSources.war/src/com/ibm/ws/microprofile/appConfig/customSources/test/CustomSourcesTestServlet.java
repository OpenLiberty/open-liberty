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
package com.ibm.ws.microprofile.appConfig.customSources.test;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class CustomSourcesTestServlet extends FATServlet {
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /**
     * This tests proper sorting of ordinals and that this is done
     * on a per property interface (not all properties in a source
     * may be 'winning' config sources).
     *
     * What happens if two sources have the same ordinal
     *
     * @throws Exception
     */
    @Test
    public void testOrdinalsCustomSources() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource();
        MySource s2 = new MySource();
        MySource s3 = new MySource();
        NullSource s4 = new NullSource();//just returns null for everything, to check we don't blow up

        s1.setOrdinal(600);
        s2.setOrdinal(700);
        s3.setOrdinal(800);

        s1.put("1", "1");
        s1.put("2", "1");
        s1.put("3", "1");

        s2.put("2", "2");
        s2.put("3", "2");

        s3.put("3", "3");

        b.addDefaultSources();
        b.withSources(s1, s2, s3, s4);
        Config config = b.build();
        try {
            TestUtils.assertContains(config, "1", "1");
            TestUtils.assertContains(config, "2", "2");
            TestUtils.assertContains(config, "3", "3");
            TestUtils.assertContains(config, "server_env_property", "server.env.value");
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    /**
     * This tests user provided ConfigSource objects together
     * with them being interleaved with default sources and value overriding.
     *
     * @throws Exception
     */
    @Test
    public void testInterleaveCustomDefaultSources() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource();
        MySource s2 = new MySource();

// server.env has:
//          server_env_property=server.env.value
//          server_env_property_override=server.env.o.value
//          server_env_property_override2=server.env.o.value2

        s1.setOrdinal(1000);
        s1.put("test1", "test1");
        s1.put("server_env_property_override", "server.env.value.overridden");

        s2.setOrdinal(1);
        s2.put("server_env_property_override2", "server.env.value.overriddenNOT");

        b.addDefaultSources();
        b.withSources(s1, s2);

        Config c = b.build();

        if (!(c.getValue("test1", String.class).equals("test1") &&
              c.getValue("server_env_property", String.class).equals("server.env.value") &&
              c.getValue("server_env_property_override", String.class).equals("server.env.value.overridden") &&
              c.getValue("server_env_property_override2", String.class).equals("server.env.o.value2"))) {

            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }

    /**
     * Tests getConfig with ClassLoader that effects how resourceNames are loaded
     *
     * @throws Exception
     */
    @Test
    public void testServiceLoadingSources() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        Config config = ConfigProvider.getConfig();

        try {
            TestUtils.assertContains(config, "configSourceProviderS1", "s1");
            TestUtils.assertContains(config, "configSourceProviderS2", "s2");
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

}