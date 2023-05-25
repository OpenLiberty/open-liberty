/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import eventlistener.web.UOWEventListenerServlet;

@RunWith(FATRunner.class)
public class UOWEventListenerTest extends FATServletClient {

    public static final String APP_NAME = "eventlistener";
    public static final String SERVLET_NAME = APP_NAME + "/UOWEventListenerServlet";

    @Server("transaction_eventListener")
    @TestServlet(servlet = UOWEventListenerServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "eventlistener.web.*");

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
