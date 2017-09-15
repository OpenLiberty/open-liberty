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
package com.ibm.ws.microprofile.appConfig.customSources.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class TestOrdinalsCustomSources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

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
        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
        return "PASSED";
    }
}
