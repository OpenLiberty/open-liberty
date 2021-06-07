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
public class CxfX509MustUnderstandTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509MustUnderstandTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
//    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.mustunderstand";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "mustunderstandclient", "com.ibm.ws.wssecurity.fat.mustunderstandclient", "fats.cxf.mustunderstand", "fats.cxf.mustunderstand.types");
        ShrinkHelper.defaultDropinApp(server, "mustunderstand", "com.ibm.ws.wssecurity.fat.mustunderstand");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

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
