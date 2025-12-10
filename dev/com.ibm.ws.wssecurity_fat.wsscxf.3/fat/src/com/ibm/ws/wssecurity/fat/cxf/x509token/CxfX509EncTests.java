/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.x509token;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.annotation.MinimumJavaLevel;

@MinimumJavaLevel(javaLevel = 17)
@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfX509EncTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509EncTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509enc";
    //issue 18363
    private static String featureVersion = "";

    @Server(serverName)
    public static LibertyServer server;

    protected static String portNumber = "";
    protected static String checkPort = "";
    protected static String portNumberSecure = "";
    protected static String clientHttpUrl = "";
    protected static String clientHttpsUrl = "";
    protected static String ENDPOINT_BASE = "";
    protected static String NAMESPACE_URI = "";

    @BeforeClass
    public static void setUp() throws Exception {

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
            featureVersion = "EE7cbh1";
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
            featureVersion = "EE7cbh2";
        }

        ShrinkHelper.defaultDropinApp(server, "x509encclient", "com.ibm.ws.wssecurity.fat.x509encclient", "test.wssecfvt.x509enc", "test.wssecfvt.x509enc.types");
        ShrinkHelper.defaultDropinApp(server, "x509enc", "com.ibm.ws.wssecurity.fat.x509enc");

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
                    "The test expected a successful message from the server.");

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
    //The test case was noticed as commented out from prior CL
    //When attempting to run the test, it failed and tracked in issue 23345
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
                    "The test expected a successful message from the server.");

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
                    "The test expected a successful message from the server.");

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
                    "The test expected a successful message from the server.");

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
                    "The test expected a successful message from the server.");

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
                    "The test expected a successful message from the server.");

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
    //The test case was noticed as commented out from prior CL
    //When attempting to run the test, it failed and tracked in issue 23341
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
                    "The test expected a successful message from the server.");

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
    //issue 230606
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCXFClientWrongEncKeyAlgorithm() throws Exception {

        String thisMethod = "testCXFClientWrongEncKeyAlgorithm";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc2.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEnc2Updated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);

        //issue 230606
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
                    //issue 23060: both EE7 and EE7cbh2.0 return with same error with new jaxws-2.2
                    "An error was discovered processing the <wsse:Security> header",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

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
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCXFClientWrongDataEncAlgorithm() throws Exception {

        String thisMethod = "testCXFClientWrongDataEncAlgorithm";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlEnc2.wsdl",
                                         defaultClientWsdlLoc + "X509XmlEnc2Updated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);

        //issue 23060
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
                    //issue 23060: both EE7 and EE7cbh2.0 return with same error with new jaxws-2.2
                    "An error was discovered processing the <wsse:Security> header",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

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
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCXFClientWrongEncryptionKey() throws Exception {

        String thisMethod = "testCXFClientWrongEncryptionKey";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        printMethodName(thisMethod, "End Prep for " + thisMethod);

        //issue 23060
        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wrongEnc.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wrongEnc_wss4j.xml");
        }

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
                    //issue 23060: both EE7 and EE7cbh2.0 return with same error with new jaxws-2.2
                    "Cannot find key for alias: [alice]",
                    // msg to issue if do NOT get the expected result
                    "The test expected an exception from the server.");

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        }

        printMethodName(thisMethod, "Start Cleanup for " + thisMethod);
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

        //issue 23060
        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        }

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
                    "The test expected a successful message from the server.");

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
            //Removed to resolve RTC 285315
            //restoreServer();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

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
