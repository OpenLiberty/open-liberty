/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class HelloWorldTlsTest extends HelloWorldBasicTest {

    protected static final Class<?> c = HelloWorldTlsTest.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServerTls")
    public static LibertyServer helloWorldTlsServer;

    private static final Set<String> clientAppName = Collections.singleton("HelloWorldClient");
    private static final String TLS_DEFAULT = "grpc.server.tls.default.xml";
    private static final String TLS_MUTUAL_AUTH = "grpc.server.tls.mutual.auth.xml";
    private static final String TLS_INVALID_CLIENT_TRUST_STORE = "grpc.server.tls.invalid.trust.xml";
    private static final String TLS_OUTBOUND_FILTER = "grpc.server.tls.outbound.xml";
    private static String serverConfigurationFile = TLS_DEFAULT;

    @BeforeClass
    public static void setUp() throws Exception {

        helloWorldTlsServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        ShrinkHelper.defaultDropinApp(helloWorldTlsServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(helloWorldTlsServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        helloWorldTlsServer.startServer(HelloWorldTlsTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", helloWorldTlsServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Setting serverConfigurationFile to null forces a server.xml update (when GrpcTestUtils.setServerConfiguration() is first called) on the repeat run
        // If not set to null, test failures may occur (since the incorrect server.xml could be used)
        serverConfigurationFile = null;

        // SRVE0777E: for testHelloWorldWithTlsInvalidClientTrustStore case
        // CWWKO0801E: for testHelloWorldWithTlsInvalidClientTrustStore case
        //     Unable to initialize SSL connection. Unauthorized access was denied or security settings have expired.
        //     Exception is javax.net.ssl.SSLHandshakeException: Received fatal alert: certificate_unknown
        helloWorldTlsServer.stopServer("SRVE0777E", "CWWKO0801E");
    }

    @Before
    public void preTest() {
        serverRef = helloWorldTlsServer;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /**
     * testHelloWorld() with TLS enabled.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testHelloWorldWithTls() throws Exception {
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, serverConfigurationFile, TLS_DEFAULT, clientAppName, LOG);
        String response = runHelloWorldTlsTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r2"));
    }

    /**
     * testHelloWorld() with TLS mutual authentication.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testHelloWorldWithTlsMutualAuth() throws Exception {
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, serverConfigurationFile, TLS_MUTUAL_AUTH, clientAppName, LOG);
        String response = runHelloWorldTlsTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r2"));
    }

    /**
     * testHelloWorld() an invalid client trust store configured.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    @AllowedFFDC("io.grpc.StatusRuntimeException")
    public void testHelloWorldWithTlsInvalidClientTrustStore() throws Exception {
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, serverConfigurationFile, TLS_INVALID_CLIENT_TRUST_STORE, clientAppName, LOG);
        // grpc.server.tls.invalid.trust.xml will cause the ssl channel to get restarted; we need to wait for it to come back up
        assertNotNull("CWWKO0219I.*ssl not received", helloWorldTlsServer.waitForStringInLog("CWWKO0219I.*ssl"));
        Exception clientException = null;

        try {
            runHelloWorldTlsTest();
        } catch (Exception e) {
            clientException = e;
            Log.info(c, name.getMethodName(), "exception caught: " + e);
        }
        assertTrue("An error is expected for this case", clientException != null);

        // test cleanup: restore a "good" server.xml so that we don't need to wait for the ssl channel restart in another test case
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, serverConfigurationFile, TLS_OUTBOUND_FILTER, clientAppName, LOG);
        assertNotNull("CWWKO0219I.*ssl not received", helloWorldTlsServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    /**
     * testHelloWorld() with TLS and outbound filter enabled.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testHelloWorldWithTlsFilter() throws Exception {
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, serverConfigurationFile, TLS_OUTBOUND_FILTER, clientAppName, LOG);

        String response = runHelloWorldTlsTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r2"));
    }
}
