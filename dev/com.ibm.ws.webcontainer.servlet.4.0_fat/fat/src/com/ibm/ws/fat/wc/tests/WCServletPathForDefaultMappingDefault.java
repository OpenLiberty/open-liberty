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
 * is true. There is no need to set the property in the server.xml because the value is true by default
 * for the servlet-4.0 feature.
 *
 */
@RunWith(FATRunner.class)
public class WCServletPathForDefaultMappingDefault extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServletPathForDefaultMappingDefault.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_ServletPathForDefaultMapping_Default");

    @BeforeClass
    public static void setUp() throws Exception {

        LOG.info("Setup : add ServletPathDefaultMapping.war to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), "ServletPathDefaultMapping.war", false,
                                                  "servletpathdefaultmapping.war.servlets");

        SHARED_SERVER.startIfNotStarted();

        LOG.info("Setup : wait for message to indicate app has started");

        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* ServletPathDefaultMapping", 10000);

        LOG.info("Setup : complete, ready for Tests");

    }

    /**
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default() throws Exception {
        this.verifyResponse("/ServletPathDefaultMapping", "ServletPath = / PathInfo = null");
    }

    /**
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default_2() throws Exception {
        this.verifyResponse("/ServletPathDefaultMapping/index.html", "ServletPath = /index.html PathInfo = null");
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
