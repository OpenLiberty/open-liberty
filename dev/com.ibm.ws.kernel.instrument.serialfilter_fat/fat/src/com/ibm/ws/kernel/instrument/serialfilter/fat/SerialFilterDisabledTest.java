/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.instrument.serialfilter.fat;

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
@RunWith(FATRunner.class)
public class SerialFilterDisabledTest extends FATServletClient {
    public static final String APP_NAME = "SerialFilterTestWeb";
    public static final String SERVLET_NAME = APP_NAME + "/SerialFilterTestServlet";

    @Server("com.ibm.ws.kernel.instrument.serialfilter.fat.SerialFilterDisabled")
    @TestServlets({ @TestServlet(servlet = SerialFilterTestServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;
    private static String backupFile = null;

    private static String WAS_PROP_FILE = "WebSphereApplicationServer.properties";
    private static String WAS_PROP_PATH = "lib/versions/";

    @BeforeClass
    public static void setUp() throws Exception {
        backupFile = backupPropFile();
        WebArchive TestWeb = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.serialfilter.fat.web.", "com.ibm.ws.serialfilter.fat.object.denied.",
                                                          "com.ibm.ws.serialfilter.fat.object.allowed.");
        ShrinkHelper.exportDropinAppToServer(server, TestWeb);
        server.startServer(true);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        restorePropFile(backupFile);
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS8070E:");
        }
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.kernel.productinfo.ProductInfoParseException" })
    public void testAllAllowedWhenDisabled() throws Exception {
        String result = runTestWithResponse(server, SERVLET_NAME, "AllAllowed").toString();
        Log.info(this.getClass(), "AllAllowed", "AllAllowed returned: " + result);
        assertTrue("The result should be SUCCESS", result.contains("SUCCESS"));
        server.resetLogMarks();
        assertNull("The messages.log file should not contain CWWKS8000I message.", server.waitForStringInLogUsingMark("CWWKS8000I", 1000));
    }

    private static String backupPropFile() throws Exception {
        File f = File.createTempFile(WAS_PROP_FILE, null, new File(server.getInstallRoot() + "/" + WAS_PROP_PATH));
        String name = f.getName();
        f.delete();
        server.renameLibertyInstallRootFile(WAS_PROP_PATH + "/" + WAS_PROP_FILE, WAS_PROP_PATH + "/" + name);
        //deleteFileFromLibertyInstallRoot(String filePath);
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
