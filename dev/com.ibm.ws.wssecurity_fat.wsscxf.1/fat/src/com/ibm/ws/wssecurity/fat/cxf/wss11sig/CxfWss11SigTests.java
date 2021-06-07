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

package com.ibm.ws.wssecurity.fat.cxf.wss11sig;

import java.io.File;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.AllowedFFDC;
//Added 10/2020
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfWss11SigTests extends CommonTests {

    static private final Class<?> thisClass = CxfWss11SigTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wss11sig";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //2/2021
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

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "wss11sigclient", "com.ibm.ws.wssecurity.fat.wss11sigclient", "test.wssecfvt.wss11sig", "test.wssecfvt.wss11sig.types");
        ShrinkHelper.defaultDropinApp(server, "wss11sig", "com.ibm.ws.wssecurity.fat.wss11sig");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, false,
                    "/wss11sigclient/CxfWss11SigSvcClient");
    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in both the request and response.
     * Signature confirmation is enabled on both client and server side
     * This is a positive scenario.
     *
     */

    //4/2021
    //@AllowedFFDC(value = { "java.net.MalformedURLException", "java.lang.ClassNotFoundException" })
    //5/2021 added PrivilegedActionExc, NoSuchMethodExc as a result of java11 and ee8
    @AllowedFFDC(value = { "java.net.MalformedURLException", "java.lang.ClassNotFoundException", "java.security.PrivilegedActionException",
                           "java.lang.NoSuchMethodException" })
    @Test
    public void testCXFClientBasicSigConfirm() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientBasicSigConfirm",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11SigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is NOT signed in either the request or response.
     * Signature confirmation is enabled on both client and server
     * side. The response should contain one SignatureConfirmation
     * element with no value.
     * This is a positive scenario.
     *
     */

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    @Test
    public void testCXFClientBasicSigConfirmNoSig() throws Exception {

        genericTest(
                    // test name for logging
                    "testCXFClientBasicSigConfirmNoSig",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11SigService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * Signature confirmation is enabled in just the client policy.
     * The response does not contain a SignatureConfirmation but
     * the client is expecting it to be there.
     * This is a negative scenario.
     *
     */
    @Test
    //Orig:
    //@AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //2/2021
    //@AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException" })
    //4/2021
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.net.MalformedURLException" })
    public void testCXFClientBasicSigClConfSrvNoConf() throws Exception {

        String thisMethod = "testCXFClientBasicSigClConfSrvNoConf";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_reqSigMismatch.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_reqSigMismatchUpdated.wsdl");
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
                    "WSS11SigService4",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Check Signature confirmation: the stored signature values list is not empty",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * Signature confirmation is enabled in just the server policy.
     * The response contains a SignatureConfirmation but when
     * the client is NOT expecting it to be there. It should be ignored.
     * This is a positive scenario.
     *
     */

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    @Test
    public void testCXFClientBasicSigClNoConfSrvConf() throws Exception {

        String thisMethod = "testCXFClientBasicSigClNoConfSrvConf";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_reqSigMismatch.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_reqSigMismatchUpdated.wsdl");
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
                    "WSS11SigService5",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig5",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc5 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request, but not the response.
     * Signature confirmation is enabled in the client and server policy.
     * SignatureConfirmation is included in EncryptedElements in the
     * server policy only.
     * The response does not sign the body as the client expects.
     * This is a negative scenario.
     *
     */

    //2/2021 to test with EE7, then the corresponding message can be expected
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //@ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Mei:
    @AllowedFFDC("org.apache.ws.security.WSSecurityException") //@AV999
    //Orig:
    //public void testCXFClientBasicEncryptedElementMisMatch() throws Exception {
    public void testCXFClientBasicEncryptedElementMisMatchEE7Only() throws Exception {

        //Orig:
        //String thisMethod = "testCXFClientBasicEncryptedElement";
        String thisMethod = "testCXFClientBasicEncryptedElementMisMatchEE7Only";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_Elements.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_ElementsUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        //Mei:
        //server.is //@AV999 TODO
        boolean jaxws23feature = false;
        String messagetoexpect = "Body not SIGNED"; //existing
        //if (server.hasjasws23feature) then jaxws23feature=true //@AV999 TODO
        jaxws23feature = true;
        if (jaxws23feature) {
            messagetoexpect = "Soap Body is not SIGNED";
        }
        //End
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
                    "WSS11SigService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig6",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "Body not SIGNED",
                    //Mei:
                    //"Body not SIGNED", //@AV999
                    //messagetoexpect,
                    //End
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //2/2021 to test with EE8, then the corresponding message can be expected
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //@AllowedFFDC("org.apache.wss4j.common.ext.WSSecurityException")
    //4/2021
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.net.MalformedURLException" })
    public void testCXFClientBasicEncryptedElementMisMatchEE8Only() throws Exception {

        String thisMethod = "testCXFClientBasicEncryptedElementMisMatchEE8Only";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_Elements.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_ElementsUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        //Mei:
        //server.is //@AV999 TODO
        boolean jaxws23feature = false;
        String messagetoexpect = "Body not SIGNED"; //existing
        //if (server.hasjasws23feature) then jaxws23feature=true //@AV999 TODO
        jaxws23feature = true;
        if (jaxws23feature) {
            messagetoexpect = "Soap Body is not SIGNED";
        }
        //End
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
                    "WSS11SigService6",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig6",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    //"Body not SIGNED",
                    //Mei:
                    //"Body not SIGNED", //@AV999
                    //messagetoexpect,
                    "Soap Body is not SIGNED",
                    //End
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request, but not the response.
     * Signature confirmation is enabled in the client and server policy.
     * SignatureConfirmation is included in EncryptedElements in the
     * server policy only.
     * Server policy also uses X509 IncludeToken set to Never and
     * RequireKeyIdentifierReference in the InitiatorToken.
     * The response does not sign the body as the client expects.
     * This is a positive scenario.
     *
     */
    //2/2021 to test with EE7, then the corresponding server_enchdr.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientBasicEncryptedElement() throws Exception {
    public void testCXFClientBasicEncryptedElementEE7Only() throws Exception {

        //Orig:
        //String thisMethod = "testCXFClientBasicEncryptedElement";
        String thisMethod = "testCXFClientBasicEncryptedElementEE7Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr.xml");
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
                    "WSS11SigService6a",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig6a",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc6a Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 to test with EE8, then the corresponding server_enchdr_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientBasicEncryptedElementEE8Only() throws Exception {

        String thisMethod = "testCXFClientBasicEncryptedElementEE8Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr_wss4j.xml");
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
                    "WSS11SigService6a",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig6a",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc6a Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request, but not the response.
     * Signature confirmation is enabled in the client and server policy.
     * SignatureConfirmation is included in SignedElements in the server
     * policy only.
     * The body in the response is not signed as the client expects.
     * This is a negative scenario
     *
     */

    //2/2021 to test with EE7, then the corresponding message can be expected
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testCXFClientBasicSigSignedElementMisMatch() throws Exception {
    public void testCXFClientBasicSigSignedElementMisMatchEE7Only() throws Exception {

        //Orig:
        //String thisMethod = "testCXFClientBasicSigSignedElement";
        String thisMethod = "testCXFClientBasicSigSignedElementMisMatchEE7Only";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_Elements.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_ElementsUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        //Mei:
        //server.is //@AV999 TODO
        boolean jaxws23feature = false;
        String messagetoexpect = "Body not SIGNED"; //existing
        //if (server.hasjasws23feature) then jaxws23feature=true //@AV999 TODO
        jaxws23feature = true;
        if (jaxws23feature) {
            messagetoexpect = "Soap Body is not SIGNED";
        }
        //End
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
                    "WSS11SigService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig7",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "Body not SIGNED",
                    //Mei:
                    //"Body not SIGNED", //@AV999
                    //messagetoexpect,
                    //End
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //2/2021 to test with EE8, then the corresponding message can be expected and server_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //@AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //4/2021
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "java.net.MalformedURLException" })
    public void testCXFClientBasicSigSignedElementMisMatchEE8Only() throws Exception {

        String thisMethod = "testCXFClientBasicSigSignedElementMisMatchEE8Only";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_Elements.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_ElementsUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        //Mei:
        //server.is //@AV999 TODO
        boolean jaxws23feature = false;
        String messagetoexpect = "Body not SIGNED"; //existing
        //if (server.hasjasws23feature) then jaxws23feature=true //@AV999 TODO
        jaxws23feature = true;
        if (jaxws23feature) {
            messagetoexpect = "Soap Body is not SIGNED";
        }
        //End
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
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
                    "WSS11SigService7",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig7",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    //"Body not SIGNED",
                    //Mei:
                    //"Body not SIGNED", //@AV999
                    //messagetoexpect,
                    //End
                    "Soap Body is not SIGNED",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and the response.
     * Signature confirmation is enabled in the client and server
     * policy. SignatureConfirmation is included in SignedElements
     * in both the client and server policy. The response is as
     * the client expects.
     * This is a positive scenario.
     */

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientBasicSigSignedElement() throws Exception {
    public void testCXFClientBasicSigSignedElementEE7Only() throws Exception {

        String thisMethod = "testCXFClientBasicSigSignedElement";
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
                    "WSS11SigService7a",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig7a",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc7a Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientBasicSigSignedElementEE8Only() throws Exception {

        String thisMethod = "testCXFClientBasicSigSignedElement";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
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
                    "WSS11SigService7a",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Sig7a",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11SigWebSvc7a Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is NOT signed in either the request or the response.
     * Signature confirmation is enabled in the client only. The
     * response is missing the signature confirmation that it expects.
     * This is a negative scenario.
     */
    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientBasicSigClNoSignConfSrvNoSignNoConf() throws Exception {
    public void testCXFClientBasicSigClNoSignConfSrvNoSignNoConfEE7Only() throws Exception {

        String thisMethod = "testCXFClientBasicSigClNoSignConfSrvNoSignNoConf";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_sigConfMissingInServer.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_sigConfMissingInServerUpdated.wsdl");
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
                    "WSS11SigService8",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig8",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Wss11: Signature Confirmation policy validation failed",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientBasicSigClNoSignConfSrvNoSignNoConfEE8Only() throws Exception {

        String thisMethod = "testCXFClientBasicSigClNoSignConfSrvNoSignNoConf";
        printMethodName(thisMethod, "Start Prep for " + thisMethod);
        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "WSS11Signature_sigConfMissingInServer.wsdl",
                                         defaultClientWsdlLoc + "WSS11Signature_sigConfMissingInServerUpdated.wsdl");
        Log.info(thisClass, thisMethod, "Using " + newClientWsdl);
        printMethodName(thisMethod, "End Prep for " + thisMethod);
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
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
                    "WSS11SigService8",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "WSS11Sig8",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Wss11: Signature Confirmation policy validation failed",
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
            restoreServer();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    //2/2021
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
