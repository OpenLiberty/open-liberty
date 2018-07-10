/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.variableServerXML.web;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class VariableServerXMLBean {

    @Inject
    @ConfigProperty(name = "changeable_variable")
    String changeableVariable;

    @Inject
    @ConfigProperty(name = "string_variable")
    String stringVariable;

    @Inject
    @ConfigProperty(name = "int_variable")
    Integer intVariable;

    @Inject
    @ConfigProperty(name = "bool_variable")
    Boolean booleanVariable;

    @Inject
    @ConfigProperty(name = "multi_string_variable")
    String[] multiStringVariable;

    @Inject
    @ConfigProperty(name = "serverXMLKey1")
    String orderVariable1;

    @Inject
    @ConfigProperty(name = "serverXMLKey2")
    String orderVariable2;

    @Inject
    @ConfigProperty(name = "serverXMLKey3")
    String orderVariable3;

    @Inject
    @ConfigProperty(name = "serverXMLKey4")
    String orderVariable4;

    @Inject
    @ConfigProperty(name = "serverXMLChangeableKey")
    String changeableAppProperty;

    /**
     * Just a basic test that the key/value pair exists in the server.xml
     *
     * @throws Exception
     */
    public void varPropertiesBaseTest() throws Exception {
        assertEquals("string_value", stringVariable);
        assertEquals(Integer.valueOf(999), intVariable);
        assertEquals(Boolean.TRUE, booleanVariable);
        assertEquals("first", multiStringVariable[0]);
        assertEquals("second", multiStringVariable[1]);
        assertEquals("third", multiStringVariable[2]);
    }

    public void varPropertiesOrderTest() throws Exception {
        assertEquals("valueinAppProperties1", orderVariable1);
        assertEquals("valueinServerXMLVariable2", orderVariable2);
        assertEquals("valueinAppProperties3", orderVariable3);
        assertEquals("valueinAppProperties4", orderVariable4);
    }

    /**
     * Check the original variable value.
     *
     * @throws Exception
     */
    public void varPropertiesBeforeTest() throws Exception {
        assertEquals("original_value", changeableVariable);
    }

    /**
     * Check the variable value after the server.xml has been refreshed.
     *
     * @throws Exception
     */
    public void varPropertiesAfterTest() throws Exception {
        assertEquals("updated_value", changeableVariable);
    }

    /**
     * Check the original app property value.
     *
     * @throws Exception
     */
    public void appPropertiesBeforeTest() throws Exception {
        assertEquals("originalvalueinAppProperties", changeableAppProperty);
    }

    /**
     * Check the app property value after the server.xml has been refreshed.
     *
     * @throws Exception
     */
    public void appPropertiesAfterTest() throws Exception {
        assertEquals("updatedvalueinAppProperties", changeableAppProperty);
    }
}
