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

package com.ibm.ws.wssecurity.fat.cxf.x509migtoken;

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
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.AllowedFFDC;
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
public class CxfEndSupTokensSymTests extends CommonTests {

    private final static Class<?> thisClass = CxfEndSupTokensSymTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.endsuptokens";
    static private String newClientWsdl = null;

    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "endsuptokensclient", "com.ibm.ws.wssecurity.fat.endsuptokensclient", "test.wssecfvt.endsuptokens",
                                      "test.wssecfvt.endsuptokens.types");
        ShrinkHelper.defaultDropinApp(server, "endsuptokens", "com.ibm.ws.wssecurity.fat.endsuptokens");
        //server.addInstalledAppForValidation("x509endsuptokens");
        //server.addInstalledAppForValidation("x509endsuptokensclient");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            commonSetUp(serverName, "server_sym.xml", true,
                        "/endsuptokensclient/CxfEndSupTokensSvcClient");
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            commonSetUp(serverName, "server_sym_wss4j.xml", true,
                        "/endsuptokensclient/CxfEndSupTokensSvcClient");
        }

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingToken
     * in both the client and server policies.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens4() throws Exception {

        String thisMethod = "testCXFEndSupTokens4";

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
                    "EndSupTokensService4",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens4ClAddEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens4ClAddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens4AddEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                               + "EndSupTokens/EndSupTokens4AddEncryptedUpdated.wsdl");
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
                    "EndSupTokensService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens4ClAddSigned() throws Exception {

        String thisMethod = "testCXFEndSupTokens4ClAddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens4AddSigned.wsdl", defaultClientWsdlLoc
                                                                                                            + "EndSupTokens/EndSupTokens4AddSignedUpdated.wsdl");
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
                    "EndSupTokensService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens4ClAddSignedEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens4ClAddSignedEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens4AddSignedEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                                     + "EndSupTokens/EndSupTokens4AddSignedEncryptedUpdated.wsdl");
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
                    "EndSupTokensService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc4 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingToken
     * in the client and server policies.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens5() throws Exception {

        String thisMethod = "testCXFEndSupTokens5";

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
                    "EndSupTokensService5",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc5 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens5AddEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens5AddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens5AddEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                               + "EndSupTokens/EndSupTokens5AddEncryptedUpdated.wsdl");
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
                    "EndSupTokensService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc5 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens5MissingSigned() throws Exception {

        String thisMethod = "testCXFEndSupTokens5MissingSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens5MissingSigned.wsdl", defaultClientWsdlLoc
                                                                                                                + "EndSupTokens/EndSupTokens5MissingSignedUpdated.wsdl");
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
                    "EndSupTokensService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingSupportingTokens: The received token does not match the signed endorsing supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens5MissingSignedAddEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens5MissingSignedAddEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens5MissingSignedAddEncrypted.wsdl",
                                         defaultClientWsdlLoc + "EndSupTokens/EndSupTokens5MissingSignedAddEncryptedUpdated.wsdl");
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
                    "EndSupTokensService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingSupportingTokens: The received token does not match the signed endorsing supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingToken
     * in the cleint and server policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens6() throws Exception {

        String thisMethod = "testCXFEndSupTokens6";

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
                    "EndSupTokensService6",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc6 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingEncryptedSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens6AddSigned() throws Exception {

        String thisMethod = "testCXFEndSupTokens6AddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens6AddSigned.wsdl", defaultClientWsdlLoc
                                                                                                            + "EndSupTokens/EndSupTokens6AddSignedUpdated.wsdl");
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
                    "EndSupTokensService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc6 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens6MissingEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens6MissingEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens6MissingEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                                   + "EndSupTokens/EndSupTokens6MissingEncryptedUpdated.wsdl");
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
                    "EndSupTokensService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "EndorsingEncryptedSupportingTokens: The received token does not match the endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the EndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens6MissingEncryptedAddSigned() throws Exception {

        String thisMethod = "testCXFEndSupTokens6MissingEncryptedAddSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens6MissingEncryptedAddSigned.wsdl",
                                         defaultClientWsdlLoc + "EndSupTokens/EndSupTokens6MissingEncryptedAddSignedUpdated.wsdl");
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
                    "EndSupTokensService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "EndorsingEncryptedSupportingTokens: The received token does not match the endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingToken
     * in the client andserver policy.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCXFEndSupTokens7() throws Exception {

        String thisMethod = "testCXFEndSupTokens7";

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
                    "EndSupTokensService7",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is EndSupTokensWebSvc7 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * SignedEndorsingSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens7MissingEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens7MissingEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens7MissingEncrypted.wsdl", defaultClientWsdlLoc
                                                                                                                   + "EndSupTokens/EndSupTokens7MissingEncryptedUpdated.wsdl");
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
                    "EndSupTokensService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingEncryptedSupportingTokens: The received token does not match the signed endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingEncryptedSupportingToken in the client policy.
     * This is a negative scenario.
     *
     */
    // xxxxx - can't create service
    //@Test
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCXFEndSupTokens7MissingSigned() throws Exception {

        String thisMethod = "testCXFEndSupTokens7MissingSigned";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens7MissingSigned.wsdl", defaultClientWsdlLoc
                                                                                                                + "EndSupTokens/EndSupTokens7MissingSigned.wsdl");
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
                    "EndSupTokensService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Missing Signed",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed and encrypted in the request and response.
     * UsernameToken is specified in the SignedEndorsingEncryptedSupportingToken
     * in the server policy. UsernameToken is specified in
     * EndorsingSupportingToken in the client policy.
     * This is a positive scenario.
     *
     */
    @Test
    public void testCXFEndSupTokens7MissingSignedEncrypted() throws Exception {

        String thisMethod = "testCXFEndSupTokens7MissingSignedEncrypted";
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "EndSupTokens/EndSupTokens7MissingSignedEncrypted.wsdl",
                                         defaultClientWsdlLoc + "EndSupTokens/EndSupTokens7MissingSignedEncryptedUpdated.wsdl");
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
                    "EndSupTokensService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    //"",
                    // wsdl port that svc client code should use
                    "EndSupTokensUNTSignedEndorsingEncryptedPort",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "SignedEndorsingEncryptedSupportingTokens: The received token does not match the signed endorsing encrypted supporting token requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

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
