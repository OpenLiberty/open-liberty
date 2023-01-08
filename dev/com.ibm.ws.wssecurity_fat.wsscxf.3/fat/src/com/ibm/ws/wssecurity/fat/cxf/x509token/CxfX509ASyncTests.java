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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfX509ASyncTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509ASyncTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509async";

    @Server(serverName)
    public static LibertyServer server;

    private static String CBHVersion = "";

    @BeforeClass
    public static void setUp() throws Exception {

        //issue 23060
        //6/2021 need to update CommonTest.java to get req parameter of CBHVersion; comment out for now
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            CBHVersion = "EE7cbh1";
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
            CBHVersion = "EE7cbh2";
        }

        ShrinkHelper.defaultDropinApp(server, "x509aSyncclient", "com.ibm.ws.wssecurity.fat.x509Asyncclient", "test.wssecfvt.x509async", "test.wssecfvt.x509async.types");
        ShrinkHelper.defaultDropinApp(server, "x509aSync", "com.ibm.ws.wssecurity.fat.x509async", "test.wssecfvt.x509async", "test.wssecfvt.x509async.types");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);
        commonSetUp(serverName, false, "/x509aSyncclient/CxfX509AsyncSvcClient");
        portNumber = "" + server.getHttpDefaultPort();
        clientHttpUrl = "http://localhost:" + portNumber +
                        "/x509aSyncclient/CxfX509AsyncSvcClient";

    }

    // All tests are using the same server side methods - they just invoke the calls
    // differently - the service client is keying off the testName
    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body, Time and UserNameToken are signed in the request and response.
     * The test uses an Async invocation and does NOT block waiting for the response.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfAsyncInvokeNonBlocking() throws Exception {

        Log.info(thisClass, "setup", "CBHVersion in test method: " + CBHVersion);
        genericAsyncTest(
                         // test name for logging
                         testName.getMethodName(),
                         // Svc Client Url that generic test code should use
                         clientHttpUrl,
                         // Port that svc client code should use
                         "",
                         // user that svc client code should use
                         "user1",
                         // pw that svc client code should use
                         "security",
                         // wsdl sevice that svc client code should use
                         "X509AsyncService",
                         // wsdl that the svc client code should use
                         "",
                         // wsdl port that svc client code should use
                         "X509AsyncPort",
                         // msg to send from svc client to server
                         "",
                         // expected response from server
                         "Response: null",
                         //"This is WSSECFVT CXF X509AsyncService",
                         // msg to issue if do NOT get the expected result
                         "The test expected a successful message from the server.",
                         CBHVersion);

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body, Time and UserNameToken are signed in the request and response.
     * The test uses an Async invocation and does block waiting for the response.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfAsyncInvokeBlocking() throws Exception {

        genericAsyncTest(
                         // test name for logging
                         testName.getMethodName(),
                         // Svc Client Url that generic test code should use
                         clientHttpUrl,
                         // Port that svc client code should use
                         "",
                         // user that svc client code should use
                         "user1",
                         // pw that svc client code should use
                         "security",
                         // wsdl sevice that svc client code should use
                         "X509AsyncService",
                         // wsdl that the svc client code should use
                         "",
                         // wsdl port that svc client code should use
                         "X509AsyncPort",
                         // msg to send from svc client to server
                         "",
                         // expected response from server
                         "Response: null",
                         //"This is WSSECFVT CXF X509AsyncService",
                         // msg to issue if do NOT get the expected result
                         "The test expected a successful message from the server.",
                         CBHVersion);

    }

    /**
     * TestDescription:
     *
     * A CXF service client invokes a simple jax-ws CXF web service.
     * The SOAP Body, Time and UserNameToken are signed in the request and response.
     * The test uses an Async invocation and AsyncHandler waiting for the response.
     * This is a positive scenario.
     *
     */

    @Test
    public void testCxfAsyncInvokeWithHandler() throws Exception {

        genericAsyncTest(
                         // test name for logging
                         testName.getMethodName(),
                         // Svc Client Url that generic test code should use
                         clientHttpUrl,
                         // Port that svc client code should use
                         "",
                         // user that svc client code should use
                         "user1",
                         // pw that svc client code should use
                         "security",
                         // wsdl sevice that svc client code should use
                         "X509AsyncService",
                         // wsdl that the svc client code should use
                         "",
                         // wsdl port that svc client code should use
                         "X509AsyncPort",
                         // msg to send from svc client to server
                         "",
                         // expected response from server
                         "Response: null",
                         //"This is WSSECFVT CXF X509AsyncService",
                         // msg to issue if do NOT get the expected result
                         "The test expected a successful message from the server.",
                         CBHVersion);

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

    @After
    public void testTearDown() throws Exception {
        try {
            if (newWsdl != null) {
                //newWsdl.removeWSDLFile();
                newWsdl = null;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
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
