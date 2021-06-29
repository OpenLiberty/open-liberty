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

package com.ibm.ws.wssecurity.fat.cxf.wss11enc;

import java.io.File;
import java.util.Set;

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

//4/2021
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
public class CxfWss11EncTests extends CommonTests {

    static private final Class<?> thisClass = CxfWss11EncTests.class;
    //static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wss11enc";

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
        ShrinkHelper.defaultDropinApp(server, "wss11encclient", "com.ibm.ws.wssecurity.fat.wss11encclient", "test.wssecfvt.wss11enc", "test.wssecfvt.wss11enc.types");
        ShrinkHelper.defaultDropinApp(server, "wss11enc", "com.ibm.ws.wssecurity.fat.wss11enc");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, false, "/wss11encclient/CxfWss11EncSvcClient");
    }

    /**
     * TestDescription:
     *
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Name="xs:NCName" Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message is like:
     * <soapenv:Header><t:EncryptedData>... </t:EncryptedData><s:Security>...</s:Security>
     * </soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    //2/2021 to test with EE7, then the corresponding server_enchdr.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientEncryptHeaderNS1() throws Exception {
    public void testCXFClientEncryptHeaderNS1EE7Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS1EE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    //4/2021
    //@AllowedFFDC(value = { "java.net.MalformedURLException", "java.lang.ClassNotFoundException" })
    //5/2021 added PrivilegedActionExc, NoSuchMethodExc as a result of java11 and ee8
    @AllowedFFDC(value = { "java.net.MalformedURLException", "java.lang.ClassNotFoundException", "java.security.PrivilegedActionException",
                           "java.lang.NoSuchMethodException" })
    //2/2021 to test with EE8, then the corresponding server_enchdr_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientEncryptHeaderNS1EE8Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_enchdr_wss4j.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS1EE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    /**
     * TestDescription:
     *
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Name="xs:NCName" Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * <fvt:CXF_FVT_TEST xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF"></fvt:CXF_FVT_TEST>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message has EncryptedHeader for specific header element:
     * <soapenv:Header><enc:EncryptedData>...</enc:EncryptedData>
     * <fvt:CXF_FVT_TEST xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF"></fvt:CXF_FVT_TEST>
     * <s:Security>...</s:Security></soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */
    //2/2021 to test with EE7, then the corresponding server.xml and callbackhandler can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientEncryptHeaderNS2() throws Exception {
    public void testCXFClientEncryptHeaderNS2EE7Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS2EE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "multiHeaders",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 to test with EE8, then the corresponding server_wss4j.xml and callbackhandler can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientEncryptHeaderNS2EE8Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderNS2EE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc1",
                    // msg to send from svc client to server
                    "multiHeaders",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc1 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    /**
     * TestDescription:
     * Test simple <EncrytedHeader> element is present in the request message when the EncrytedPart policy
     * assertion has been specified on the policy.
     * This test will use the following simple assertion in policy:
     * <sp:EncrytedParts.. >
     * <sp:Header Namespace="xs:anyURI">
     * <sp:EncrytedParts..>
     *
     * Plain SOAP message has:
     * <soapenv:Envelope ...><soapenv:Header>
     * <fvt:CXF_FVT xmlns:fvt="http://encryptedhdr/WSSECFVT/CXF">
     * <fvt:id>ENCHDR_TEST</fvt:id>
     * <fvt:password>Good_and_Ok</fvt:password>
     * </fvt:CXF_FVT>
     * </soapenv:Header><soapenv:Body>...</soapenv:Body></soapenv:Envelope>
     *
     * Outbound request message is like:
     * <soapenv:Header>
     * <enc:EncryptedData>...</enc:EncryptedData><s:Security>...</s:Security>
     * </soapenv:Header>
     *
     * Verify that the Web service is invoked successfully. This is a positive scenario.
     */

    //2/2021 to test with EE7, then the corresponding server.xml and callbackhandler can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientEncryptHeaderAny() throws Exception {
    public void testCXFClientEncryptHeaderAnyEE7Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderAnyEE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 to test with EE8, then the corresponding server_wss4j.xml and callbackhandler can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientEncryptHeaderAnyEE8Only() throws Exception {
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        genericTest(
                    // test name for logging
                    "testCXFClientEncryptHeaderAnyEE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "WSS11EncService2",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "WSS11Enc2",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is Wss11EncWebSvc2 Web Service.",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");
    }

//    @After
//    public void testTearDown() throws Exception {
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
