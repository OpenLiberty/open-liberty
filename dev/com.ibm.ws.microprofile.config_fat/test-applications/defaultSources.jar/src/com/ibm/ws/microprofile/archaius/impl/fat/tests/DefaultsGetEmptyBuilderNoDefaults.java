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
package com.ibm.ws.microprofile.archaius.impl.fat.tests;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class DefaultsGetEmptyBuilderNoDefaults implements AppConfigTestApp {

    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {
        System.setProperty("MY_VALUE", "aValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        Config config = builder.build();
        try {
            TestUtils.assertNotContains(config, "MY_VALUE");
        } catch (AssertionError e) {
            return "FAILED: " + e.getMessage();
        }
        return "PASSED";
    }

}
