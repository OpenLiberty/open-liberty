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

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
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
public class CxfX509SigTests extends CommonTests {

    static private final Class<?> thisClass = CxfX509SigTests.class;
    static private UpdateWSDLPortNum newWsdl = null;
    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509sig";

    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

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

        ShrinkHelper.defaultDropinApp(server, "x509sigclient", "com.ibm.ws.wssecurity.fat.x509sigclient", "test.wssecfvt.x509sig", "test.wssecfvt.x509sig.types");
        ShrinkHelper.defaultDropinApp(server, "x509sig", "com.ibm.ws.wssecurity.fat.x509sig");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, false, "/x509sigclient/CxfX509SigSvcClient");

    }

    @Test
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCxfClientSignSoapBody() throws Exception {

        genericTest(
                    // test name for logging
                    "testCxfClientSignSoapBody",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT CXF Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @SkipForRepeat({ EE8_FEATURES })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfBodyNotSignedEE7Only() throws Exception {

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlSigNoClientSig.wsdl",
                                         defaultClientWsdlLoc + "X509XmlSigNoClientSigUpdated.wsdl");
        Log.info(thisClass, "testCxfBodyNotSigned", "Using " + newClientWsdl);
        genericTest(
                    // test name for logging
                    "testCxfBodyNotSignedEE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "Body not SIGNED",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @SkipForRepeat({ NO_MODIFICATION })
    public void testCxfBodyNotSignedEE8Only() throws Exception {

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlSigNoClientSig.wsdl",
                                         defaultClientWsdlLoc + "X509XmlSigNoClientSigUpdated.wsdl");
        Log.info(thisClass, "testCxfBodyNotSigned", "Using " + newClientWsdl);
        genericTest(
                    // test name for logging
                    "testCxfBodyNotSignedEE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Soap Body is not SIGNED", //@AV999
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCxfClientTimestampMissing() throws Exception {

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlSigClientTimestamps.wsdl",
                                         defaultClientWsdlLoc + "X509XmlSigClientTimestampsUpdated.wsdl");
        Log.info(thisClass, "testCxfClientTimestampMissing", "Using " + newClientWsdl);
        genericTest(
                    // test name for logging
                    "testCxfClientTimestampMissing",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService3",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Sig3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Received Timestamp does not match the requirements",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfClientPassTimestamp() throws Exception {

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlSigClientTimestamps.wsdl",
                                         defaultClientWsdlLoc + "X509XmlSigClientTimestampsUpdated.wsdl");
        Log.info(thisClass, "testCxfClientPassTimestamp", "Using " + newClientWsdl);
        genericTest(
                    // test name for logging
                    "testCxfClientPassTimestamp",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Received Timestamp does not match the requirements",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCxfClientSignBodyUNTAndTs() throws Exception {

        genericTest(
                    // test name for logging
                    "testCxfClientSignBodyUNTAndTs",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService3",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig3",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Response: This is WSSECFVT CXF Web Service",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    public void testCxfClientSigBadUrl() throws Exception {

        newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "X509XmlSigBadUrl.wsdl",
                                         defaultClientWsdlLoc + "X509XmlSigBadUrlUpdated.wsdl");
        Log.info(thisClass, "testCxfClientSigBadUrl", "Using " + newClientWsdl);

        genericTest(
                    // test name for logging
                    "testCxfClientSigBadUrl",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    newClientWsdl,
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Could not send Message",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }

    @Test
    @SkipForRepeat({ EE8_FEATURES })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfClientSignWithExpKeyEE7Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_expcert.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientSignWithExpKeyEE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService4",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "cannot create instance",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        //Added to resolve RTC 285315
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

    }

    @Test
    @SkipForRepeat({ NO_MODIFICATION })
    public void testCxfClientSignWithExpKeyEE8Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_expcert_wss4j.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientSignWithExpKeyEE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService4",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig4",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Cannot create Crypto class", //@AV999
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        //Added to resolve RTC 285315
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");

    }

    @Test
    @SkipForRepeat({ EE8_FEATURES })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfClientBadClKeyStorePswdEE7Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badclpwd.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientBadClKeyStorePswdEE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "cannot create instance",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

    }

    @Test
    @SkipForRepeat({ NO_MODIFICATION })
    public void testCxfClientBadClKeyStorePswdEE8Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badclpwd_wss4j.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientBadClKeyStorePswdEE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Cannot create Crypto class", //@AV999
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");

    }

    @Test
    @SkipForRepeat({ EE8_FEATURES })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfClientBadSrvKeyStorePswdEE7Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badsvrpwd.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientBadSrvKeyStorePswdEE7Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    //Orig:
                    "cannot create instance",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");

    }

    @Test
    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.net.MalformedURLException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    public void testCxfClientBadSrvKeyStorePswdEE8Only() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badsvrpwd_wss4j.xml");

        genericTest(
                    // test name for logging
                    "testCxfClientBadSrvKeyStorePswdEE8Only",
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "X509XmlSigService1",
                    // wsdl that the svc client code should use
                    "",
                    // wsdl port that svc client code should use
                    "UrnX509Sig",
                    // msg to send from svc client to server
                    "",
                    // expected response from server
                    "Cannot create Crypto class",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig_wss4j.xml");

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
            //Removed to resolve RTC 285315
            //restoreServer();
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
