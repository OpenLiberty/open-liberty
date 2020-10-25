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

package com.ibm.ws.wssecurity.fat.cxf.x509token;

//import java.io.File;

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

public class CxfX509MustUnderstandTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509MustUnderstandTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
//    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.mustunderstand";

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(serverName, false,
                    "/mustunderstandclient/CxfMustUnderstandSvcClient");
    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, username token is encrypted with Basic128Rsa15 algorithm suite.
     * There is no ws-security applied to the response message.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFClientMustUnderstand1() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientMustUnderstand1",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "MustUnderstandWebSvc1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "MustUnderstand1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is MustUnderstandWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, username token is encrypted with Basic128Rsa15 algorithm suite.
     * There is no ws-security applied to the response message.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFClientMustUnderstand2() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientMustUnderstand2",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "MustUnderstandWebSvc2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "MustUnderstand2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is MustUnderstandWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

//    public String updateClientWsdl(String origClientWsdl,
//                                   String updatedClientWsdl) {
//
//        try {
//            if (portNumber.equals(defaultHttpPort)) {
//                Log.info(thisClass, "updateClientWsdl", "Test should use " + origClientWsdl + " as the client WSDL");
//                return origClientWsdl;
//            } else { // port number needs to be updated
//                newWsdl = new UpdateWSDLPortNum(origClientWsdl,
//                                updatedClientWsdl);
//                newWsdl.updatePortNum(defaultHttpPort, portNumber);
//                Log.info(thisClass, "updateClientWsdl", "Test should use " + updatedClientWsdl + " as the client WSDL");
//
//                return updatedClientWsdl;
//            }
//        } catch (Exception ex) {
//            Log.info(thisClass, "updateClientWsdl",
//                     "Failed updating the client wsdl try using the original");
//            newWsdl = null;
//            return origClientWsdl;
//        }
//    }
//
//    @After
//    public void testTearDown() throws Exception {
//        try {
//            if (newWsdl != null) {
//                //newWsdl.removeWSDLFile();
//                newWsdl = null;
//                newClientWsdl = null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace(System.out);
//        }
//    }
}
