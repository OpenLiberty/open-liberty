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
package com.ibm.ws.microprofile.appConfig.stress.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestLargeConfigSources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

        int size = 1000;
        Config config = setupConfig(size);
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

        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }

        return "PASSED";
    }

    private Config setupConfig(int size) {
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
}
