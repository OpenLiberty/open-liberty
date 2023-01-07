/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfX509StrTypeTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509StrTypeTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509sig";
    //issue 18361
    private static String featureVersion = "";

    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "x509sigclient", "com.ibm.ws.wssecurity.fat.x509sigclient", "test.wssecfvt.x509sig", "test.wssecfvt.x509sig.types");
        ShrinkHelper.defaultDropinApp(server, "x509sig", "com.ibm.ws.wssecurity.fat.x509sig");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            commonSetUp(serverName, "server_enc.xml", false, "/x509sigclient/CxfX509SigSvcClient");
            featureVersion = "EE7cbh1";
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            commonSetUp(serverName, "server_enc_wss4j.xml", false, "/x509sigclient/CxfX509SigSvcClient");
            featureVersion = "EE7cbh2";
        }

    }

    //issue 18361 - rearranging the method sequence: testCxfClientSignIssuerSerial() on top of testCxfClientSignThumbPrint()
    //issue 20061 - add new service for negative test instead of using the one from positive test
    //issue 23060 - update to meet with new change of jaxws-2.2

    /**
     * Description:
     * The SOAP Body is signed in the request message using Issuer serial as the security token reference method.
     * In the response message, the SOAP Body is also signed using the Issuer serial reference method.
     * This is a positive scenario.
     */

    @Test
    public void testCxfClientSignIssuerSerial() throws Exception {

        genericTest(
                    // test name for logging
                    "testCxfClientSignIssuerSerial",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlStrService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Str2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509XmlStrService2 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");

    }

    /**
     * Description:
     * The SOAP Body is signed in the request message using Thumbprint as the security token reference method.
     * In the response message, the SOAP Body is signed using the thumbprint reference method.
     * This is a positive scenario.
     */

    @Test
    public void testCxfClientSignThumbPrint() throws Exception {

        genericTest(
                    // test name for logging
                    "testCxfClientSignThumbPrint",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlStrService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Str1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509XmlStrService1 Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a successful message from the server.");
    }

    /**
     * Description:
     * The SOAP Body is signed in the request message using Issuer serial as the security token reference method.
     * but the Web service is configured with a keystore that does not contain the key used for signing the
     * SOAP body. The request is expected to be rejected with an appropriate exception.
     */

    @Test
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCxfClientKeysMismatch() throws Exception {

        //issue 23060
        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badenc.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badenc_wss4j.xml");
        }

        genericTest(
                    // test name for logging
                    "testCxfClientKeysMismatch",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    //issue 20061
                    "X509XmlStrService3",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    //issue 20061
                    "UrnX509Str3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "The signature or decryption was invalid",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enc.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enc_wss4j.xml");
        }

    }

//    public String updateWsdlPortNumber(String origClientWsdl,
//                                       String updatedClientWsdl) throws Exception {
//
//        try {
//            if (portNumber.equals(defaultHttpPort)) {
//                Log.info(thisClass, "updateClientWsdl", "Test should use " + origClientWsdl + " as the client WSDL");
//                return origClientWsdl;
//            } else { // port number needs to be updated
//                newWsdl = new UpdateWSDLPortNum(origClientWsdl,
//                                updatedClientWsdl);
//                newWsdl.updatePortNum(defaultHttpPort, portNumber);
//                Log.info(thisClass, "updateClientWsdl", "Test should use " + updatedClientWsdl + " as the client WSDL");
//
//                return updatedClientWsdl;
//            }
//        } catch (Exception ex) {
//            Log.info(thisClass, "updateClientWsdl",
//                     "Failed updating the client wsdl try using the original");
//            newWsdl = null;
//            throw ex;
//        }
//    }
//
//    @After
//    public void testTearDown() throws Exception {
//
//        try {
//            if (newWsdl != null) {
//                //newWsdl.removeWSDLFile();
//                newWsdl = null;
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