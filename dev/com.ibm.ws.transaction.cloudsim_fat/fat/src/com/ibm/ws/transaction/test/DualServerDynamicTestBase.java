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
package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cloudtx.ut.util.LastingXAResourceImpl;
import com.ibm.ws.transaction.web.SimpleFS2PCCloudServlet;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/*
 * These tests are based on the original JTAREC recovery tests.
 * Test plan is attached to RTC WI 213854
 */
@Mode
public abstract class DualServerDynamicTestBase extends FATServletClient {

    protected static LibertyServer serverTemplate;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/SimpleFS2PCCloudServlet";
    protected static final int FScloud2ServerPort = 9992;
    public static final String FSCloud1RecoveryIdentity = "FScloud001";

    @Server("com.ibm.ws.transaction_FSCLOUD001")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_FSCLOUD002")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @BeforeClass
    public static void setUp() throws Exception {

        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
    }

    @Test
    public void dynamicCloudRecovery001() throws Exception {
        dynamicTest(1, 2);
    }

    @Test
    public void dynamicCloudRecovery002() throws Exception {
        dynamicTest(2, 2);
    }

    @Test
    public void dynamicCloudRecovery003() throws Exception {
        dynamicTest(3, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery004() throws Exception {
        dynamicTest(4, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery005() throws Exception {
        dynamicTest(5, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery006() throws Exception {
        dynamicTest(6, 3);
    }

    @Test
    @Mode(TestMode.LITE)
    public void dynamicCloudRecovery007() throws Exception {
        dynamicTest(7, 2);
    }

    @Test
    public void dynamicCloudRecovery008() throws Exception {
        dynamicTest(8, 2);
    }

    @Test
    public void dynamicCloudRecovery009() throws Exception {
        dynamicTest(9, 2);
    }

    @Test
    public void dynamicCloudRecovery010() throws Exception {
        dynamicTest(10, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery011() throws Exception {
        dynamicTest(11, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery012() throws Exception {
        dynamicTest(12, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery013() throws Exception {
        dynamicTest(13, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery014() throws Exception {
        dynamicTest(14, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery015() throws Exception {
        dynamicTest(15, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery016() throws Exception {
        dynamicTest(16, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery017() throws Exception {
        dynamicTest(17, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery018() throws Exception {
        dynamicTest(18, 3);
    }

    @Test
    public void dynamicCloudRecovery047() throws Exception {
        dynamicTest(47, 4);
    }

    @Test
    public void dynamicCloudRecovery048() throws Exception {
        dynamicTest(48, 4);
    }

    @Test
    public void dynamicCloudRecovery050() throws Exception {
        dynamicTest(50, 10);
    }

    @Test
    public void dynamicCloudRecovery051() throws Exception {
        dynamicTest(51, 10);
    }

    @Mode(TestMode.LITE)
    @Test
    public void dynamicCloudRecovery090() throws Exception {
        dynamicTest(90, 3);
    }

    public void dynamicTest(int test, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;

        // Start Server1
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        assertNotNull("First server did not crash", server1.waitForStringInLog("Dump State:"));

        // Now start server2
        server2.setHttpDefaultPort(FScloud2ServerPort);
        ProgramOutput po = server2.startServerAndValidate(false, true, true);

        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not start server2");
            Log.error(this.getClass(), "dynamicTest", ex);
            throw ex;
        }

        // wait for 2nd server to perform peer recovery
        assertNotNull("Second server did not perform peer recovery", server2.waitForStringInTrace("Performed recovery for " + FSCloud1RecoveryIdentity));

        // flush the resource states
        try {
            sb = runTestWithResponse(server2, SERVLET_NAME, "dumpState");
            Log.info(this.getClass(), method, sb.toString());
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            throw e;
        }

        // restart 1st server
        server1.startServerAndValidate(false, true, true);

        assertNotNull("Recovery incomplete on first server", server1.waitForStringInTrace("WTRN0133I"));

        // check resource states
        Log.info(this.getClass(), method, "calling checkRec" + id);
        try {
            sb = runTestWithResponse(server1, SERVLET_NAME, "checkRec" + id);
        } catch (Exception e) {
            Log.error(this.getClass(), "dynamicTest", e);
            throw e;
        }
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

        // Bounce first server to clear log
        server1.stopServer(null);
        server1.startServerAndValidate(false, true, true);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on first server",
                      server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on first server",
                      server1.waitForStringInTrace("WTRN0134I.*0"));

        tidyServerAfterTest(server1);
        tidyServerAfterTest(server2);
        // XA resource data is cleared in setup servlet methods. Probably should do it here.
    }

    protected void tidyServerAfterTest(LibertyServer s) throws Exception {
        if (s.isStarted()) {
            s.stopServer();
        }
        try {
            final RemoteFile rf = s.getFileFromLibertySharedDir(LastingXAResourceImpl.STATE_FILE_ROOT);
            if (rf.exists()) {
                rf.delete();
            }
        } catch (FileNotFoundException e) {
            // Already gone
        }

    }
}