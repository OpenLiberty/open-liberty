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
package io.openliberty.microprofile.config.internal_fat.apps.configProperties;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ConfigPropertiesTestServlet extends FATServlet {

    // Config Property values for tests
    public static final String NO_PREFIX_TEST_KEY = "validPrefix.validkey";
    public static final String NO_PREFIX_TEST_VALUE = "value";
    public static final String CAMEL_CASE_TEST_KEY = "validPrefix.validCamelCaseKey";
    public static final String CAMEL_CASE_TEST_VALUE = "aValueFromCamelCase";

    @Inject
    @ConfigProperties
    ConfigPropertiesBean configPropertiesBeanWithoutPrefix;

    @Inject
    @ConfigProperties(prefix = "validPrefix")
    ConfigPropertiesBean configPropertiesBean;

    @Inject
    @ConfigProperties(prefix = "validPrefix")
    ConfigPropertiesEmptyBean configPropertiesEmptyBean; // Should not cause deployment exception

    @Test
    public void testConfigPropertiesWithoutPrefix() {

        String actual = configPropertiesBeanWithoutPrefix.validkey;
        assertEquals("Not defining a prefix when injecting a Config Properties Bean should inherit the prefix defined in the bean itself. The key should resolve to " + NO_PREFIX_TEST_KEY,
                     NO_PREFIX_TEST_VALUE, actual);
    }

    @Test
    public void testCamelCaseConfigProperty() {

        String actual = configPropertiesBean.validCamelCaseKey;
        assertEquals("CamelCase Config Property's should not be a problem for Config Properties Beans. The key should resolve to " + CAMEL_CASE_TEST_KEY,
                     CAMEL_CASE_TEST_VALUE, actual);
    }

}