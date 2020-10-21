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
package com.ibm.ws.transaction.test.tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.SimpleServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
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
public class SimpleTestNoRecOnStart extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server("com.ibm.ws.transaction_noRecoveryOnStartup")
    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.web.*");

        // TODO: Revisit this after all features required by this FAT suite are available.
        // The test-specific public features, txtest-x.y, are not in the repeatable EE feature
        // set. And, the ejb-4.0 feature is not yet available. Enable jdbc-4.2 to enable transactions-2.0
        // The following sets the appropriate features for the EE9 repeatable tests.
        if (JakartaEE9Action.isActive()) {
            server.changeFeatures(Arrays.asList("jdbc-4.2", "txtest-2.0", "servlet-5.0", "componenttest-2.0", "osgiconsole-1.0", "jndi-1.0"));
        }

        try {
            server.setServerStartTimeout(300000);
            server.startServer();
        } catch (Exception e) {
            Log.error(SimpleTestNoRecOnStart.class, "setUp", e);
            // Try again
            server.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    /**
     * Test enlistment in transactions.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testTransactionEnlistmentNoRecOnStart() throws Exception {
        runTest("testTransactionEnlistment");
    }

    /**
     * Runs the test
     */
    private void runTest(String testName) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, testName);

        } catch (Throwable e) {
        }
        Log.info(this.getClass(), testName, testName + " returned: " + sb);

    }
}
