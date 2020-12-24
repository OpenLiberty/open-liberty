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

import java.io.File;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
//Added 11/2020
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

//Added 11/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfEndSupTokensAsymTests extends CommonTests {

    private final static Class<?> thisClass = CxfEndSupTokensAsymTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.endsuptokens";
    static private String newClientWsdl = null;
//    static private boolean bConfigServer = false;

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

        //orig from CL
        commonSetUp(serverName, "server_asym.xml", true, "/endsuptokensclient/CxfEndSupTokensSvcClient");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * X509 is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens0() throws Exception {

        String thisMethod = "testCXFEndSupTokens0";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService0",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc0 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * X509 is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    //@Test
    public void testCXFEndSupTokens0AddEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens0";

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens0AddEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                               + "EndSupTokens/EndSupTokens0AddEncryptedUpdated.wsdl");
        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService0",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc0 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * X509 is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens0Body() throws Exception {

        String thisMethod = "testCXFEndSupTokens0Body";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService0Body",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingBodyPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc0Body Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body and Element is signed in the request and response.
     * X509 is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens0BodyElement() throws Exception {

        String thisMethod = "testCXFEndSupTokens0BodyElement";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService0BodyElement",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingBodySignElementPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc0BodyElement Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * X509 and KeyValue is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    // Not supporting KeyValue at this time - this test doesn't work
    //@Test
    public void testCXFEndSupTokens0Key() throws Exception {

        String thisMethod = "testCXFEndSupTokens0Key";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService0Key",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509EndorsingKeyPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc0Key Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * X509 is specified in the SignedEndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens1() throws Exception {

        String thisMethod = "testCXFEndSupTokens1";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpsUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService1",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509SignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc1 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * X509 is specified in the SignedEndorsingEncryptedSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */
    //@Test
    public void testCXFEndSupTokens3() throws Exception {

        String thisMethod = "testCXFEndSupTokens3";

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpsUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "EndSupTokensService3",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensX509SignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc3 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    public String updateClientWsdl(String origClientWsdl,
                                   String updatedClientWsdl) {

        try {
            if (portNumber.equals(defaultHttpPort)) {
                Log.info(thisClass, "updateClientWsdl", "Test should use " + origClientWsdl + " as the client WSDL");
                return origClientWsdl;
            } else { // port number needs to be updated
                newWsdl = new UpdateWSDLPortNum(origClientWsdl, updatedClientWsdl);
                newWsdl.updatePortNum(defaultHttpPort, portNumber);
                Log.info(thisClass, "updateClientWsdl", "Test should use " + updatedClientWsdl + " as the client WSDL");

                return updatedClientWsdl;
            }
        } catch (Exception ex) {
            Log.info(thisClass, "updateClientWsdl",
                     "Failed updating the client wsdl try using the original");
            newWsdl = null;
            return origClientWsdl;
        }
    }

    @Override
    @After
    public void endTest() throws Exception {
        try {
            if (newWsdl != null) {
                //newWsdl.removeWSDLFile();
                newWsdl = null;
                newClientWsdl = null;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    //added 11/2020
    public static void copyServerXml(String copyFromFile) throws Exception {

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "copyServerXml", "Copying: " + copyFromFile
                                                 + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

}
