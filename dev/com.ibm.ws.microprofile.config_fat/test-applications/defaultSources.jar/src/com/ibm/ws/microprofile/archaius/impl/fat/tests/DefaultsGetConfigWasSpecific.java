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
public class DefaultsGetConfigWasSpecific implements AppConfigTestApp {

    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();
        try {
            TestUtils.assertContains(config, "bootstrap.properties.appConfig", "bootstrap.properties.defaultValue");
            TestUtils.assertContains(config, "server_env_appConfig", "server.env.defaultValue");
            TestUtils.assertContains(config, "jvm_options_appConfig", "jvm.options.defaultValue");
        } catch (AssertionError e) {
            return "FAILED: " + e.getMessage();
        }
        return "PASSED";
    }
}
