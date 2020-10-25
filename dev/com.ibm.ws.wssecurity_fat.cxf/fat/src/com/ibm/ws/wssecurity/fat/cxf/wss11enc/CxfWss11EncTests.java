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

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

import org.junit.Test;

import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;

public class CxfWss11EncTests extends CommonTests {

    static private final Class<?> thisClass = CxfWss11EncTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wss11enc";

    @BeforeClass
    public static void setUp() throws Exception {
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
