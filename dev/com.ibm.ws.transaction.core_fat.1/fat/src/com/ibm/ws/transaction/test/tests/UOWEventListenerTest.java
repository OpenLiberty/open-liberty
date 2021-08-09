/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.eventlistener.web.UOWEventListenerServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class UOWEventListenerTest extends FATServletClient {

    public static final String APP_NAME = "eventlistener";
    public static final String SERVLET_NAME = APP_NAME + "/UOWEventListenerServlet";

    @Server("com.ibm.ws.transaction_eventListener")
    @TestServlet(servlet = UOWEventListenerServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.eventlistener.web.*");

        server.copyFileToLibertyInstallRoot("lib/features/", "features/eventListener-1.0.mf");
        assertTrue("Failed to install eventListener-1.0 manifest",
                   server.fileExistsInLibertyInstallRoot("lib/features/eventListener-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.transaction.fat.eventListener.jar");
        assertTrue("Failed to install eventListener bundle",
                   server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.transaction.fat.eventListener.jar"));

        server.setServerStartTimeout(300000);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W");
                server.deleteFileFromLibertyInstallRoot("lib/features/eventListener-1.0.mf");
                assertFalse("Failed to uninstall eventListener-1.0 manifest",
                            server.fileExistsInLibertyInstallRoot("lib/features/eventListener-1.0.mf"));
                server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.transactions.fat.eventListener.jar");
                assertFalse("Failed to uninstall EventListener bundle",
                            server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.transactions.fat.eventListener.jar"));
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }
}
