/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.wsstemplates;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;
import java.util.Set;

import org.junit.BeforeClass;
//issue 18363
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
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
//issue 18363
public class CxfWssTemplatesTestsWithExternalPolicy extends CommonTests {

    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wsstemplateswithep";
    static private final Class<?> thisClass = CxfWssTemplatesTestsWithExternalPolicy.class;

    @Server(serverName)
    public static LibertyServer server;
    //issue 18363
    private static String featureVersion = "";

    @BeforeClass
    public static void setUp() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            //issue 18363
            featureVersion = "EE7";
        }
        if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
            //issue 18363
            featureVersion = "EE8";
        }

        ShrinkHelper.defaultDropinApp(server, "wsstemplatesclientwithep", "com.ibm.ws.wssecurity.fat.wsstemplatesclientwithep", "test.wssecfvt.wsstemplates",
                                      "test.wssecfvt.wsstemplates.types");
        ShrinkHelper.defaultDropinApp(server, "wsstemplateswithep", "com.ibm.ws.wssecurity.fat.wsstemplateswithep");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, true, "/wsstemplatesclientwithep/CxfWssTemplatesSvcClientWithep");
    }

    /**
     * TestDescription:
     *
     * UsernameToken with password hash, nonce, and created timestamp over SSL.
     *
     * This policy requires that the message must be protected HTTPS, and UsernameToken
     * is used for authentication. Additionally, user password inside UsernameToken is
     * hashed with nonce and timestamp.
     *
     * To validate usernameToken in Liberty server, password callback handler class must
     * be provided. The password from callback handler must match password used in
     * PasswordDigest, and also must match the password in user registry in Liberty
     * profile.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    @Test
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCXFUserNameTokenPasswordHashOverSSL() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFUserNameTokenPasswordHashOverSSL",
                    // Svc Client Url that generic test code should use
                    clientHttpsUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");
    }

    /**
     * TestDescription:
     *
     * UsernameToken with password text, nonce, and created timestamp over SSL.
     *
     * This policy requires that the message must be protected HTTPS, and UsernameToken
     * is used for authentication. The user password inside UsernameToken is send as
     * clear text, and timestamp and nonce are included. You could modify this policy to
     * remove the requirement of nonce and timestamp.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    @Test
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCXFUserNameTokenPasswordTextOverSSL() throws Exception {
        genericTest(
                    // test name for logging
                    "testCXFUserNameTokenPasswordTextOverSSL",
                    // Svc Client Url that generic test code should use
                    clientHttpsUrl,
                    // Port that svc client code should use
                    portNumberSecure,
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");
    }

    /**
     * TestDescription:
     *
     * UsernameToken with X509Token Mutual Authentication
     *
     * Both client and service have X509 Token, and message exchange are signed and
     * encrypted. The client sends its X509 token and UsernameToken to service. This
     * policy requires both signature encryption and signature confirmation.
     *
     * This template is best used if client need authenticate itself to service with
     * both X509 client certificate and UsernameToken.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void testCXFAsymmetricX509MutualAuthenticationWithUnt() throws Exception {
        genericTest(
                    // test name for logging
                    "testCXFAsymmetricX509MutualAuthenticationWithUnt",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService4",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc4 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");
    }

    /**
     * TestDescription:
     *
     * UsernameToken as EndorsingToken, and X509Token for message protection.
     *
     * Service has X509 Certificate, and client has UsernameToken without X509 token.
     * The message exchanges are signed and encrypted by an ephemeral key protected for
     * service, and client is using derived key from user password to sign message
     * signature.
     *
     * This policy template could be used if HTTP transport is not supported, service has
     * X509 token and supports UsernameToken, and service requires client to endorse the
     * message.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    //issue 18363
    @Test
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCXFUsernameTokenAsEndorsingAndX509Symmetric() throws Exception {

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym_wss4j.xml");
        } //End of issue 18363

        genericTest(
                    // test name for logging
                    "testCXFUsernameTokenAsEndorsingAndX509Symmetric",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService3",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc3 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * Service X509Token for message protection, and client X509Token as Endorsing.
     *
     * Client and server X509 certificates are used for mutual authentication and message
     * protection. Request and response are signed and encrypted by derived key from an
     * ephemeral key protected for server's certificate. The request message signature
     * is signed using client certificate.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    //issue 18363
    @Test
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCXFX509SymmetricAndEndorsing() throws Exception {

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym_wss4j.xml");
        } //End of issue 18363

        genericTest(
                    // test name for logging
                    "testCXFX509SymmetricAndEndorsing",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService5",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate5",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc5 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * UsernameToken authentication with Service X509Token for message protection
     *
     * Service has X509 Certificate, and client has UsernameToken without X509 token.
     * The message exchanges are signed and encrypted by an ephemeral key protected for
     * service, and client is using UsernameToken for authentication.
     *
     * In this sample, the token reference is using RequireThumbprintReference, you
     * could change to RequireIssuerSerialReference or RequireKeyIdentifierReference.
     * You could also modify this policy to use derived keys from ephemeral key to
     * secure message exchange by adding assertion, <sp:RequireDerivedKeys/>.
     *
     * This policy template is best used if client can only use UsernameToken to
     * authenticate itself, and message exchange is required to be signed and encrypted.
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    //issue 18363
    @Test
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCXFX509SymmetricForMessageAndUntForClient() throws Exception {

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym_wss4j.xml");
        } //End of issue 18363

        genericTest(
                    // test name for logging
                    "testCXFX509SymmetricForMessageAndUntForClient",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSSTemplatesService6",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSSTemplate6",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSTemplateWebSvc6 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

        //issue 18363
        if (featureVersion == "EE7") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        }
        if (featureVersion == "EE8") {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        } //End of issue 18363

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
