/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

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
public class LibertyCXFPositivePropertiesTest {

    @Server("LibertyCXFPositivePropertiesTestServer")
    public static LibertyServer server;

    private final static Class<?> c = LibertyCXFPositivePropertiesTest.class;
    private static final int CONN_TIMEOUT = 300;

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

        server.startServer("LibertyCXFPositivePropertiesTest.log");

        server.waitForStringInLog("CWWKF0011I");

        assertNotNull("SSL service needs to be started for tests, but the HTTPS was never started", server.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKO0801E");
        }
    }

    @Test
    public void testCxfPropertyAttachmentOutputPolicy() throws Exception {

        connect("ImageServiceImplService");

        /*
         * Testing cxf.multipart.attachment property is used to skip or not the attachment output
         * If cxf.add.attachments is set to true for Inbound or Outbound messages
         * or cxf.multipart.attachment is set to false, SwAOutInterceptor setup AttachmentOutput
         * If not SwAOutInterceptor skip AttachmentOutput
         */
        assertNotNull("Property cxf.multipart.attachment is failed to be enabled",
                      server.waitForStringInTraceUsingMark("skipAttachmentOutput: getAttachments returned"));

    }

    /*
     * Testing cxf.ignore.unsupported.policy for used alternative policies
     * When this property is set to true, it prevents addition of used policies
     * into ws-policy.validated.alternatives in PolicyVerificationInInterceptor
     */
    @Test
    public void testCxfPropertyUsedAlternativePolicy() throws Exception {

        connect("ImageServiceImplService");

        assertNotNull("Property cxf.ignore.unsupported.policy is failed to be enabled to skip checking used alternative policies",
                      server.waitForStringInTraceUsingMark("WARNING: checkEffectivePolicy will not be called"));
    }

    /*
     * Testing cxf.ignore.unsupported.policy for not supported alternative policies.
     * Setting this property to true make alternative policies to be potentially supported
     */
    @Test
    public void testCxfPropertyUnsupportedPolicy() throws Exception {

        connect("ImageServiceImplServiceTwo");

        assertNotNull("Property cxf.ignore.unsupported.policy is failed to be enabled to skip checking not supported alternative policies ",
                      server.waitForStringInTraceUsingMark("WARNING: Unsupported policy assertions will be ignored"));
    }

    /*
     * Calls WSAPropertyTestServlet with parameter defining which Image Service Implementation to call
     */
    private void connect(String methodName) throws Exception {
        //Server URL is constructed to pass to servlet since getting this info from HttpServletRequest is not providing the correct one each time
        String serverURL = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();
        URL url = new URL(serverURL + "/webServiceRefFeatures/wsapolicyskip?impl=" + methodName + "&serverurl=" + serverURL);
        Log.info(c, "LibertyCXFPositivePropertiesTest",
                 "Calling Application with URL=" + url.toString());
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpsURLConnection con = (HttpsURLConnection) HttpUtils.getHttpConnection(url, HttpsURLConnection.HTTP_OK, CONN_TIMEOUT);
        con.disconnect();
    }
}
