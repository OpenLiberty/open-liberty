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

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SecureHelloWorldTest extends HelloWorldBasicTest {

    protected static final Class<?> c = SecureHelloWorldTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("SecureHelloWorldServer")
    public static LibertyServer secureHelloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(secureHelloWorldServer, "SecureHelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(secureHelloWorldServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        secureHelloWorldServer.startServer(SecureHelloWorldTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", secureHelloWorldServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
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
    public void testSecureHelloWorldWithTls() throws Exception {
        if (!checkJavaVersion()) {
            return;
        }
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
