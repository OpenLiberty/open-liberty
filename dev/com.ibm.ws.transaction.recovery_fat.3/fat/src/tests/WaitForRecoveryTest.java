/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class WaitForRecoveryTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/WaitForRecoveryServlet";

    @Server("com.ibm.ws.transaction_waitForRecovery")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server1, APP_NAME, "web");

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W" }, server1); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected

        // Clean up XA resource file
        server1.deleteFileFromLibertyServerRoot("XAResourceData.dat");
    }

    @SuppressWarnings("deprecation")
    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testWaitForRecovery() throws Exception {
        final String method = "testBaseRecovery";

        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, SERVLET_NAME, "testRec001");
            fail();
        } catch (IOException e) {
        }

        assertNotNull(server1.getServerName() + " failed to crash", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
        server1.resetLogOffsets(); // Dunno how you would do this with log marks

        try {
            final ProgramOutput po = server1.startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                String str = po.getStdout();
                if (str != null && !str.isEmpty()) {
                    Log.info(this.getClass(), method, "Stdout: " + str);
                }
                str = po.getStderr();
                if (str != null && !str.isEmpty()) {
                    Log.info(this.getClass(), method, "Stderr: " + str);
                }
                // Not really bothered. Checks below will tell us whether app start waited.
            }
        } catch (Exception e) {
        }

        // Check for key string to see whether recovery has succeeded
        assertNotNull(server1.getServerName() + " did not perform recovery", server1.waitForStringInTraceUsingLastOffset("Performed recovery for " + server1.getServerName()));

        // Check app start happened afterwards
        assertNotNull("App must have started first", server1.waitForStringInTraceUsingLastOffset("Starting application " + APP_NAME));
    }
}