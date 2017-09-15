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
package com.ibm.ws.microprofile.appConfig.dynamicSources.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class TestDynamicServiceLoaderSources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

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

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                return e.getStackTrace().toString();
            }

            TestUtils.assertContains(config, "2", "updated");
            //TODO originally DiscoveredSource4 was static. we don't currently have the ability to control sources separately so it is dynamic too!
            TestUtils.assertContains(config, "4", "updated");

        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
        return "PASSED";
    }
}
