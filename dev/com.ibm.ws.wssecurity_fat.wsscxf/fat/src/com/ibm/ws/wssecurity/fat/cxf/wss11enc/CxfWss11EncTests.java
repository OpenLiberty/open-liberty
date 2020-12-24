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

package com.ibm.ws.wssecurity.fat.cxf.wss11enc;

//import java.io.File;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;

//Added 10/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfWss11EncTests extends CommonTests {

    static private final Class<?> thisClass = CxfWss11EncTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wss11enc";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "wss11encclient", "com.ibm.ws.wssecurity.fat.wss11encclient", "test.wssecfvt.wss11enc", "test.wssecfvt.wss11enc.types");
        ShrinkHelper.defaultDropinApp(server, "wss11enc", "com.ibm.ws.wssecurity.fat.wss11enc");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, false, "/wss11encclient/CxfWss11EncSvcClient");
    }

    /**
     * TestDescription:
     *
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Name="xs:NCName" Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message is like:
     * <soapenv:Header><t:EncryptedData>... </t:EncryptedData><s:Security>...</s:Security>
     * </soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    @Test
    public void testCXFClientEncryptHeaderNS1() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS1",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    /**
     * TestDescription:
     *
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Name="xs:NCName" Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * <fvt:CXF_FVT_TEST xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF"></fvt:CXF_FVT_TEST>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message has EncryptedHeader for specific header element:
     * <soapenv:Header><enc:EncryptedData>...</enc:EncryptedData>
     * <fvt:CXF_FVT_TEST xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF"></fvt:CXF_FVT_TEST>
     * <s:Security>...</s:Security></soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    @Test
    public void testCXFClientEncryptHeaderNS2() throws Exception {
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS2",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "multiHeaders",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    /**
     * TestDescription:
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message is like:
     * <soapenv:Header>
     * <enc:EncryptedData>...</enc:EncryptedData><s:Security>...</s:Security>
     * </soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    @Test
    public void testCXFClientEncryptHeaderAny() throws Exception {
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderAny",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

//    @After
//    public void testTearDown() throws Exception {
//        try {
//            if (newWsdl != null) {
//                //newWsdl.removeWSDLFile();
//                newWsdl = null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace(System.out);
//        }
//    }
}
