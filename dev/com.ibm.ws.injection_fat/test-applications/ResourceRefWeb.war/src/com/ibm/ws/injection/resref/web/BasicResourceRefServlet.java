/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.resref.web;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@Resource(name = "com.ibm.ws.injection.resref.web.BasicResourceRefServlet/JNDI_Class_Ann_DataSource", type = javax.sql.DataSource.class)
@WebServlet("/BasicResourceRefServlet")
public class BasicResourceRefServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = BasicResourceRefServlet.class.getName();

    /* Annotation targets */
    @Resource(name = "ResRef_DS")
    DataSource dsFldAnnBnd;
    DataSource dsMthdAnnBnd;

    /* XML targets */
    DataSource dsFldXMLBnd;
    DataSource dsMthdXMLBnd;

    // Annotation Method targets
    @Resource(name = "ResRef_DS")
    public void setDataSourceAnnBnd(DataSource ds) {
        dsMthdAnnBnd = ds;
    }

    // XML Method targets
    public void setDataSourceXMLBnd(DataSource ds) {
        dsMthdXMLBnd = ds;
    }

    /**
     * Tests annotation injection of a DataSource that is looked up through a
     * binding file
     */
    @Test
    public void testDataSourceAnnBindingInjection() {
        ResRefTestHelper.testDataSource(dsFldAnnBnd, "dsFldAnnBnd");
        ResRefTestHelper.testDataSource(dsMthdAnnBnd, "dsMthdAnnBnd");
    }

    /**
     * Tests XML injection of a DataSource that is looked up through a binding
     * file. Also checks that the XML Resource defined in the web.xml can be
     * looked up in the JNDI namespace and is tested.
     */
    @Test
    public void testDataSourceXMLBindingInjection() {
        ResRefTestHelper.testDataSource(dsFldXMLBnd, "dsFldXMLBnd");
        ResRefTestHelper.testJNDILookup(CLASS_NAME + "/dsFldXMLBnd");

        ResRefTestHelper.testDataSource(dsMthdXMLBnd, "dsMthdXMLBnd");
        ResRefTestHelper.testJNDILookup(CLASS_NAME + "/dsFldXMLBnd");
    }

    /**
     * Test the DataSources that were injected with the Resource that were
     * defined by class-level @Resource annotations. It also ensures those
     * resources can be found by a lookup and then tested.
     */
    @Test
    public void testDataSourceClassLevelResourceDefinition() {
        ResRefTestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Class_Ann_DataSource");
    }
}