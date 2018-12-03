/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.tx.methodintf.web.SerialFilterEnabledServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This test ensures that the new method-intf element values specified in the
 * EJB 3.1 work properly.
 */
@RunWith(FATRunner.class)
public class SerialFilterEnabledTest {

    @Server("com.ibm.ws.kernel.instrument.serialfilter.fat.SerialFilterEnabled")
    @TestServlets({ @TestServlet(servlet = SerialFilterEnabledServlet.class, contextRoot = "MethodIntfWeb") })
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
        JavaArchive MethodIntfEJB = ShrinkHelper.buildJavaArchive("MethodIntfEJB.jar", "com.ibm.ws.ejbcontainer.tx.methodintf.ejb.");
        WebArchive MethodIntfWeb = ShrinkHelper.buildDefaultApp("MethodIntfWeb.war", "com.ibm.ws.ejbcontainer.tx.methodintf.web.");
        EnterpriseArchive MethodIntfTestApp = ShrinkWrap.create(EnterpriseArchive.class, "MethodIntfTest.ear");
        MethodIntfTestApp.addAsModule(MethodIntfEJB).addAsModule(MethodIntfWeb);

        ShrinkHelper.exportDropinAppToServer(server, MethodIntfTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        restorePropFile(backupFile);
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS8014E.*", "WTRN0040W.*");
        }
    }

    @Test
    public void testSerialFilterEnabledMessage() throws Exception {
        assertNotNull("The messages.log file should contain CWWKS8000I message.", server.waitForStringInLogUsingMark("CWWKS8000I"));
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
        Files.copy(Paths.get(backupFileName), Paths.get(server.getInstallRoot() + "/" + WAS_PROP_PATH + "/" + WAS_PROP_FILE), StandardCopyOption.REPLACE_EXISTING);
        new File(backupFileName).delete();
    }
}
