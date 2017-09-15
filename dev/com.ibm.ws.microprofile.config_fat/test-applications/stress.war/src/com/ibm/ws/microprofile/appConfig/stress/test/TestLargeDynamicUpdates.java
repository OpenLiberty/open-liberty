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
package com.ibm.ws.microprofile.appConfig.stress.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class TestLargeDynamicUpdates implements AppConfigTestApp {

    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {

        Config config = setupConfig();

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
        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
        return "PASSED";
    }

    private Config setupConfig() {
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
}
