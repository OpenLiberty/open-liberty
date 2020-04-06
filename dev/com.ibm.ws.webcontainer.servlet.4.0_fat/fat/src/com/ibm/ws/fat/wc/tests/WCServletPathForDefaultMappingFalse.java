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
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;

/**
 * This is a test class for the servlet-4.0 feature to test the behavior when servletPathForDefaultMapping
 * is false. Setting servletPathForDefaultMapping to false is the testing the non default behavior.
 *
 */
@RunWith(FATRunner.class)
public class WCServletPathForDefaultMappingFalse extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServletPathForDefaultMappingFalse.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_ServletPathForDefaultMapping_False");

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add ServletPathDefaultMapping.war to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), "ServletPathDefaultMapping.war", false,
                                                  "servletpathdefaultmapping.war.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("ServletPathDefaultMapping", WCServletPathForDefaultMappingFalse.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    /**
     * Test to ensure that if we set servletPathForDefaultMapping to false in the server.xml ( not the
     * default value for the servlet-4.0 feature) then a request to the default servlet will result in:
     *
     * servlet path = "" (empty string)
     * path info = / request URI - context path
     *
     * This is the incorrect behavior
     * according to the servlet specification and that is why we're making it the non default behavior
     * for the servlet-4.0 feature.
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_False() throws Exception {
        this.verifyResponse("/ServletPathDefaultMapping", "ServletPath =  PathInfo = /");
    }

    /**
     * Test to ensure that if we set servletPathForDefaultMapping to false in the server.xml ( not the
     * default value for the servlet-4.0 feature) then a request to the default servlet will result in:
     *
     * servlet path = "" (empty string)
     * path info = / request URI - context path
     *
     * This is the incorrect behavior
     * according to the servlet specification and that is why we're making it the non default behavior
     * for the servlet-4.0 feature.
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_False_2() throws Exception {
        this.verifyResponse("/ServletPathDefaultMapping/index.html", "ServletPath =  PathInfo = /index.html");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
