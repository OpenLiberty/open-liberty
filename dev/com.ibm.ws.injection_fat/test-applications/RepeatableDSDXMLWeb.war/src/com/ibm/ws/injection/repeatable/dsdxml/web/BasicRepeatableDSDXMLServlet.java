/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.repeatable.dsdxml.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.injection.repeatable.dsdxml.ejb.RepeatableDSDStatelessBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BasicRepeatableDSDXMLServlet")
public class BasicRepeatableDSDXMLServlet extends FATServlet {
    private static final String CLASSNAME = BasicRepeatableDSDXMLServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Lookup the bean and call the testDS method with the jndi name provided by
     * the test method and verify it returns the expected loginTimeout value and
     * isolation level of the defined DataSource
     *
     * @param jndi
     * @param expectedLTO
     * @param expectedIso
     * @throws Exception
     */
    public void getAndVerifyResult(String jndi, int expectedLTO, int expectedIso) throws Exception {
        svLogger.info("--> Looking up bean...");
        RepeatableDSDStatelessBean bean = (RepeatableDSDStatelessBean) FATHelper.lookupDefaultBindingEJBJavaApp(RepeatableDSDStatelessBean.class.getName(), "RepeatableDSDXMLEJB",
                                                                                                                "RepeatableDSDStatelessBean");
        svLogger.info("--> Calling test method on the bean that defines the DS...");
        boolean result = bean.testDS(jndi, expectedLTO, expectedIso);
        svLogger.info("--> result = " + result);

        assertTrue("--> Expecting the returned result to be true. " + "Actual value = " + result, result);
    }

    /**
     * Verify that a DS defined in a SLSB using the XML data-source element can
     * be successfully looked up from the SLSB using the java:module namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDModLevel() throws Exception {
        getAndVerifyResult("java:module/BasicModLevelDS", 1842, 8);
    }

    /**
     * Verify that a DS defined in a SLSB using the XML data-source element can
     * be successfully looked up from the SLSB using the java:app namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDAppLevel() throws Exception {
        getAndVerifyResult("java:app/BasicAppLevelDS", 1822, 8);
    }

    /**
     * Verify that a DS defined in a SLSB using the XML data-source element can
     * be successfully looked up from the SLSB using the java:global namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDGlobalLevel() throws Exception {
        getAndVerifyResult("java:global/BasicGlobalLevelDS", 1832, 8);
    }

    /**
     * Verify that a DS defined in a SLSB using the XML data-source element can
     * be successfully looked up from the SLSB using the java:comp namespace
     *
     * @throws Exception
     */
    @Test
    public void testRepeatableDSDCompLevel() throws Exception {
        getAndVerifyResult("java:comp/env/BasicCompLevelDS", 1813, 8);
    }

    /**
     * Verify that a DS defined in a SLSB using both XML and annotations will
     * ignore anything set in via annotations when metadata-complete = true.
     * We expect to get the default values for the loginTimeout and Isolation
     * level properties which are 0 and 4 respectively.
     *
     */
    @Test
    public void testRepeatableDSDMetaDataCompleteValid() throws Exception {
        getAndVerifyResult("java:module/MetaDataCompleteValidDS", 0, 4);
    }

    /**
     * Verify that a DS defined ONLY via annotation will not be created when
     * metadata-complete = true.
     */
    @Test
    public void testRepeatableDSDMetaDataCompleteAnnOnly() throws Exception {
        svLogger.info("--> Looking up bean...");
        RepeatableDSDStatelessBean bean = (RepeatableDSDStatelessBean) FATHelper.lookupDefaultBindingEJBJavaApp(RepeatableDSDStatelessBean.class.getName(), "RepeatableDSDXMLEJB",
                                                                                                                "RepeatableDSDStatelessBean");
        assertTrue("--> Expected to receive result = true, actual value of result = " + bean.testInvalidDS(), bean.testInvalidDS());
    }
}