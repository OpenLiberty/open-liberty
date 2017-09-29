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
package com.ibm.ws.microprofile.appConfig.defaultSources.tests;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class DefaultsGetConfig implements AppConfigTestApp {

    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {
        Config config = ConfigProvider.getConfig();
        try {
            TestUtils.assertContains(config, "defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
            TestUtils.assertContains(config, "defaultSources.war.meta-inf.config.properties", "warPropertiesDefaultValue");
        } catch (AssertionError e) {
            return "FAILED: " + e.getMessage();
        }
        return "PASSED";
    }

}
