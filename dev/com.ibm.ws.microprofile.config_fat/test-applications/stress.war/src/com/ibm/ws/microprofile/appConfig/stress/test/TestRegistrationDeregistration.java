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

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 *
 */
public class TestRegistrationDeregistration implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        boolean passed = true;

        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        //this size used to be 1000 but the http request was timing out ... so change it to 100 and call it 10 times!
        int size = 100;
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
            for (Iterator iterator = names.iterator(); iterator.hasNext();) {
                passed = false;
                break;
            }

            if (!passed) {
                break;
            }
        }

        if (passed) {
            return "PASSED";
        } else {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Config c = ConfigProvider.getConfig();
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            return result.toString();
        }

    }
}
