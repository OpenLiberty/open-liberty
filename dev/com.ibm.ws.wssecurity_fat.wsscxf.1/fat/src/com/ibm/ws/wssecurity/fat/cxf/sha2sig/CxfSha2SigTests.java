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

package com.ibm.ws.wssecurity.fat.cxf.sha2sig;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSha2SigTests extends CommonTests {

//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.sha2sig";

    static private final Class<?> thisClass = CxfSha2SigTests.class;
    //issue 18363
    private static String featureVersion = "";

    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("jaxws-2.2")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            //issue 18363
            featureVersion = "EE7";
        } else if (features.contains("jaxws-2.3")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
            //issue 18363
            featureVersion = "EE8";
        }

        ShrinkHelper.defaultDropinApp(server, "sha2sigclient", "com.ibm.ws.wssecurity.fat.sha2sigclient", "test.wssecfvt.sha2sig", "test.wssecfvt.sha2sig.types");
        ShrinkHelper.defaultDropinApp(server, "sha2sig", "com.ibm.ws.wssecurity.fat.sha2sig");

        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, false,
                    "/sha2sigclient/Sha2SigSvcClient");
    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * The server setup in server.xml specifies to use sha2 signature algorithm
     * in this test. This is a positive scenario.
     *
     */

    @Test
    public void testCxfSha2SignSoapBody() throws Exception {

        String thisMethod = "testCxfSha2SignSoapBody";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService1",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response messages.
     * The WS-Security policy specifies to use Sha256 digest algorithm
     * in the algorithm suite. This is a positive scenario.
     *
     */

    @Test
    public void testCxfSha2DigestAlgorithm() throws Exception {

        String thisMethod = "testCxfSha2DigestAlgorithm";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService2",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "This is WSSECFVT SHA256 Digest Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * The server setup in server.xml specifies to use sha384 signature algorithm
     * in this test. This is a positive scenario.
     *
     */
    @Test
    public void testCxfSha384SigAlgorithm() throws Exception {

        String thisMethod = "testTwasSha384SigAlgorithm";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha384.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha384_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService3",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA384 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * The server setup in server.xml specifies to use sha384 signature algorithm
     * in this test. This is a positive scenario.
     *
     */
    @Test
    public void testCxfSha512SigAlgorithm() throws Exception {

        String thisMethod = "testTwasSha512SigAlgorithm";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService4",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA512 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws cxf web service.
     * The SOAP Body is signed in the request and response messages,
     * using Sha1 signature algorithm, but the Web service is configured
     * to use the sha256 signature algorithm. The client request is
     * expected to be rejected with an appropriate exception.
     * This is a negative scenario.
     *
     */

    @Test
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCxfSha1ToSha2SigAlgorithm() throws Exception {

        String thisMethod = "testCxfSha1ToSha2SigAlgorithm";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha2.xml");
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
                        "Sha2SigService5",
                        // wsdl that the svc client code should use
                        // newClientWsdl,
                        "",
                        // wsdl port that svc client code should use
                        "UrnSha2Sig5",
                        // msg to send from svc client to server
                        "",
                        "Response: javax.xml.ws.soap.SOAPFaultException",
                        // expected response from server
                        "The signature method does not match the requirement",
                        // msg to issue if do NOT get the expected result
                        "The test did not receive the expected exception from the server.");

            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha2_wss4j.xml");
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
                        "Sha2SigService5",
                        // wsdl that the svc client code should use
                        // newClientWsdl,
                        "",
                        // wsdl port that svc client code should use
                        "UrnSha2Sig5",
                        // msg to send from svc client to server
                        "",
                        "Response: javax.xml.ws.soap.SOAPFaultException",
                        // expected response from server
                        "An error was discovered processing the <wsse:Security> header",
                        // msg to issue if do NOT get the expected result
                        "The test did not receive the expected exception from the server.");

            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");

        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * A TWAS thin client invokes a simple jax-ws cxf web service.
     * The SOAP Body is signed in the request and response messages,
     * using Sha256 signature algorithm and 2048 size keys.
     * The client request is expected to be invoked successfully.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfSha256SigAlg2048Keylen() throws Exception {

        String thisMethod = "testCxfSha256SigAlg2048Keylen";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_2048.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_2048_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService7",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig7",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA2-2048 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * A CXF client invokes a simple jax-ws cxf web service.
     * The SOAP Body is signed and encrypted in the request and response messages,
     * using Sha384 signature algorithm, symmetric binding is used in the policy.
     * The client request is expected to be invoked successfully.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfSha384SymBinding() throws Exception {

        String thisMethod = "testCxfSha384SymBinding";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha3sym.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha3sym_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService8",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig8",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA2 SYM Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

    }

    /**
     * TestDescription:
     *
     * A CXF client invokes a simple jax-ws cxf web service.
     * The SOAP Body is signed and encrypted in the request and response messages,
     * using Sha384 signature algorithm, symmetric binding is used in the policy.
     * The client request is expected to be invoked successfully.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfSha512SymBinding() throws Exception {
        String thisMethod = "testCxfSha512SymBinding";
        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha5sym.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha5sym_wss4j.xml");
        } //End of issue 18363

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
                    "Sha2SigService8",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig8",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT SHA2 SYM Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        //issue 18363
        if (featureVersion.equals("EE7")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
        } else if (featureVersion.equals("EE8")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");
        } //End of issue 18363

    }

//    @After
//    public void testTearDown() throws Exception {
//        try {
//            if (newWsdl != null) {
//                //newWsdl.removeWSDLFile();
//                newWsdl = null;
//                //newClientWsdl = null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace(System.out);
//        }
//    }

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
