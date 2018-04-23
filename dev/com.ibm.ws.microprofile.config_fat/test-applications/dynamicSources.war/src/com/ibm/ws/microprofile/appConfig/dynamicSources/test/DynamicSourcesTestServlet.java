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
package com.ibm.ws.microprofile.appConfig.dynamicSources.test;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class DynamicSourcesTestServlet extends FATServlet {

    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /**
     * Are the polling intervals honoured
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTiming() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 1000);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource("MySource S1");
        MySource s2 = new MySource("MySource S2");

        s1.put("1", "1");
        s1.put("2", "1");
        s1.setOrdinal(600);

        s2.put("2", "2");
        s2.setOrdinal(700);

        b.withSources(s1, s2);
        Config config = b.build();

        try {
            TestUtils.assertContains(config, "1", "1");
            TestUtils.assertContains(config, "2", "2");
            TestUtils.assertNotContains(config, "server_env_property");

            s2.put("1", "updated");

            //shouldn't show up yet
            TestUtils.assertContains(config, "1", "1");
            TestUtils.assertContains(config, "2", "2");

            //wait a while
            Thread.sleep(3000);

            TestUtils.assertContains(config, "1", "updated");
            TestUtils.assertContains(config, "2", "2");

        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    /**
     * Do user sources get to change there minds?
     *
     * @throws Exception
     */
    @Test
    public void testDynamicServiceLoaderSources() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 1);
        Config config = ConfigProvider.getConfig();
        try {

            TestUtils.assertContains(config, "2", "2");
            TestUtils.assertContains(config, "4", "4");

            Iterable<ConfigSource> sources = config.getConfigSources();

            for (ConfigSource s : sources) {
                if (s instanceof DiscoveredSource2) { //this one is dynamic
                    ((DiscoveredSource2) s).put("2", "updated");
                }
                //TODO originally DiscoveredSource4 was static. we don't currently have the ability to control sources separately so it is dynamic too!
                if (s instanceof DiscoveredSource4) {
                    ((DiscoveredSource4) s).put("4", "updated");
                }
            }

            Thread.sleep(1500);

            TestUtils.assertContains(config, "2", "updated");
            //TODO originally DiscoveredSource4 was static. we don't currently have the ability to control sources separately so it is dynamic too!
            TestUtils.assertContains(config, "4", "updated");

        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    /**
     * Do user sources get to change their minds?
     *
     * @throws Exception
     */
    @Test
    public void testDynamicUserAddedSources() throws Exception {
        // Remember that the mimimum refresh is currently 500
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 1);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource("MySource S1");
        MySource s2 = new MySource("MySource S2");
        MySource s3 = new MySource("MySource S3");

        s1.setOrdinal(600);
        s2.setOrdinal(700);
        s3.setOrdinal(800);

        s1.put("1", "1");
        s1.put("2", "1");
        s1.put("3", "1");

        s2.put("2", "2");
        s2.put("3", "2");

        s3.put("3", "3");

        b.withSources(s1, s2, s3);
        b.addDefaultSources();
        Config config = b.build();

        try {
            TestUtils.assertContains(config, "1", "1");
            TestUtils.assertContains(config, "2", "2");
            TestUtils.assertContains(config, "3", "3");
            TestUtils.assertContains(config, "server_env_property", "server.env.value");

            s1.put("1", "1.2");
            s1.put("2", "1.2");
            s1.put("3", "1.2");

            s2.put("2", "2.2");
            s2.put("3", "2.2");

            s3.put("3", "3.2");

            // Remember that the mimimum refresh is currently 500 ... so wait longer than that
            Thread.sleep(1500);

            TestUtils.assertContains(config, "1", "1.2");
            TestUtils.assertContains(config, "2", "2.2");
            TestUtils.assertContains(config, "3", "3.2");
            TestUtils.assertContains(config, "server_env_property", "server.env.value");
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

}