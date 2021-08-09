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
package com.ibm.ws.injection.repeatable.dsdmix.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.injection.repeatable.dsdmix.ejb.RepeatableDSDMixedBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BasicRepeatableDSDMixServlet")
public class BasicRepeatableDSDMixServlet extends FATServlet {
    private static final String CLASSNAME = BasicRepeatableDSDMixServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Lookup the bean and call the testDS method with the jndi name provided by
     * the test method and verify it returns the expected loginTimeout value,
     * isolation level, and user of the defined DataSource
     *
     * @param jndi
     * @param expectedLTO
     * @param expectedIso
     * @param expectedUser
     * @throws Exception
     */
    public void getAndVerifyResult(String jndi, int expectedLTO, int expectedIso, String expectedUser) throws Exception {
        svLogger.info("--> Looking up bean...");
        RepeatableDSDMixedBean bean = (RepeatableDSDMixedBean) FATHelper.lookupDefaultBindingEJBJavaApp(RepeatableDSDMixedBean.class.getName(), "RepeatableDSDMixEJB",
                                                                                                        "RepeatableDSDMixedBean");

        svLogger.info("--> Calling test method on the SLSB that defines the DS...");
        boolean result = bean.testDS(jndi, expectedLTO, expectedIso, expectedUser);
        svLogger.info("--> result = " + result);

        assertTrue("--> Expecting the returned result to be true. " + "Actual value = " + result, result);
    }

    /**
     * Verify that a DS defined with some attributes defined via annotation and
     * other attributes defined via XML will be successfully created with a
     * merged set of attributes.
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDMerge() throws Exception {
        getAndVerifyResult("java:module/mix_MergeSLSBModLevelDS", 1826, 1, "dsdTesterMerge");
    }

    /**
     * Verify that a DS defined via both annotation and XML, where some of the
     * same attributes defined using the annotation are also defined via XML with
     * different values, result in the annotation attributes being overridden by
     * the values used in XML.
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDOverride() throws Exception {
        getAndVerifyResult("java:module/mix_XMLOverrideSLSBModLevelDS", 1828, 4, "dsdTesterXMLKing");
    }

    /**
     * Verify that a DS defined via annotation only is successfully created even
     * if there are other DataSources defined in combination of annotation and
     * XML and one only defined in XML.
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDAnnOnly() throws Exception {
        getAndVerifyResult("java:module/mix_AnnOnlySLSBModLevelDS", 1829, 8, "dsdTesterAnn");
    }

    /**
     * Verify that a DS defined via XML only is successfully created even if
     * there are other DataSources defined in combination of annotation and XML
     * and one only defined in annotation.
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDXMLOnly() throws Exception {
        getAndVerifyResult("java:module/mix_XMLOnlySLSBModLevelDS", 1830, 2, "dsdTesterXML");
    }
}