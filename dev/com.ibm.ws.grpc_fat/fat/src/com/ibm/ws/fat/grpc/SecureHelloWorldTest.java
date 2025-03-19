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
public class SecureHelloWorldTest extends HelloWorldBasicTest {

    protected static final Class<?> c = SecureHelloWorldTest.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Rule
    public TestName name = new TestName();

    @Server("SecureHelloWorldServer")
    public static LibertyServer secureHelloWorldServer;

    private static final Set<String> clientAppName = Collections.singleton("HelloWorldClient");
    private static final String SECURITY_PLAIN_TEXT = "grpc.client.security.plaintext.xml";
    private static final String DEFAULT_CONFIG_FILE = "grpc.client.security.default.xml";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    @BeforeClass
    public static void setUp() throws Exception {

        secureHelloWorldServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultDropinApp(secureHelloWorldServer, "SecureHelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(secureHelloWorldServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        secureHelloWorldServer.startServer(SecureHelloWorldTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", secureHelloWorldServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Setting serverConfigurationFile to null forces a server.xml update (when GrpcTestUtils.setServerConfiguration() is first called) on the repeat run
        // If not set to null, test failures may occur (since the incorrect server.xml could be used)
        serverConfigurationFile = null;

        // SRVE0777E for testSecureHelloWorldWOTls case
        secureHelloWorldServer.stopServer("SRVE0777E", "CWWKE1102W", "CWWKE1107W", "CWWKE1106W");
    }

    @Before
    public void preTest() {
        serverRef = secureHelloWorldServer;
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
    public void testSecureHelloWorldWithTls() throws Exception {
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(secureHelloWorldServer, serverConfigurationFile, DEFAULT_CONFIG_FILE, clientAppName, LOG);
        String response = runHelloWorldTlsTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r2"));
    }

    /**
     * testHelloWorld() without TLS when when security is enforced.
     * The server should try to redirect to the secure port and an FFDC entry should be generated
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("io.grpc.StatusRuntimeException")
    public void testSecureHelloWorldWOTls() throws Exception {
        // set <grpcClient ... usePlaintext="true" /> to disable the Liberty TLS config on the client
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(secureHelloWorldServer, serverConfigurationFile, SECURITY_PLAIN_TEXT, clientAppName, LOG);
        Exception clientException = null;
        try {
            runHelloWorldTest();
        } catch (Exception e) {
            clientException = e;
            Log.info(c, name.getMethodName(), "exception caught: " + e);
        }
        assertTrue("An error is expected for this case", clientException != null);
    }
}
