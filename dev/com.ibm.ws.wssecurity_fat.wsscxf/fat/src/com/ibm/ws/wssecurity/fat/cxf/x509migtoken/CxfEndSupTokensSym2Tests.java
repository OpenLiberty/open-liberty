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

package com.ibm.ws.wssecurity.fat.cxf.x509migtoken;

import org.junit.BeforeClass;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
//Added 11/2020
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;

//Added 11/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfEndSupTokensSym2Tests extends CommonTests {

    private final static Class<?> thisClass = CxfEndSupTokensSym2Tests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.endsuptokens";
//    static private String newClientWsdl = null;

    //Added 11/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "endsuptokensclient", "com.ibm.ws.wssecurity.fat.endsuptokensclient", "test.wssecfvt.endsuptokens",
                                      "test.wssecfvt.endsuptokens.types");
        ShrinkHelper.defaultDropinApp(server, "endsuptokens", "com.ibm.ws.wssecurity.fat.endsuptokens");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);
        //orig from CL:
        commonSetUp(serverName, "server_sym2.xml", true,
                    "/endsuptokensclient/CxfEndSupTokensSvcClient");
    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * X509 is specified in the EndorsingEncryptedSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    //orig from CL commented:
    //@Test
    @Test
    public void testCXFEndSupTokens2() throws Exception {

        String thisMethod = "testCXFEndSupTokens2";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpsUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    //"",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService2",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc2 Web Service",
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
//                newWsdl = new UpdateWSDLPortNum(origClientWsdl, updatedClientWsdl);
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
