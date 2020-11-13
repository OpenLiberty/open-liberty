/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test is to verify the custom binding file could override Endpoint address and EJB based Web Services context root.
 */

public class WsBndEndpointOverrideTest extends WsBndEndpointOverrideTest_Lite {

    @Server("EJBinWarOverrideServer")
    public static LibertyServer EJBinWarOverrideServer;

    @Server("WebEndpointAddressOverrideServer")
    public static LibertyServer WebEndpointAddressOverrideServer;

    @BeforeClass
    public static void setUp() throws Exception {
        WsBndEndpointOverrideTest_Lite.setUp();

        JavaArchive jar = ShrinkHelper.buildJavaArchive("helloEJBServer", "com.ibm.ws.jaxws.test.hello.ejb.server");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "helloEJBApp.ear");
        ear.addAsModule(jar);
        ExplodedShrinkHelper.explodedArchiveToDestination(EJBinWarOverrideServer, ear, "apps");

        WebArchive war = ShrinkHelper.buildDefaultApp("helloServer", "com.ibm.ws.jaxws.test.wsr.server",
                                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                                      "com.ibm.ws.jaxws.test.hello.ejb.server");
        ExplodedShrinkHelper.explodedArchiveToDestination(WebEndpointAddressOverrideServer, war, "apps");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();

        if (EJBinWarOverrideServer.isStarted()) {
            EJBinWarOverrideServer.stopServer();
        }
        if (WebEndpointAddressOverrideServer.isStarted()) {
            WebEndpointAddressOverrideServer.stopServer();
        }

        EJBinWarOverrideServer.deleteFileFromLibertyServerRoot("apps/helloEJBApp.ear/helloEJBServer.jar/META-INF/ibm-ws-bnd.xml");
    }

    /**
     * TestDescription: override the default context root of EJB based Web Services using binding file.
     * Condition:
     * - A helloEJBServer.jar in helloEJBApp.ear, using loose config
     * - Config the publishing context-root in binding file: helloEJBServer.jar/META-INF/ibm-ws-bnd.xml
     * Result:
     * - response contains the port welcome message "Hello! This is a CXF Web Service!"
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testEJBWebServicesContextRootOverride() throws Exception {
        String testFilesDir = testFilesRootDir + "/testEJBWebServicesContextRootOverride";

        EJBinWarOverrideServer.copyFileToLibertyServerRoot("apps/helloEJBApp.ear/helloEJBServer.jar/META-INF", testFilesDir + "/ibm-ws-bnd.xml");

        EJBinWarOverrideServer.startServer();
        // Pause for application to start successfully
        EJBinWarOverrideServer.waitForStringInLog("CWWKT0016I.*helloEJBServer");
        EJBinWarOverrideServer.waitForStringInLog("CWWKZ0001I.*helloEJBApp");

        String result = getServletResponse(getBaseURL(EJBinWarOverrideServer) + "/hi/HelloService");
        assertTrue("Can not access the target port, the return result is: " + result, result.contains("Hello! This is a CXF Web Service!"));
    }

    /**
     * TestDescription: override the Endpoint address of EJB based Web Services using binding file.
     * Condition:
     * - A helloEJBServer.jar in helloEJBApp.ear, using loose config
     * - Config the endpoint address in binding file: helloEJBServer.jar/META-INF/ibm-ws-bnd.xml
     * Result:
     * - response contains the port welcome message "Hello! This is a CXF Web Service!"
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testEJBEndpointAddressOverride() throws Exception {
        String testFilesDir = testFilesRootDir + "/testEJBEndpointAddressOverride";

        EJBinWarOverrideServer.copyFileToLibertyServerRoot("apps/helloEJBApp.ear/helloEJBServer.jar/META-INF", testFilesDir + "/ibm-ws-bnd.xml");

        EJBinWarOverrideServer.startServer();
        // Pause for application to start successfully
        EJBinWarOverrideServer.waitForStringInLog("CWWKT0016I.*helloEJBServer");
        EJBinWarOverrideServer.waitForStringInLog("CWWKZ0001I.*helloEJBApp");

        String result = getServletResponse(getBaseURL(EJBinWarOverrideServer) + "/helloEJBServer/hi");
        assertTrue("Can not access the target port, the return result is: " + result, result.contains("Hello! This is a CXF Web Service!"));
    }

    /**
     * TestDescription: override the Endpoint address of POJO Web Services in a Web application using binding file.
     * Condition:
     * - A helloServer.war
     * - Config the endpoint address in binding file: helloServer.war/WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - response contains the port welcome message "Hello! This is a CXF Web Service!"
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testWebEndpointAddressOverride() throws Exception {
        String testFilesDir = testFilesRootDir + "/testWebEndpointAddressOverride";

        WebEndpointAddressOverrideServer.copyFileToLibertyServerRoot("apps/helloServer.war/WEB-INF", testFilesDir + "/ibm-ws-bnd.xml");

        WebEndpointAddressOverrideServer.startServer();
        // Pause for application to start successfully
        WebEndpointAddressOverrideServer.waitForStringInLog("CWWKZ0001I.*helloServer");

        String result = getServletResponse(getBaseURL(WebEndpointAddressOverrideServer) + "/helloServer/hi");
        assertTrue("Can not access the target port, the return result is: " + result, result.contains("Hello! This is a CXF Web Service!"));
    }
}
