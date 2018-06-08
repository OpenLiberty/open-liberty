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
package com.ibm.ws.injection.repeatable.dsdann.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.injection.repeatable.dsdann.ejb.RepeatableDSDStatefulBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BasicRepeatableDSDAnnServlet")
public class BasicRepeatableDSDAnnServlet extends FATServlet {
    private static final String CLASSNAME = BasicRepeatableDSDAnnServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Lookup the bean and call the testDS method with the jndi name provided by
     * the test method and verify it returns the expected loginTimeout value of
     * the defined DataSource
     *
     * @param jndi
     * @param loginTO
     * @throws Exception
     */
    public void getAndVerifyResult(String jndi, int loginTO) throws Exception {
        svLogger.info("--> Looking up bean...");
        RepeatableDSDStatefulBean bean = (RepeatableDSDStatefulBean) FATHelper.lookupDefaultBindingEJBJavaApp(RepeatableDSDStatefulBean.class.getName(), "RepeatableDSDAnnEJB",
                                                                                                              "RepeatableDSDStatefulBean");

        svLogger.info("--> Calling test() on the SFSB that defines the DS...");
        int result = bean.testDS(jndi);
        svLogger.info("--> result = " + result);

        assertTrue("--> Expecting the returned login timeout for the DS to be " + loginTO + ". Actual value = " + result, loginTO == result);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the
     * java:module namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDModLevel() throws Exception {
        getAndVerifyResult("java:module/ann_BasicModLevelDS", 1814);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the java:app
     * namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDAppLevel() throws Exception {
        getAndVerifyResult("java:app/ann_BasicAppLevelDS", 1819);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the
     * java:global namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testRepeatableDSDGlobalLevel() throws Exception {
        getAndVerifyResult("java:global/ann_BasicGlobalLevelDS", 1806);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the java:comp
     * namespace
     *
     * @throws Exception
     */
    @Test
    public void testRepeatableDSDCompLevel() throws Exception {
        getAndVerifyResult("java:comp/env/ann_BasicCompLevelDS", 1815);
    }
}