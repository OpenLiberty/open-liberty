/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.RecoveryServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
@Mode
@RunWith(FATRunner.class)
public class RecoveryTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/RecoveryServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = RecoveryServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<ProgramOutput>() {

            @Override
            public ProgramOutput run() throws Exception {
                return server.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
            }
        });
    }

    @Test
    public void testRec001() throws Exception {
        recoveryTest("001");
    }

    @Test
    public void testRec002() throws Exception {
        recoveryTest("002");
    }

    @Test
    public void testRec003() throws Exception {
        recoveryTest("003");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec004() throws Exception {
        recoveryTest("004");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec005() throws Exception {
        recoveryTest("005");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec006() throws Exception {
        recoveryTest("006");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    @Mode(TestMode.LITE)
    public void testRec007() throws Exception {
        recoveryTest("007");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec008() throws Exception {
        recoveryTest("008");
    }

    @Test
    public void testRec009() throws Exception {
        recoveryTest("009");
    }

    @Test
    public void testRec010() throws Exception {
        recoveryTest("010");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec011() throws Exception {
        recoveryTest("011");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec012() throws Exception {
        recoveryTest("012");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec013() throws Exception {
        recoveryTest("013");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec014() throws Exception {
        recoveryTest("014");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec015() throws Exception {
        recoveryTest("015");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec016() throws Exception {
        recoveryTest("016");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec017() throws Exception {
        recoveryTest("017");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec018() throws Exception {
        recoveryTest("018");
    }

    @Test
    public void testRec047() throws Exception {
        recoveryTest("047");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec048() throws Exception {
        recoveryTest("048");
    }

    @Test
    public void testRec050() throws Exception {
        recoveryTest("050");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec051() throws Exception {
        recoveryTest("051");
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec090() throws Exception {
        recoveryTest("090");
    }

    protected void recoveryTest(String id) throws Exception {
        final String method = "recoveryTest";
        StringBuilder sb = null;
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        server.waitForStringInLog("Dump State:");

        ProgramOutput po = server.startServerAndValidate(false, true, true);

        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(this.getClass(), method, "start server failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po = server.startServerAndValidate(false, true, true);

            // If it fails again then we'll report the failure
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "recoveryTest", ex);
                throw ex;
            }
        }

        // Server appears to have started ok
        server.waitForStringInTrace("Setting state from RECOVERING to ACTIVE");
        Log.info(this.getClass(), method, "calling checkRec" + id);
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "checkRec" + id);
        } catch (Exception e) {
            Log.error(this.getClass(), "recoveryTest", e);
            throw e;
        }
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);
    }
}
