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

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
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
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_DUPXID002")
    public static LibertyServer server2;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server1, APP_NAME, "servlets.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "servlets.*");

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);

        FATUtils.startServers(server1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<ProgramOutput>() {

            @Override
            public ProgramOutput run() throws Exception {
                FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W" }, server1); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
                return null;
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

        assertNotNull(server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

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

        assertNotNull(server2.waitForStringInLog(XAResourceImpl.DUMP_STATE));

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
