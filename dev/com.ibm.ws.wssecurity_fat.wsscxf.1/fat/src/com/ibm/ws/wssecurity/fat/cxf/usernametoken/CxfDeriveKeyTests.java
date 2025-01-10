/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

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
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfDeriveKeyTests extends CommonTests {

    private final static Class<?> thisClass = CxfDeriveKeyTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.derived";
    static private String newClientWsdl = null;

    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        }
        if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        }

        ShrinkHelper.defaultDropinApp(server, "derivekeyclient", "com.ibm.ws.wssecurity.fat.derivekeyclient", "test.wssecfvt.derivekey", "test.wssecfvt.derivekey.types");
        ShrinkHelper.defaultDropinApp(server, "derivekey", "com.ibm.ws.wssecurity.fat.derivekey");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, true,
                    "/derivekeyclient/CxfDeriveKeySvcClient");
    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken is specified in the ProtectionToken in
     * both the client and server policies
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey1() throws Exception {

        String thisMethod = "testCXFDeriveKey1";

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
                    "DeriveKeyService1",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc1 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken is specified in the ProtectionToken in
     * both the client and server policies
     * An incorrect password is specified.
     * This is a negative scenario.
     *
     */
    @Test
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID, EE8FeatureReplacementAction.ID })
    public void testCXFDeriveKey1WrongPw() throws Exception {

        String thisMethod = "testCXFDeriveKey1WrongPw";

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
                    "notSecure",
                    // wsdl sevice that svc client code should use
                    "DeriveKeyService1",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "The signature or decryption was invalid",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken is specified in the ProtectionToken in
     * server policy. ProtectionToken is missing in the client's
     * policy.
     * This is a negative scenario.
     *
     */

    @Test
    public void testCXFDeriveKey1ClMissingPToken() throws Exception {

        String thisMethod = "testCXFDeriveKey1ClMissingPToken";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey1MissingProToken.wsdl", defaultClientWsdlLoc
                                                                                                             + "DeriveKeys/DeriveKey1MissingProTokenUpdated.wsdl");
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
                    "DeriveKeyService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "UsernameToken: The received token does not match the token inclusion requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken is specified in the ProtectionToken in
     * server policy. X509 is specified by the client.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey1X509NotUNT() throws Exception {

        String thisMethod = "testCXFDeriveKey1X509NotUNT";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey1X509.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey1X509Updated.wsdl");
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
                    "DeriveKeyService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "UsernameToken: The received token does not match the token inclusion requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken and RequireDerivedKeys are specified in the
     * ProtectionToken in both the client and server policies
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey2() throws Exception {

        String thisMethod = "testCXFDeriveKey2";

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
                    "DeriveKeyService2",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigDKPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc2 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken and RequireDerivedKeys are specified in the
     * server policy. RequireDerivedKeys is missing from the client
     * policy.
     * This is a negative scenario.
     *
     */

    @Test
    public void testCXFDeriveKey2ClMissingReqDerivedKeys() throws Exception {

        String thisMethod = "testCXFDeriveKey2ClMissingReqDerivedKeys";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey2MissingReqDerivedKey.wsdl", defaultClientWsdlLoc
                                                                                                                  + "DeriveKeys/DeriveKey2MissingReqDerivedKeyUpdated.wsdl");
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
                    "DeriveKeyService2",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigDKPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SymmetricBinding: Message fails the DerivedKeys requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * UsernameToken and RequireDerivedKeys are specified in the
     * ProtectionToken client policy. RequireDerivedKeys is missing
     * in the server policy.
     * Once 93212 issue is addressed, this should
     * fail as it is similar scenario. This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey1ReqDerivedKeysOnlyInClient() throws Exception {

        String thisMethod = "testCXFDeriveKey1ReqDerivedKeysOnlyInClient";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey1WithReqDerivedKey.wsdl", defaultClientWsdlLoc
                                                                                                               + "DeriveKeys/DeriveKey1WithReqDerivedKeyUpdated.wsdl");
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
                    "DeriveKeyService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionSigPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SymmetricBinding: Message fails the DerivedKeys requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is encrypted in the request and response.
     * UsernameToken and RequireDerivedKeys are specified in the
     * ProtectionToken in both the client and server policies
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey3() throws Exception {

        String thisMethod = "testCXFDeriveKey3";

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
                    "DeriveKeyService3",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricProtectionEncPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc3 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingTokens
     * in both the client and server policies. TransportToken is also
     * specified requiring Https in both client and server policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey4() throws Exception {

        String thisMethod = "testCXFDeriveKey4";

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
                    "DeriveKeyService4",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeyTransportEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingTokens
     * in both the client and server policies. TransportToken is also
     * specified requiring Https in the server policy. Client does not
     * specify https.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey4NoSSL() throws Exception {

        String thisMethod = "testCXFDeriveKey4NoSSL";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey4NoHttps.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey4NoHttpsUpdated.wsdl");

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
                    "DeriveKeyService4",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeyTransportEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "HttpsToken could not be asserted",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * TransportToken is specified requiring Https in both the client
     * and server policy.
     * UsernameToken is specified in the EndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey4ClAddEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey4ClAddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey4AddEncrypted.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey4AddEncryptedUpdated.wsdl");

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
                    "DeriveKeyService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "DeriveKeyTransportEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * TransportToken is specified requiring Https in both the client
     * and server policy.
     * UsernameToken is specified in the EndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey4ClAddSigned() throws Exception {

        String thisMethod = "testCXFDeriveKey4ClAddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey4AddSigned.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey4AddSignedUpdated.wsdl");

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
                    "DeriveKeyService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "DeriveKeyTransportEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * TransportToken is specified requiring Https in both the client
     * and server policy.
     * UsernameToken is specified in the EndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey4ClAddSignedEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey4ClAddSignedEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey4AddSignedEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                                + "DeriveKeys/DeriveKey4AddSignedEncryptedUpdated.wsdl");

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
                    "DeriveKeyService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "DeriveKeyTransportEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingTokens
     * in the client and server policies.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey5() throws Exception {

        String thisMethod = "testCXFDeriveKey5";

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
                    "DeriveKeyService5",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc5 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey5AddEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey5AddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey5AddEncrypted.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey5AddEncryptedUpdated.wsdl");

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
                    "DeriveKeyService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc5 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey5MissingSigned() throws Exception {

        String thisMethod = "testCXFDeriveKey5MissingSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey5MissingSigned.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey5MissingSignedUpdated.wsdl");

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
                    "DeriveKeyService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingSupportingTokens: The received token does not match the signed endorsing supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey5MissingSignedAddEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey5MissingSignedAddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey5MissingSignedAddEncrypted.wsdl",
                                         defaultClientWsdlLoc + "DeriveKeys/DeriveKey5MissingSignedAddEncryptedUpdated.wsdl");

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
                    "DeriveKeyService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingSupportingTokens: The received token does not match the signed endorsing supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingTokens
     * in the cleint and server policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey6() throws Exception {

        String thisMethod = "testCXFDeriveKey6";

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
                    "DeriveKeyService6",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc6 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey6AddSigned() throws Exception {

        String thisMethod = "testCXFDeriveKey6AddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey6AddSigned.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey6AddSignedUpdated.wsdl");

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
                    "DeriveKeyService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc6 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey6MissingEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey6MissingEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey6MissingEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                              + "DeriveKeys/DeriveKey6MissingEncryptedUpdated.wsdl");

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
                    "DeriveKeyService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "EndorsingEncryptedSupportingTokens: The received token does not match the endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey6MissingEncryptedAddSigned() throws Exception {

        String thisMethod = "testCXFDeriveKey6MissingEncryptedAddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey6MissingEncryptedAddSigned.wsdl",
                                         defaultClientWsdlLoc + "DeriveKeys/DeriveKey6MissingEncryptedAddSignedUpdated.wsdl");

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
                    "DeriveKeyService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "EndorsingEncryptedSupportingTokens: The received token does not match the endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingTokens
     * in the client andserver policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFDeriveKey7() throws Exception {

        String thisMethod = "testCXFDeriveKey7";

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
                    "DeriveKeyService7",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is DeriveKeyWebSvc7 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFDeriveKey7MissingEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey7MissingEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey7MissingEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                              + "DeriveKeys/DeriveKey7MissingEncryptedUpdated.wsdl");

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
                    "DeriveKeyService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingEncryptedSupportingTokens: The received token does not match the signed endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    // xxxxx - can't create service
    //@Test
    public void testCXFDeriveKey7MissingSigned() throws Exception {

        String thisMethod = "testCXFDeriveKey7MissingSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey7MissingSigned.wsdl", defaultClientWsdlLoc + "DeriveKeys/DeriveKey7MissingSigned.wsdl");

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
                    "DeriveKeyService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Missing Signed",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingTokens
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFDeriveKey7MissingSignedEncrypted() throws Exception {

        String thisMethod = "testCXFDeriveKey7MissingSignedEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "DeriveKeys/DeriveKey7MissingSignedEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                                    + "DeriveKeys/DeriveKey7MissingSignedEncryptedUpdated.wsdl");

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
                    "DeriveKeyService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "DeriveKeySymmetricSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingEncryptedSupportingTokens: The received token does not match the signed endorsing encrypted supporting token requirement",
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
            //Removed to resolve RTC 285305
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