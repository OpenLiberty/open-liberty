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
        assertEquals("Not defining a prefix when injecting a Config Properties Bean should inherit the prefix defined in the bean itself. "
                     + "configPropertiesBeanWithoutPrefix.validkey should resolve to validPrefix.validkey and hence be aValue", "aValue",
                     configPropertiesBeanWithoutPrefix.validkey);
    }

    @Test
    public void testCamelCaseConfigProperty() {
        assertEquals("CamelCase Config Property's should not be a problem for Config Properties Beans."
                     + "configPropertiesBean.validCamelCaseKey should be aValueFromCamelCase", "aValueFromCamelCase", configPropertiesBean.validCamelCaseKey);
    }

}