/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RecoveryTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/RecoveryServlet";

    @Server("recovery")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        // TODO: Revisit this after all features required by this FAT suite are available.
        // The test-specific public features, txtest-x.y, are not in the repeatable EE feature
        // set. And, the ejb-4.0 feature is not yet available. Enable jdbc-4.2 to enable transactions-2.0.
        // The following sets the appropriate features for the EE9 repeatable tests.
        if (JakartaEE9Action.isActive()) {
            server.changeFeatures(Arrays.asList("jdbc-4.2", "txtest-2.0", "servlet-5.0", "componenttest-2.0", "osgiconsole-1.0", "jndi-1.0"));
        }

        server.setServerStartTimeout(TestUtils.LOG_SEARCH_TIMEOUT);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        TestUtils.stopServer(server);
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec000() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "007");
        TestUtils.recoveryTest(server, SERVLET_NAME, "090");
    }

    @Test
    public void testRec001() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "001");
    }

    @Test
    public void testRec002() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "002");
    }

    @Test
    public void testRec003() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "003");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec004() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "004");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec005() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "005");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec006() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "006");
    }

    @Test
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec007() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "007");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec008() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "008");
    }

    @Test
    public void testRec009() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "009");
    }

    @Test
    public void testRec010() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "010");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec011() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "011");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec012() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "012");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec013() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "013");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec014() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "014");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec015() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "015");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec016() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "016");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec017() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "017");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec018() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "018");
    }

    @Test
    public void testRec047() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "047");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec048() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "048");
    }

    @Test
    public void testRec050() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "050");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec051() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "051");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec090() throws Exception {
        TestUtils.recoveryTest(server, SERVLET_NAME, "090");
    }
}
