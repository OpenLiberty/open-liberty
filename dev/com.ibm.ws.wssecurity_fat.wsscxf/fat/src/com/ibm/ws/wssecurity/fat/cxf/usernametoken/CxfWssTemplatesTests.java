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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

//import java.io.File;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfWssTemplatesTests extends CommonTests {

//    static private UpdateWSDLPortNum newWsdl = null;

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
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCXFUserNameTokenPasswordHashOverSSL() throws Exception {
        // reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr.xml");
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
    //Added 11/2020
    //@Mode(TestMode.FULL)
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
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
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
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCXFUsernameTokenAsEndorsingAndX509Symmetric() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
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
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCXFX509SymmetricAndEndorsing() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
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
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCXFX509SymmetricForMessageAndUntForClient() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sym.xml");
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
    }

}
