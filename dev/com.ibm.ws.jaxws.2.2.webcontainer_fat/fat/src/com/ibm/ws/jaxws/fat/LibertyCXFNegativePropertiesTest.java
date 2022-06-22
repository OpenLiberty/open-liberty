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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/*
 * Positive tests checking behavior changes after 2 property settings
 * Details are on top of test methods
 *
 * Usage of waitForStringInTraceUsingMark cut the runtime significantly
 */
@RunWith(FATRunner.class)
public class LibertyCXFNegativePropertiesTest {

    @Server("LibertyCXFNegativePropertiesTestServer")
    public static LibertyServer server;

    private final static Class<?> c = LibertyCXFNegativePropertiesTest.class;
    private static final int CONN_TIMEOUT = 300;

    // *** Stop and Start server between tests ***
    @BeforeClass
    public static void setUp() throws Exception {
        ExplodedShrinkHelper.explodedApp(server, "webServiceRefFeatures", "com.ibm.ws.test.client.stub",
                                         "com.ibm.ws.test.wsfeatures.client",
                                         "com.ibm.ws.test.wsfeatures.client.handler",
                                         "com.ibm.ws.test.wsfeatures.handler",
                                         "com.ibm.ws.test.wsfeatures.service");

        TestUtils.publishFileToServer(server,
                                      "WebServiceRefFeaturesTestServer", "image-property.wsdl",
                                      "apps/webServiceRefFeatures.war/WEB-INF/wsdl", "image.wsdl");

        TestUtils.publishFileToServer(server,
                                      "WebServiceRefFeaturesTestServer/client", "image.wsdl",
                                      "", "image.wsdl");

        server.startServer("LibertyCXFNegativePropertiesTestServer.log");
//        server.resetLogMarks();
        server.waitForStringInLog("CWWKF0011I");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0777E", "SRVE0315E");
        }
    }

    /*
     * Testing cxf.ignore.unsupported.policy for not supported alternative policies.
     * Not setting this property make alternative policies to not be supported in PolicyEngineImpl
     */
    @Test
    public void testCxfUnsupportedPolicyProperty() throws Exception {

        connect("ImageServiceImplService", HttpsURLConnection.HTTP_OK);

        assertNotNull("Since cxf.ignore.unsupported.policy is not enabled, invalid alternative policies are not supported",
                      server.waitForStringInTraceUsingMark("BasicAuthentication is not supported"));

        assertNull("Since cxf.ignore.unsupported.policy is not enabled, Unsupported policy assertions won't be ignored",
                   server.waitForStringInTraceUsingMark("WARNING: Unsupported policy assertions will be ignored"));
    }

    /*
     * Testing cxf.ignore.unsupported.policy for used alternative policies
     * When this property is not set, it allows addition of used policies
     * into ws-policy.validated.alternatives in PolicyVerificationInInterceptor
     */
    @Test
    public void testCxfUsedAlternativePolicyProperty() throws Exception {

        connect("ImageServiceImplService", HttpsURLConnection.HTTP_OK);

        assertNotNull("Since cxf.ignore.unsupported.policy is not enabled, used alternative policies are not put as alternatives",
                      server.waitForStringInTraceUsingMark("Verified policies for inbound message"));

        assertNull("Since cxf.ignore.unsupported.policy is not enabled, checkEffectivePolicy will be called",
                   server.waitForStringInTraceUsingMark("WARNING: checkEffectivePolicy will not be called"));

    }

    /*
     * Testing cxf.multipart.attachment property is not set or set to false
     * it sets up the attachment data out in SwAOutInterceptor
     */
    @Test
    public void testCxfAttachmentOutputProperty() throws Exception {

        connect("ImageServiceImplService", HttpsURLConnection.HTTP_OK);

        assertNotNull("Since cxf.multipart.attachment is not enabled, ",
                      server.waitForStringInTraceUsingMark("--uuid:"));

    }

    private void connect(String methodName, int ExpectedConnection) throws Exception {
        URL url = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort()
                          + "/webServiceRefFeatures/wsapolicyskip?impl=" + methodName);
        Log.info(c, "LibertyCXFPositivePropertiesTest",
                 "Calling Application with URL=" + url.toString());
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpURLConnection con = HttpUtils.getHttpConnection(url, ExpectedConnection, CONN_TIMEOUT);
        con.disconnect();
    }
}
