/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.instrument.serialfilter.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.serialfilter.fat.web.SerialFilterTestServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@Mode
@RunWith(FATRunner.class)
public class SerialFilterEnabledTest extends FATServletClient {

    public static final String APP_NAME = "SerialFilterTestWeb";
    public static final String SERVLET_NAME = APP_NAME + "/SerialFilterTestServlet";

    @Server("com.ibm.ws.kernel.instrument.serialfilter.fat.SerialFilterEnabled")
    @TestServlets({ @TestServlet(servlet = SerialFilterTestServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;
    private static String backupFile = null;

    private static String WAS_PROP_FILE = "WebSphereApplicationServer.properties";
    private static String WAS_PROP_PATH = "lib/versions/";

//    @ClassRule
//    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.kernel.instrument.serialfilter.fat.MDBServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.kernel.instrument.serialfilter.fat.MDBServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        backupFile = backupPropFile();
        server.copyFileToLibertyInstallRoot(WAS_PROP_PATH, WAS_PROP_FILE);
        // Use ShrinkHelper to build the ears
        WebArchive TestWeb = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.serialfilter.fat.web.", "com.ibm.ws.serialfilter.fat.object.denied.",
                                                          "com.ibm.ws.serialfilter.fat.object.allowed.");
        ShrinkHelper.exportDropinAppToServer(server, TestWeb);
        server.startServer(true);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        restorePropFile(backupFile);
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS8014E.*", "CWWKS8015E.*", "CWWKS8028E.*", "CWWKS8071W.*", "CWWKS8072W.*");
        }
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testAllAllowed() throws Exception {

        // update server.xml
        server.reconfigureServer("serverAllAllowed.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "AllAllowed").toString();
        Log.info(this.getClass(), "AllAllowed", "AllAllowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNull("The messages.log file should not contain CWWKS80xxE message.", server.waitForStringInLogUsingMark("CWWKS80[0-3][0-9]", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testTest1Allowed() throws Exception {
        // update server.xml
        server.reconfigureServer("serverTest1Allowed.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "Test1Allowed").toString();
        Log.info(this.getClass(), "Test1Allowed", "Test1Allowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNotNull("The messages.log file should contain CWWKS8014E with violated class (Test2) message.",
                      server.waitForStringInLogUsingMark("CWWKS8014E:.*Test2.*", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testTest1And2Allowed() throws Exception {
        // update server.xml
        server.reconfigureServer("serverTest1And2Allowed.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "Test1And2Allowed").toString();
        Log.info(this.getClass(), "Test1And2Allowed", "Test1And2Allowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNotNull("The messages.log file should contain CWWKS8014E with violated class (Test3) message.",
                      server.waitForStringInLogUsingMark("CWWKS8014E:.*Test3.*", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testTestAllAllowed() throws Exception {
        // update server.xml
        server.reconfigureServer("serverTestAllAllowed.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "TestAllAllowed").toString();
        Log.info(this.getClass(), "TestAllAllowed", "TestAllAllowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNull("The messages.log file should not contain CWWKS80xxE message.", server.waitForStringInLogUsingMark("CWWKS80[0-3][0-9]", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testAllRejected() throws Exception {
        // update server.xml
        server.reconfigureServer("serverAllRejected.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "AllDenied").toString();
        Log.info(this.getClass(), "AllDenied", "AllDenied returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNotNull("The messages.log file should contain CWWKS8028E with violated class (Test1) message.",
                      server.waitForStringInLogUsingMark("CWWKS8028E:.*Test1.*", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testCallerMethodDenied() throws Exception {
        // update server.xml
        server.reconfigureServer("serverCallerMethodDenied.xml");
        server.setMarkToEndOfLog();
        String result = runTestWithResponse(server, SERVLET_NAME, "ProhibitedCaller").toString();
        Log.info(this.getClass(), "testCallerMethodDenied", "ProhibitedCaller returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNotNull("The messages.log file should contain CWWKS8028E message.", server.waitForStringInLogUsingMark("CWWKS8028E:"));
        server.setMarkToEndOfLog();
        result = runTestWithResponse(server, SERVLET_NAME, "AllAllowed").toString();
        Log.info(this.getClass(), "testCallerMethodDenied", "AllAllowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        assertNull("The messages.log file should not contain CWWKS8028E message.", server.waitForStringInLogUsingMark("CWWKS8028E:", 1000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testDuplicateMode() throws Exception {
        // update server.xml
        server.setMarkToEndOfLog();
        server.reconfigureServer("serverDuplicateMode.xml");
        assertNotNull("The messages.log file should contain CWWKS8071W.",
                      server.waitForStringInLogUsingMark("CWWKS8071W:.*Enforce.*duplicateClass.*", 5000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testDuplicateModeWithMethod() throws Exception {
        // update server.xml
        server.setMarkToEndOfLog();
        server.reconfigureServer("serverDuplicateMode2.xml");
        assertNotNull("The messages.log file should contain CWWKS8071W.",
                      server.waitForStringInLogUsingMark("CWWKS8071W:.*Enforce.*duplicateClass.*duplicateMethod.*", 5000));
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testDuplicatePolicy() throws Exception {
        // update server.xml
        server.setMarkToEndOfLog();
        server.reconfigureServer("serverDuplicatePolicy.xml");
        assertNotNull("The messages.log file should contain CWWKS8072W.",
                      server.waitForStringInLogUsingMark("CWWKS8072W:.*Deny.*duplicateClass.*", 5000));
    }

    private static String backupPropFile() throws Exception {
        File f = File.createTempFile(WAS_PROP_FILE, null, new File(server.getInstallRoot() + "/" + WAS_PROP_PATH));
        String name = f.getName();
        f.delete();
        server.renameLibertyInstallRootFile(WAS_PROP_PATH + "/" + WAS_PROP_FILE, WAS_PROP_PATH + "/" + name);
        return name;
    }

    private static void restorePropFile(String name) throws Exception {
        String backupFileName = server.getInstallRoot() + "/" + WAS_PROP_PATH + "/" + name;
        try {
            Files.copy(Paths.get(backupFileName), Paths.get(server.getInstallRoot() + "/" + WAS_PROP_PATH + "/" + WAS_PROP_FILE), StandardCopyOption.REPLACE_EXISTING);
            new File(backupFileName).delete();
        } catch (Exception e) {
        }
    }
}
