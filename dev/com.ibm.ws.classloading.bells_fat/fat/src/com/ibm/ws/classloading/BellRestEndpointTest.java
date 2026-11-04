/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static com.ibm.ws.classloading.TestUtils.BETA_EDITION_JVM_OPTION;
import static com.ibm.ws.classloading.TestUtils.buildAndExportBellLibrary;
import static com.ibm.ws.classloading.TestUtils.removeSysProps;
import static com.ibm.ws.classloading.TestUtils.setSysProps;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpsRequest;

/**
 * Tests for {@link LibraryServiceExporter}.
 */
@SuppressWarnings("serial")
public class BellRestEndpointTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_restep_server");

    @BeforeClass
    public static void setup() throws Throwable {
        buildAndExportBellLibrary(server, "testRestEndpoint.jar", "BellEndpoint", "BellEndpoint$1");
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        stopServer();
    }

    @Before
    public void beforeTest() throws Throwable {
        // nada
    }

    @After
    public void afterTest() {
        stopServer();
    };

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    };

    static final int TimeOut = 3000;

    /**
     * Ring the BELL.
     */
    @Test
    public void testRingTheBellRestEndpoint() throws Exception
    {
        doTestRingTheBellRestEndpoint(server, Boolean.FALSE);
    }

    @Test
    public void testRingTheBellEndpoint_BETA() throws Exception
    {
        doTestRingTheBellRestEndpoint(server, Boolean.TRUE); // beta-edition
    }

    void doTestRingTheBellRestEndpoint(LibertyServer server, Boolean runAsBetaEdition) throws Exception
    {
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BETA_EDITION_JVM_OPTION, runAsBetaEdition.toString());

        try {
            setSysProps(server, props);
            server.startServer();

            if (runAsBetaEdition) {
                assertNotNull("The server should report BELL SPI Visibility and BELL Properties was invoked in beta images, but did not.",
                        server.waitForStringInLog(".*BETA: BELL SPI Visibility and BELL Properties "));

                assertNotNull("The server should report BELL SPI visibility is enabled for library 'testRestEndpoint', but did not.",
                        server.waitForStringInLog(".*CWWKL0059I: .*testRestEndpoint"));

                assertNotNull("The server should register the `RestEndpoint` impl in the 'testRestEndpoint' library referenced by the BELL, but did not.",
                        server.waitForStringInLog(".*CWWKL0050I: .*testRestEndpoint.*RestEndpoint"));

                assertNotNull("Wait for the SSL port to open",
                        server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests"));

                // fyi: the allowInsecure() call in the request builder chain configures the SSL factory
                try {
                    new HttpsRequest(server, "/ibm/api/bellEP").allowInsecure().basicAuth("you", "yourPassword").run(String.class);
                } catch (Exception ex) {
                    System.out.println("FAILED TO HIT ENDPOINT https://localhost:<secPort>/ibm/api/bellEP " + ex);
                    ex.printStackTrace(System.out);
                }

                assertNotNull("The server should log the call to BellEndpoint.handleRequest(), but did not.",
                        server.waitForStringInLog("hello DOLLY"));
            }
            else {
                assertNull("The server should not report BELL SPI Visibility and BELL Properties has been invoked in non-beta images, but did.",
                        server.waitForStringInLog(".*BETA: BELL SPI Visibility and BELL Properties has been invoked by class", TimeOut));
            }
        } finally {
            stopServer();
            removeSysProps(server, props);
        }
    }

}
