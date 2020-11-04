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
package com.ibm.ws.microprofile.appConfig.stress.test;

import static org.junit.Assert.fail;

import java.util.Iterator;

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
public class StressTestServlet extends FATServlet {

    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    @Test
    public void testLargeConfigSources() throws Exception {
        int size = 1000;
        Config config = setupLargeConfigSource(size);
        try {
            //env
            TestUtils.assertContains(config, "pEnv1000", "ev1000");

            for (int i = 1; i < size; i++) {

                if (i % 5 == 0) {
                    // s5 gets 0,5,10...etc.
                    TestUtils.assertContains(config, "p" + i, "5v" + i);
                } else if (i % 3 == 0) {
                    // s3 gets 0,3,6...
                    TestUtils.assertContains(config, "p" + i, "3v" + i);
                } else {
                    // s1 gets everything.... 0,1,2...
                    TestUtils.assertContains(config, "p" + i, "1v" + i);
                }

            }

        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    private static Config setupLargeConfigSource(int size) {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource();
        s1.setSleep(1); //TODO play with this more
        MySource s3 = new MySource();
        MySource s5 = new MySource();

        s1.setOrdinal(600);
        s3.setOrdinal(700);
        s5.setOrdinal(800);

        for (int i = 0; i < size; i++) {

            // s1 gets All - 1K
            s1.put("p" + i, "1v" + i);

            // s3 gets 0,3,6,9...
            if (i % 3 == 0) {
                s3.put("p" + i, "3v" + i);
            }

            // s5 gets 0,5,10...
            if (i % 5 == 0) {
                s5.put("p" + i, "5v" + i);
            }

        }

        b.withSources(s1, s3);
        b.withSources(s5);

        b.addDefaultSources(); //server.env has 1K entries like  p4=e4
        //b.addDiscoveredSources(); // p2=2v2 and p8=4v8

        Config c = b.build();
        return c;
    }

    @Test
    public void testLargeDynamicUpdates() throws Exception {
        Config config = setupDynamicConfigSource();

        try {
            // Make sure the refresh interval has passed
            Thread.sleep(1500);

            //0-999, result should be even is 3xxx, odd is 1xxx
            // 0=30, 1=11, 2=32, 3=13, 4=34 ... 998=3998, 999=1999
            for (int i = 0; i < 1000; i++) {
                if (i % 2 == 0) {
                    TestUtils.assertContains(config, "" + i, "3" + i);
                } else {
                    TestUtils.assertContains(config, "" + i, "1" + i);
                }
            }
            //1000-1999, result should be even is 3xxx, odd is null
            // 1000=31000, 1001=null ... 1997=null, 1998=31998, 1999=null
            for (int i = 1000; i < 2000; i++) {
                if (i % 2 == 0) {
                    TestUtils.assertContains(config, "" + i, "3" + i);
                } else {
                    TestUtils.assertNotContains(config, "" + i);
                }
            }
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    private static Config setupDynamicConfigSource() {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 1);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        MySource s1 = new MySource();

        s1.setOrdinal(600);
        // set all numbers to 1xxx
        // 0=10, 1=11, 2=12, 3=13 ... 999=1999
        for (int i = 0; i < 1000; i++) {
            s1.put("" + i, "1" + i);
        }

        MySource s2 = new MySource();
        s2.setOrdinal(700);
        // set even numbers to 2xxx (odd numbers should be left as 1xxx)
        // 0=20, 2=22, 4=24 ... 998=2998
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                s2.put("" + i, "2" + i);
            }
        }

        b.withSources(s1, s2);
        Config c = b.build();

        //update even numbers to 3xxx ... shouldn't take effect until the source has been refreshed
        // 0=30, 2=32, 4=34 ... 1998=31998
        for (int i = 0; i < 2000; i++) {
            if (i % 2 == 0) {
                s2.put("" + i, "3" + i);
            }
        }

        return c;

    }

    @Test
    public void testManyConfigSources() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        // Add 500 sources each with 3 parms overriding and overridden
        int size = 500;
        for (int i = 0; i < size; i++) {
            MySource s = new MySource();
            s.setOrdinal(1000 + i);
            s.put("p" + i, "s" + i);
            s.put("p" + (i - 1), "s-1" + (i));
            s.put("p" + (i + 1), "s+1" + (i));
            s.put("" + i + "p", "v" + i);
            b.withSources(s);
        }

        Config c = b.build();

        String msg = "ok";
        boolean passed = true;

        // most values should come from the '-1' clause above.
        for (int i = 1; i < size - 1; i++) {
            if (!c.getValue("" + i + "p", String.class).equals("v" + i) ||
                !c.getValue("p" + i, String.class).equals("s-1" + Integer.valueOf(i + 1))) {
                passed = false;
                msg = "Failed for i=" + i;
                break;
            }
        }

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: " + msg);
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }

    @Test
    public void testRegistrationDeregistration() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        boolean passed = true;

        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        int size = 1000;
        for (int i = 0; i < size; i++) {
            ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());

            MySource s = new MySource();
            s.put("p" + i, "v" + i);
            b.withSources(s);
            Config configA = b.build();
            ConfigProviderResolver.instance().registerConfig(configA, Thread.currentThread().getContextClassLoader());
            ConfigProviderResolver.instance().releaseConfig(configA);

            Config configB = b.withSources(new MySource().put("p2" + i, "v2" + i)).build();
            ConfigProviderResolver.instance().registerConfig(configB, Thread.currentThread().getContextClassLoader());

            Config configB2 = ConfigProvider.getConfig();
            if (configB2 != configB) {
                passed = false;
                break;
            }
            ConfigProviderResolver.instance().releaseConfig(configB2);

            Config configC = b.withSources(new MySource().put("p2" + i, "v2" + i)).build();

            ConfigBuilder emptyB = ConfigProviderResolver.instance().getBuilder();
            Config configD = emptyB.build();
            ConfigProviderResolver.instance().registerConfig(configD, Thread.currentThread().getContextClassLoader());

            // Should be empty.
            Config configE = ConfigProvider.getConfig();
            Iterable<String> names = configE.getPropertyNames();
            for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                passed = false;
                break;
            }

            if (!passed) {
                break;
            }
        }

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Config c = ConfigProvider.getConfig();
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }
}