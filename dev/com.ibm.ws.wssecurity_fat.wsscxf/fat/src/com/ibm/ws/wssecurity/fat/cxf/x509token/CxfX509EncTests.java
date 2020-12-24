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

import java.io.File;

import org.junit.After;
import org.junit.BeforeClass;
//Added 10/2020
import org.junit.Test;
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.AllowedFFDC;
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
public class CxfX509EncTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509EncTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509enc";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    //Added 10/2020
    protected static String portNumber = "";
    protected static String checkPort = "";
    protected static String portNumberSecure = "";
    protected static String clientHttpUrl = "";
    protected static String clientHttpsUrl = "";
    protected static String ENDPOINT_BASE = "";
    protected static String NAMESPACE_URI = "";

    @BeforeClass
    public static void setUp() throws Exception {

        //orig from CL: Commented out 10/2020
        //commonSetUp(serverName, false,
        //            "/x509encclient/CxfX509EncSvcClient");

        //Added 10/2020
        ShrinkHelper.defaultDropinApp(server, "x509encclient", "com.ibm.ws.wssecurity.fat.x509encclient", "test.wssecfvt.x509enc", "test.wssecfvt.x509enc.types");
        ShrinkHelper.defaultDropinApp(server, "x509enc", "com.ibm.ws.wssecurity.fat.x509enc");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");

        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);
        commonSetUp(serverName, false, "/x509encclient/CxfX509EncSvcClient");
        portNumber = "" + server.getHttpDefaultPort();
        clientHttpUrl = "http://localhost:" + portNumber +
                        "/x509encclient/CxfX509EncSvcClient";
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
    public void testCXFClientEncryptUsernameToken() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientEncryptUsernameToken",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc1 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, username token is encrypted with Basic128Rsa15 algorithm suite.
     * There is no ws-security applied to the response message.
     * Uses XPath to define encryption
     * This is a positive scenario.
     *
     */
    //@Test
    public void testCXFClientEncryptUsernameTokenUsingXPath() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientEncryptUsernameTokenUsingXPath",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService1X",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc1X",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc1X Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //11/2020 For the following 3 negative scenarios, they use an incorrect wsdl file to force the error msg; for example,
    //some comment lines are added to the wsdl and/or incorrect 'sp' value <sp:SupportingTokens> vs <sp:EncryptedSupportingTokens>
    //To view the incorrect wsdl, see open-liberty\dev\com.ibm.ws.wssecurity_fat.cxf\override\autoFVT\cxfclient-policies\X509XmlEnc.wsdl

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, a plain username token is sent, but the Web service provider expects
     * the username token to be encrypted. The request is expected to be rejected with an appropriate
     * exception. This is a negative scenario.
     *
     */
    @Test
    public void testCXFClientUntNotEncrypted() throws Exception {

        String thisMethod = "testCXFClientUntNotEncrypted";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEncUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Enc1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "EncryptedSupportingTokens: The received token does not match the encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, the SOAP body is signed and encrypted and the
     * username token is encrypted.
     * In the response message, the SOAP body is signed and encrypted.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFClientEncryptBodyAndUnt() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientEncryptBodyAndUnt",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService3",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc3 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, username token, with Nonce and Created, is
     * is encrypted.
     * There is no WS-Security security applied to the response message.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFClientEncryptUntNonce() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientEncryptUntNonce",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService4",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request and response messages, the SOAP Body is signed and the
     * <Signature> element is encrypted.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCxfClientEncryptSignature() throws Exception {

        genericTest(
                    // test name for logging
                    "testCxfClientEncryptSignature",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService5",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc5",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc5 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, username token is encrypted.
     * In the response message, SOAP body is encrypted.
     * In the provider's WSDL file, this scenario uses defferent WS-Security policy
     * for the request and response messages.
     * This is a positive scenario.
     *
     */
    //@Test
    public void testCXFClientEncryptUntBody() throws Exception {

        String thisMethod = "testCXFClientEncryptUntBody";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEncUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService6",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc6",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc6 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, SOAP Body is encrypted, using Algorithm suite TripleDesRsa15, but
     * the Web Service provider requires the SOAP Body to be encrypted using Algorithm suite Basic128.
     * The client request is expected to be rejected with an appropriate exception.
     * This is a negative scenario.
     *
     */
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCXFClientWrongEncKeyAlgorithm() throws Exception {

        String thisMethod = "testCXFClientWrongEncKeyAlgorithm";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc2.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEnc2Updated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Enc7",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "AsymmetricBinding: The Key transport method does not match the requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, SOAP Body is encrypted, using Algorithm suite TripleDesRsa15, but
     * the Web Service provider requires the SOAP Body to be encrypted using Algorithm suite Basic128Rsa15.
     * The client request is expected to be rejected with an appropriate exception.
     * This is a negative scenario.
     *
     */
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCXFClientWrongDataEncAlgorithm() throws Exception {

        String thisMethod = "testCXFClientWrongDataEncAlgorithm";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc2.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEnc2Updated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        genericTest(
                    // test name for logging
                    "testCXFClientWrongDataEncAlgorithm",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService8",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Enc8",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "AsymmetricBinding: The encryption algorithm does not match the requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, SOAP Body is encrypted, using Alice's public key.
     * The Web Service provider does not have access to Alice's private key to decrypt the message.
     * The client request is expected to be rejected with an appropriate exception.
     * This is a negative scenario.
     *
     */
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCXFClientWrongEncryptionKey() throws Exception {

        String thisMethod = "testCXFClientWrongEncryptionKey";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        //orig:
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wrongEnc.xml");
        //Added 11/2020
        //UpdateServerXml reconfigServerObj = new UpdateServerXml();
        //reconfigServerObj.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wrongEnc.xml");
        //Added 11/2020
        //newReconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wrongEnc.xml");
        printMethodName(thisMethod, "End Prep for " + thisMethod);

        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService9",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc9",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "The signature or decryption was invalid",
                    // msg to issue if do NOT get the expected result
                    "The test expected an exception from the server.");

        printMethodName(thisMethod, "Start Cleanup for " + thisMethod);
        //orig:
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        //Added 11/2020
        //reconfigServerObj.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        printMethodName(thisMethod, "End Cleanup for " + thisMethod);

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * In the request message, SOAP Body is signed and encrypted, with Encryption done first before signing.
     * The WS-Security policy on the provider side is configured with the assertion <sp:EncryptBeforeSigning/>
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFClientEncryptionBeforeSign() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientEncryptionBeforeSign",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlEncService10",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Enc10",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509EncWebSvc10 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    public String updateClientWsdl(String origClientWsdl,
                                   String updatedClientWsdl) {

        try {
            if (portNumber.equals(defaultHttpPort)) {
                Log.info(thisClass, "updateClientWsdl", "Test should use the original client wsdl " + origClientWsdl + " as the client WSDL");
                return origClientWsdl;
            } else { // port number needs to be updated
                newWsdl = new UpdateWSDLPortNum(origClientWsdl, updatedClientWsdl);
                newWsdl.updatePortNum(defaultHttpPort, portNumber);
                Log.info(thisClass, "updateClientWsdl", "Test should use the updated client wsdl " + updatedClientWsdl + " as the client WSDL");

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
            restoreServer();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
