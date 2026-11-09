/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * Validates that external WS-SecurityPolicy attachment files (WEB-INF/policy-attachments-client.xml and
 * WEB-INF/policy-attachments-server.xml) are resolved independently per module in an EAR that contains
 * multiple client and provider WARs.
 *
 * The EAR contains four WARs:
 * - untBasicClient / untBasicProvider : plain UsernameToken policy (SOAPService2)
 * - untNonceClient / untNonceProvider : UsernameToken with Nonce and Created policy (SOAPService3)
 *
 * If the first module's attachment file were applied to every module, the nonce pair would fail
 * (or the basic pair would fail on the provider that requires Nonce and Created).
 */
@SkipForRepeat({ RepeatWithEE7cbh20.ID })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CXFMultiModuleUntTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat";
    static final private String EAR_NAME = "untMultiModuleApp";

    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CXFMultiModuleUntTests.class;

    private static final String EXPECTED_RESPONSE = "This is WSSECFVT CXF Web Service.";
    private static final String EXPECTED_REJECTION = "The security token could not be authenticated or authorized";

    private static String httpPortNumber = "";
    private static String basicClientUrl = "";
    private static String nonceClientUrl = "";

    private static LeakedPasswordChecker leakedPasswordChecker;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive basicClient = ShrinkHelper.buildDefaultApp("untBasicClient",
                                                              "com.ibm.ws.wssecurity.fat.untmmbasicclient",
                                                              "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        WebArchive basicProvider = ShrinkHelper.buildDefaultApp("untBasicProvider",
                                                                "com.ibm.ws.wssecurity.fat.untmmbasicprovider");
        WebArchive nonceClient = ShrinkHelper.buildDefaultApp("untNonceClient",
                                                              "com.ibm.ws.wssecurity.fat.untmmnonceclient",
                                                              "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        WebArchive nonceProvider = ShrinkHelper.buildDefaultApp("untNonceProvider",
                                                                "com.ibm.ws.wssecurity.fat.untmmnonceprovider");

        // Liberty reads permissions.xml from the application (EAR) root; the per-WAR files are not applied to EAR modules
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear");
        ear.addAsManifestResource(new File("test-applications/" + EAR_NAME + "/resources/META-INF/permissions.xml"), "permissions.xml");
        ear.addAsModule(basicClient);
        ear.addAsModule(basicProvider);
        ear.addAsModule(nonceClient);
        ear.addAsModule(nonceProvider);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        server.startServer();// check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        httpPortNumber = "" + server.getHttpDefaultPort();
        server.waitForStringInLog("port " + httpPortNumber);
        server.waitForStringInLog("CWWKZ0001I.*" + EAR_NAME);

        basicClientUrl = "http://localhost:" + httpPortNumber + "/untBasicClient/CxfUntBasicClientServlet";
        nonceClientUrl = "http://localhost:" + httpPortNumber + "/untNonceClient/CxfUntNonceClientServlet";

        leakedPasswordChecker = new LeakedPasswordChecker(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            //CWWKW0226E: The user [user1|baduser123] could not be validated.
            server.stopServer("CWWKW0226E");
        }
    }

    /**
     * Invokes the given client servlet and returns the response page text, or the exception message
     * if the request itself fails.
     */
    private String invokeClient(String thisMethod, String clientUrl, String clientType) {
        String respReceived = null;
        try {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(clientUrl);
            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", clientType);

            WebResponse response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from client " + clientUrl + ": " + respReceived);
        } catch (Exception ex) {
            respReceived = ex.getMessage();
            Log.info(thisClass, thisMethod, "Exception occurred: " + respReceived);
        }
        return respReceived;
    }

    /**
     * Basic client -> basic provider with valid credentials; plain UsernameToken policy applied in both modules.
     */
    @Test
    public void testBasicUntSuccess() throws Exception {
        String resp = invokeClient("testBasicUntSuccess", basicClientUrl, "cxf");

        assertTrue("Basic UNT client did not receive the expected response: " + resp,
                   resp.contains(EXPECTED_RESPONSE));

        leakedPasswordChecker.checkForPasswordInTrace("security</wsse:Password>");
    }

    /**
     * Basic client with a bad password is rejected by the basic provider.
     */
    @Test
    public void testBasicUntBadPassword() throws Exception {
        String resp = invokeClient("testBasicUntBadPassword", basicClientUrl, "cxfbadpswd");

        assertTrue("Basic UNT bad password was not rejected: " + resp,
                   resp.contains(EXPECTED_REJECTION));

        leakedPasswordChecker.checkForPasswordInTrace("badpswd123</wsse:Password>");
    }

    /**
     * Basic client with a bad user name is rejected by the basic provider.
     */
    @Test
    public void testBasicUntBadUsername() throws Exception {
        String resp = invokeClient("testBasicUntBadUsername", basicClientUrl, "cxfbaduser");

        assertTrue("Basic UNT bad user name was not rejected: " + resp,
                   resp.contains(EXPECTED_REJECTION));
    }

    /**
     * Nonce client -> nonce provider with valid credentials; UsernameToken with Nonce and Created
     * policy applied in both modules, independent of the basic modules in the same EAR.
     */
    @Test
    public void testNonceUntSuccess() throws Exception {
        String resp = invokeClient("testNonceUntSuccess", nonceClientUrl, "nonce1");

        assertTrue("Nonce UNT client did not receive the expected response: " + resp,
                   resp.contains(EXPECTED_RESPONSE));

        leakedPasswordChecker.checkForPasswordInTrace("security</wsse:Password>");
    }

    /**
     * Nonce client with a bad password is rejected by the nonce provider.
     */
    @Test
    public void testNonceUntBadPassword() throws Exception {
        String resp = invokeClient("testNonceUntBadPassword", nonceClientUrl, "cxfbadpswd");

        assertTrue("Nonce UNT bad password was not rejected: " + resp,
                   resp.contains(EXPECTED_REJECTION));

        leakedPasswordChecker.checkForPasswordInTrace("badpswd123</wsse:Password>");
    }

    /**
     * Nonce client with a bad user name is rejected by the nonce provider.
     */
    @Test
    public void testNonceUntBadUsername() throws Exception {
        String resp = invokeClient("testNonceUntBadUsername", nonceClientUrl, "cxfbaduser");

        assertTrue("Nonce UNT bad user name was not rejected: " + resp,
                   resp.contains(EXPECTED_REJECTION));
    }
}
