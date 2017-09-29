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
package com.ibm.ws.microprofile.appConfig.dynamicSources.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class TestDynamicTiming implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

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

        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }

        return "PASSED";
    }
}
