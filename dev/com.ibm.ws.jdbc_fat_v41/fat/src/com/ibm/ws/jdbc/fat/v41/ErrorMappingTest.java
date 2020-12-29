/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v41;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.v41.errormap.web.ErrorMappingTestServlet;

@RunWith(FATRunner.class)
@AllowedFFDC // allow all FFDCs because this test forces a lot of different error paths
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class ErrorMappingTest extends FATServletClient {

    static final String APP_NAME = "errorMappingApp";

    @Server("com.ibm.ws.jdbc.fat.v41.errorMap")
    @TestServlet(servlet = ErrorMappingTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive errorDriver = ShrinkHelper.buildJavaArchive("errorMappingDriver.jar", "jdbc.fat.v41.errormap.driver");
        ShrinkHelper.exportToServer(server, "derby", errorDriver);

        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.v41.errormap.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKG0058E.*identifyException", // expected by 'jdbc/invalid/noTarget'
                          "DSRA8066E.*BOGUS", // expected by testInvalidConfig_bogusTarget
                          "DSRA8067E", // expected by testInvalidConfig_noStateOrCode / testInvalidConfig_stateAndCode
                          "com\\.ibm\\.ws\\.jdbc.*CWWKE0701E" // expected by invalid datasource configs
        );
    }

    /**
     * Verify that a an <identifyException> element with no 'as' attribute defined raises an error in logs
     */
    @Test
    public void testInvalidConfig_noTarget() throws Exception {
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", testName);
        assertNotNull("Should find CWWKG0058E error message in logs indicating that the 'as' attribute is required on the <identifyException> element",
                      server.waitForStringInLog("CWWKG0058E.*identifyException.*as"));
    }

}
