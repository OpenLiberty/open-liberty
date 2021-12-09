/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v41;

import java.util.Collections;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.IdentifyException;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@AllowedFFDC("java.sql.SQLException") // simulated exceptions forced by test case
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class ErrorMappingConfigUpdateTest extends FATServletClient {

    static final String APP_NAME = "errorMappingApp";

    @Server("com.ibm.ws.jdbc.fat.v41.errorMapConfigUpdate")
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
        server.stopServer();
    }

    /**
     * Verify configuration updates that add, remove, and modify identifyException elements.
     */
    @Test
    public void testConfigUpdateIdentifyException() throws Exception {
        // confirm the behavior with the original configuration
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify1234Stale");
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify08888NotStale");

        // add: identifyException sqlState="08888" as="StaleConnection"
        ServerConfiguration config = server.getServerConfiguration();

        IdentifyException identify08888 = new IdentifyException();
        identify08888.setSqlState("08888");
        identify08888.setAs("StaleConnection");
        DataSource errorMapDS = config.getDataSources().getById("errorMapDS");
        errorMapDS.getIdentifyExceptions().add(identify08888);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify1234Stale");
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify08888Stale");

        // modify: identifyException errorCode="1234" as="StaleConnection"
        //    -->  identifyException errorCode="1234" as="Unsupported"

        IdentifyException identify1234 = errorMapDS.getIdentifyExceptions().getBy("errorCode", "1234");
        identify1234.setAs("Unsupported");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify1234NotStale");
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify08888Stale");

        // remove: identifyException sqlState="08888" as="StaleConnection"
        errorMapDS.getIdentifyExceptions().remove(identify08888);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify1234NotStale");
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify08888NotStale");

        // restore: identifyException errorCode="1234" as="Unsupported"
        //     -->  identifyException errorCode="1234" as="StaleConnection"
        identify1234.setAs("StaleConnection");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify1234Stale");
        runTest(server, APP_NAME + "/ErrorMappingTestServlet", "testIdentify08888NotStale");
    }
}
