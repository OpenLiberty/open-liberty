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

package com.ibm.ws.wssecurity.fat.cxf.x509token;

import java.io.File;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
//Added 11/2020
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;

import componenttest.annotation.AllowedFFDC;
//Mei:
import componenttest.annotation.ExpectedFFDC;
//End
//Added 11/2020
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfX509StrTypeTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509StrTypeTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509sig";

    //Added 11/2020
    @Server(serverName)
    public static LibertyServer server;

    //2/2021
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().forServers(serverName).removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0"));

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "x509sigclient", "com.ibm.ws.wssecurity.fat.x509sigclient", "test.wssecfvt.x509sig", "test.wssecfvt.x509sig.types");
        ShrinkHelper.defaultDropinApp(server, "x509sig", "com.ibm.ws.wssecurity.fat.x509sig");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        //Orig:
        //commonSetUp(serverName, "server_enc.xml", false, "/x509sigclient/CxfX509SigSvcClient");

        //2/2021
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            commonSetUp(serverName, "server_enc.xml", false, "/x509sigclient/CxfX509SigSvcClient");
        }
        if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            commonSetUp(serverName, "server_enc_wss4j.xml", false, "/x509sigclient/CxfX509SigSvcClient");
        }

    }

    /**
     * Description:
     * The SOAP Body is signed in the request message using Thumbprint as the security token reference method.
     * In the response message, the SOAP Body is signed using the thumbprint reference method.
     * This is a positive scenario.
     */

    @Test
    public void testCxfClientSignThumbPrint() throws Exception {

        // use server config with encryption keystore files
        //reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enc.xml");

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
                    "The test expected a succesful message from the server.");
    }

    /**
     * Description:
     * The SOAP Body is signed in the request message using Issuer serial as the security token reference method.
     * In the response message, the SOAP Body is also signed using the Issuer serial reference method.
     * This is a positive scenario.
     */

    //2/2021 run with EE7
    @Test
    public void testCxfClientSignIssuerSerial() throws Exception {

        // use server config with encryption keystore files
        //reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enc.xml");

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
                    "The test expected a succesful message from the server.");

    }

    /**
     * Description:
     * The SOAP Body is signed in the request message using Issuer serial as the security token reference method.
     * but the Web service is configured with a keystore that does not contain the key used for signing the
     * SOAP body. The request is expected to be rejected with an appropriate exception.
     */
    //2/2021 to test with EE7, then the corresponding server_badenc.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testCxfClientKeysMismatch() throws Exception {
    public void testCxfClientKeysMismatchEE7Only() throws Exception {

        // use server config with encryption keystore files
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badenc.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientKeysMismatchEE7Only",
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
                    "The signature or decryption was invalid",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        // restore original server config
//        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

    }

    //2/2021 to test with EE8, then the corresponding server_badenc_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testCxfClientKeysMismatchEE8Only() throws Exception {

        // use server config with encryption keystore files
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badenc_wss4j.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientKeysMismatchEE8Only",
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
                    "The signature or decryption was invalid",
                    // msg to issue if do NOT get the expected result
                    "The test did not receive the expected exception from the server.");

        // restore original server config
//        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

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