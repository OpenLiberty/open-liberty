/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.noCDI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.matchers.Matchers;

@SuppressWarnings("serial")
@WebServlet("/noCDITestServlet")
public class NoCDITestServlet extends FATServlet {

    public static final String NO_CDI_TEST_KEY = "aKey";
    public static final String NO_CDI_TEST_VALUE = "aValue";

    public Config config;

    public NoCDITestServlet() {
        config = ConfigProvider.getConfig();
    }

    // Tests mpConfig functionality works
    @Test
    public void testGetValue() {
        String testProperty = config.getValue(NO_CDI_TEST_KEY, String.class);
        assertEquals("the Config Value for " + NO_CDI_TEST_KEY + " should be able to be retrieved programmatically", NO_CDI_TEST_VALUE, testProperty);
    }

    // Tests mpConfig-2.0 specific functionality works
    @Test
    public void testConfigValue() {
        ConfigValue testConfigValue = config.getConfigValue(NO_CDI_TEST_KEY);
        assertNotNull("testConfigValue should be a Config Value object for the Config Property " + NO_CDI_TEST_KEY, testConfigValue);

        String testConfigValueSource = testConfigValue.getSourceName();
        assertThat("The Config Source of " + NO_CDI_TEST_KEY + " should be META-INF/microprofile-config.properties", testConfigValueSource,
                   Matchers.containString("microprofile-config.properties"));
    }

    // Tests mpConfig-2.0 specific CDI functionality doesn't work
    @Inject
    @ConfigProperty(name = NO_CDI_TEST_KEY)
    String myConfigProperty;

    @Test
    public void testCDINotWorking() {
        assertNull("Since CDI is not enabled, Injecting a Config Property should resolve to Null", myConfigProperty);
    }

}