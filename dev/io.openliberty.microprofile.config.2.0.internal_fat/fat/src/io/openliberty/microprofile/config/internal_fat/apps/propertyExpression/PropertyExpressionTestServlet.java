/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.propertyExpression;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@RequestScoped
@WebServlet("/propertyExpressionTestServlet")
public class PropertyExpressionTestServlet extends FATServlet {

    public static final String PE_TEST_MP_CONFIG_PROPERTIES_KEY = "KeyFromMicroprofileConfigProperties";
    public static final String PE_TEST_MP_CONFIG_PROPERTIES_VALUE = "valueFromMicroprofileConfigProperties";

    public static final String PE_TEST_2_PLACES_KEY = "KeyDefinedIn2Places";

    @Inject
    @ConfigProperty(name = "keyFromVariableInServerXML")
    String valueFromVariableInServerXML;

    @Inject
    @ConfigProperty(name = "keyFromAppPropertyInServerXML")
    String valueFromAppPropertyInServerXML;

    @Inject
    @ConfigProperty(name = "keyFromServerXML")
    String valueFromServerXML;

    /**
     * keyFromAppPropertyInServerXML is defined as a variable in the server.xml file as "${KeyFromMicroprofileConfigProperties}"
     *
     * KeyFromMicroprofileConfigProperties is defined in the microprofile-config.properties file as "valueFromMicroprofileConfigProperties".
     *
     * Hence, by Property Expression, `valueFromVariableInServerXML` should have the value of "valueFromMicroprofileConfigProperties".
     */
    @Test
    public void testVariableInServerXMLProperyExpression() throws Exception {
        Assert.assertEquals(PE_TEST_MP_CONFIG_PROPERTIES_VALUE, valueFromVariableInServerXML);
    }

    /**
     * keyFromAppPropertyInServerXML is defined as an appProperty in the server.xml file as "${KeyFromMicroprofileConfigProperties}"
     *
     * KeyFromMicroprofileConfigProperties is defined in the microprofile-config.properties file as "valueFromMicroprofileConfigProperties".
     *
     * Hence, by Property Expression, `valueFromAppPropertyInServerXML` should have the value of "valueFromMicroprofileConfigProperties".
     */
    @Test
    public void testAppPropertyInServerXMLProperyExpression() throws Exception {
        Assert.assertEquals(PE_TEST_MP_CONFIG_PROPERTIES_VALUE, valueFromAppPropertyInServerXML);
    }

    /**
     * keyFromServerXML is defined as a variable in the server.xml file as "${KeyDefinedIn2Places}".
     *
     * KeyDefinedIn2Places is defined in the bootstrap.properties as "valueFromBootstrapProperties" and also in microprofile-config.properties as
     * "valueFromMicroprofileConfigProperties".
     *
     * Since bootstrap.properties has a higher ordinal than microprofile-config.properties (400>100), KeyDefinedIn2Places="valueFromBootstrapProperties".
     *
     * Hence, by Property Expression, `valueFromServerXML` should have the value of "valueFromBootstrapProperties".
     */
    @Test
    public void testConfigOrdinalForPropertyExpressionInServerXML() throws Exception {
        Assert.assertEquals("valueFromBootstrapProperties", valueFromServerXML);
    }
}