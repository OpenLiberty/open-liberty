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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.annotation.AllowedFFDC;
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
public class CxfX509CrlTests extends CommonTests {

    //static private final Class<?> thisClass = CxfX509CrlTests.class;
//    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509crl";
    //issue 18363
    private static String featureVersion = "";

    @Server(serverName)
    public static LibertyServer server;
    static private final Class<?> thisClass = CxfX509CrlTests.class;
    //RTC 291274
    private static Boolean sunJDK8;

    @BeforeClass
    public static void setUp() throws Exception {

        //RTC 291274
        String thisMethod = "setup";

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
            featureVersion = "EE7cbh1";
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
            featureVersion = "EE7cbh2";
        }

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

        //RTC 291274
        sunJDK8 = true;
        String vendorName = System.getProperty("java.vendor");
        Log.info(thisClass, thisMethod, "JDK Vendor Name is: " + vendorName);
        String javaRuntime = System.getProperty("java.runtime.version");
        Log.info(thisClass, thisMethod, "The  current runtime java version is: " + javaRuntime);
        //Example:
        //JDK Vendor Name is: IBM Corporation; The  current runtime java version is: 8.0.7.15 - pwa6480sr7fp15-20220818_01(SR7 FP15)
        //JDK Vendor Name is: Oracle Corporation; The  current runtime java version is: 1.8.0_341-b10 13_Jul_2022_11_14 Mac OS X x64(SR7 FP15)

        if (vendorName.contains("Oracle") & javaRuntime.contains("1.8")) {
            Log.info(thisClass, thisMethod, "Using NON-IBM JDK Version 8 - this test should not run!");
        } else {
            Log.info(thisClass, thisMethod, "Using Other JDK except NON-IBM JDK 8");
            System.err.println("Using other JDK except NON-IBM JDK Version 8");
            sunJDK8 = false;
        }

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

    @Test
    public void testCXFClientCRLPNotInList() throws Exception {

        String thisMethod = "testCXFClientCRLPNotInList";

        //RTC 291274
        if (sunJDK8) {
            Log.info(thisClass, thisMethod, "Using NON-IBM JDK Version 8 - this test should not run!  SKIPPING TEST");
            System.err.println("Using Non-IBM JDK Version 8 - this test should not run!");
            return;
        }

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certp.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certp_wss4j.xml");
        }

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
                    "The test expected a successful message from the server.");

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        }

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

    @Test
    //RTC 291274
    //issue 23060
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCXFClientCRLNInList() throws Exception {

        String thisMethod = "testCXFClientCRLNInList";

        //RTC 291274
        if (sunJDK8) {
            Log.info(thisClass, thisMethod, "Using NON-IBM JDK Version 8 - this test should not run!  SKIPPING TEST");
            System.err.println("Using NON-IBM JDK Version 8 - this test should not run!");
            return;
        }

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certn.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_certn_wss4j.xml");
        }

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
                    "The test expected a successful message from the server.");

        if (featureVersion.equals("EE7cbh1")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        } else if (featureVersion.equals("EE7cbh2")) {
            reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        }

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
