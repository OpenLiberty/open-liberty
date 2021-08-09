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

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.DupXidServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class DupXidTest extends FATServletClient {

    //Prior to the change made under RTC 179941 server's could not distinguish Xids that belonged to them. The test works as follows...
    //
    // A. Cleanup database resources
    // B. Start server1
    // C. Invoke servlet that enlists resources and dies in commit, leaving indoubt tran with Xid belonging to server1.
    // D. Start server2
    // E. Invoke servlet that enlists resources and dies in commit, leaving indoubt tran with Xid belonging to server2.
    // F. Start server1
    // G. Under recovery processing server1 should commit the Xid that it owns and ignore server2's Xid. Note that during recovery processing,
    // the simulated resource manager will report 2 Xids to the TM, that belonging to server1 and server2. But only server1's Xid should be recovered.
    //
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/DupXidServlet";

    @Server("com.ibm.ws.transaction_DUPXID001")
    @TestServlet(servlet = DupXidServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_DUPXID002")
    @TestServlet(servlet = DupXidServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.web.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.web.*");

        server1.setServerStartTimeout(300000);
        server2.setServerStartTimeout(300000);

//        server1.copyFileToLibertyInstallRoot("lib/features", "features/cloudtxfat-1.0.mf");
//        assertTrue("Failed to install cloudtxfat-1.0 manifest",
//                   server1.fileExistsInLibertyInstallRoot("lib/features/cloudtxfat-1.0.mf"));
//        server1.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.cloudtx.fat.utils.jar");
//        assertTrue("Failed to install cloudtxfat-1.0 bundle",
//                   server1.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.cloudtx.fat.utils.jar"));

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<ProgramOutput>() {

            @Override
            public ProgramOutput run() throws Exception {
                return server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
            }
        });
    }

    @Test
    @AllowedFFDC(value = { "java.sql.SQLSyntaxErrorException" })
    public void testDupXid() throws Exception {
        // Clean up any existing database resources. This includes the XAResources table together with the Tran log and partner log table that may
        // contain unresolved trans from a previous run of the test. We assume (hope!) that the resources associated with server1 are in a consistent
        // state!
        runTest(server1, SERVLET_NAME, "cleanDatabase");

        final String method = "TestDupXid";
        StringBuilder sb = null;
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupDupXid001");
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupDupXid001 returned: " + sb);

        assertNotNull(server1.waitForStringInLog("Dump State:"));

        // Now start dupXid2
        ProgramOutput po = server2.startServerAndValidate(false, true, true);

        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(this.getClass(), method, "start server failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po = server2.startServerAndValidate(false, true, true);

            // If it fails again then we'll report the failure
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "TestDupXid", ex);
                throw ex;
            }
        }

        // Server appears to have started ok. So lets crash it!!
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server2, SERVLET_NAME, "setupDupXid002");
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupDupXid002 returned: " + sb);

        assertNotNull(server2.waitForStringInLog("Dump State:"));

        // Now start dupXid1
        ProgramOutput po1 = server1.startServerAndValidate(false, true, true);
        if (po1.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po1.getCommand() + " returned " + po1.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po1.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po1.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(this.getClass(), method, "start server failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po1 = server1.startServerAndValidate(false, true, true);

            // If it fails again then we'll report the failure
            if (po1.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po1.getCommand() + " returned " + po1.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po1.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po1.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "TestDupXid", ex);
                throw ex;
            }
        }

        //Check for key string to see whether recovery has been attempted
        assertNotNull(server1.waitForStringInTrace("Performed recovery for com.ibm.ws.transaction_DUPXID001"));
    }

}
