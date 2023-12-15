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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.WaitForRecoveryServlet;

@RunWith(FATRunner.class)
public class WaitForRecoveryTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/WaitForRecoveryServlet";

    @Server("com.ibm.ws.transaction_waitForRecovery")
    @TestServlet(servlet = WaitForRecoveryServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server1, APP_NAME, "web");

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server1);
    }

    @SuppressWarnings("deprecation")
    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testWaitForRecovery() throws Exception {
        final String method = "testBaseRecovery";
        StringBuilder sb = null;

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "testRec001");
            fail(sb.toString());
        } catch (IOException e) {
        }
        Log.info(this.getClass(), method, "testRec001 returned: " + sb);

        assertNotNull(server1.getServerName() + " failed to crash", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
        server1.resetLogOffsets(); // Dunno how you would do this with log marks

        // Now re-start cloud1
        ProgramOutput po = server1.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not restart the server");
            Log.error(this.getClass(), "WaitForRecoveryTest", ex);
            throw ex;
        }

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull(server1.getServerName() + " did not perform recovery", server1.waitForStringInTraceUsingLastOffset("Performed recovery for " + server1.getServerName()));
        assertNotNull("App must have started first", server1.waitForStringInTraceUsingLastOffset("Starting application " + APP_NAME));

        // Lastly stop server1
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W" }, server1); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected

        // Lastly, clean up XA resource file
        server1.deleteFileFromLibertyServerRoot("XAResourceData.dat");
    }
}