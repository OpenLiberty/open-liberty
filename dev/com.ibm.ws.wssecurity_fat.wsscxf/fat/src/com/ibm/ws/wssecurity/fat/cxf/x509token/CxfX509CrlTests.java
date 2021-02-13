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

package com.ibm.ws.wssecurity.fat.cxf.x509token;

import java.io.File;
import java.util.Set;

//Added 11/2020
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

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
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfX509CrlTests extends CommonTests {

    //static private final Class<?> thisClass = CxfX509CrlTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509crl";

    //Added 11/2020
    @Server(serverName)
    public static LibertyServer server;
    //2/2021
    static private final Class<?> thisClass = CxfX509CrlTests.class;

    @BeforeClass
    public static void setUp() throws Exception {

        //orig from CL
        //commonSetUp(serverName, false,
        //            "/x509crlclient/CxfX509CrlSvcClient");

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
        WebArchive x509crlclient_war = ShrinkHelper.buildDefaultApp("x509crlclient", "com.ibm.ws.wssecurity.fat.x509crlclient", "test.wssecfvt.x509crl",
                                                                    "test.wssecfvt.x509crl.types");
        WebArchive x509crl_war = ShrinkHelper.buildDefaultApp("x509crl", "com.ibm.ws.wssecurity.fat.x509crl");
        ShrinkHelper.exportToServer(server, "testApps", x509crlclient_war);
        ShrinkHelper.exportToServer(server, "testApps", x509crl_war);

        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);
        commonSetUp(serverName, false, "/x509crlclient/CxfX509CrlSvcClient");
        portNumber = "" + server.getHttpDefaultPort();
        clientHttpUrl = "http://localhost:" + portNumber +
                        "/x509crlclient/CxfX509CrlSvcClient";

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * The server setup in server.xml specifies a crl list and the
     * cert used in this test is NOT in that list, so the request
     * should succeed.
     * This is a positive scenario.
     *
     */
    //2/2021 to test with EE7, then the corresponding server_certp.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCXFClientCRLPNotInList() throws Exception {
    public void testCXFClientCRLPNotInListEE7Only() throws Exception {

        //Orig:
        //String thisMethod = "testCXFClientCRLPNotInList";
        String thisMethod = "testCXFClientCRLPNotInListEE7Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certp.xml");
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
                    "X509CrlNotInListService",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "X509CrlNotInList",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509CrlNotInListService Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //2/2021 to test with EE8, then the corresponding server_certp_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCXFClientCRLPNotInListEE8Only() throws Exception {

        String thisMethod = "testCXFClientCRLPNotInListEE8Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certp_wss4j.xml");
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
                    "X509CrlNotInListService",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "X509CrlNotInList",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is X509CrlNotInListService Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body is signed in the request and response.
     * The server setup in server.xml specifies a crl list and the
     * cert used in this test IS in that list, so the request
     * should be rejected.
     * This is a negative scenario.
     *
     */
    //2/2021 to test with EE7, then the corresponding server_certn.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //Mei:
    //@ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    //End
    //Orig:
    //public void testCXFClientCRLNInList() throws Exception {
    public void testCXFClientCRLNInListEE7Only() throws Exception {

        //Orig:
        //String thisMethod = "testCXFClientCRLNInList";
        String thisMethod = "testCXFClientCRLNInListEE7Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certn.xml");
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
                    "X509CrlInListService",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "X509CrlInList",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "has been revoked",
                    // additional string to check for - chc SUN doesn't say what alias has been revoked
                    //"myx509certN",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    //2/2021 to test with EE8, then the corresponding server_certn_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testCXFClientCRLNInListEE8Only() throws Exception {

        String thisMethod = "testCXFClientCRLNInListEE8Only";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certn_wss4j.xml");
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
                    "X509CrlInListService",
                    // wsdl that the svc client code should use
                    //newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "X509CrlInList",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "has been revoked",
                    // additional string to check for - chc SUN doesn't say what alias has been revoked
                    //"myx509certN",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

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
