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

package com.ibm.ws.wssecurity.fat.cxf.sha2sig;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;

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
public class CxfSha2SigTests extends CommonTests {

//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.sha2sig";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "sha2sigclient", "com.ibm.ws.wssecurity.fat.sha2sigclient", "test.wssecfvt.sha2sig", "test.wssecfvt.sha2sig.types");
        ShrinkHelper.defaultDropinApp(server, "sha2sig", "com.ibm.ws.wssecurity.fat.sha2sig");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
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
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfSha2DigestAlgorithm() throws Exception {

        String thisMethod = "testCxfSha2DigestAlgorithm";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha384.xml");
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512.xml");
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
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfSha1ToSha2SigAlgorithm() throws Exception {

        String thisMethod = "testCxfSha1ToSha2SigAlgorithm";
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_2048.xml");
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha3sym.xml");
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
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha5sym.xml");
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
}
