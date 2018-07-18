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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/ServerXMLVariableServlet")
public class VariableServerXMLServlet extends FATServlet {

    @Inject
    VariableServerXMLBean bean;

    public void varPropertiesBaseTest() throws Exception {
        bean.varPropertiesBaseTest();
    }

    public void varPropertiesOrderTest() throws Exception {
        bean.varPropertiesOrderTest();
    }

    public void varPropertiesBeforeTest() throws Exception {
        bean.varPropertiesBeforeTest();
    }

    public void varPropertiesAfterTest() throws Exception {
        bean.varPropertiesAfterTest();
    }

    public void appPropertiesBeforeTest() throws Exception {
        bean.appPropertiesBeforeTest();
    }

    public void appPropertiesAfterTest() throws Exception {
        bean.appPropertiesAfterTest();
    }
}
