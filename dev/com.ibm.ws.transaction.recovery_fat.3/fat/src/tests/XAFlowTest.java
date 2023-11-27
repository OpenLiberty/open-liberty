/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.XAFlowServlet;

@RunWith(FATRunner.class)
public class XAFlowTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/XAFlowServlet";

    @Server("com.ibm.ws.transaction_XAFlow")
    @TestServlet(servlet = XAFlowServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "web");

        server.copyFileToLibertyInstallRoot("lib/features/", "features/xaflow-1.0.mf");
        assertTrue("Failed to install xaflow-1.0 manifest",
                   server.fileExistsInLibertyInstallRoot("lib/features/xaflow-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.tx.test.impl.jar");
        assertTrue("Failed to install xaflow-1.0 bundle",
                   server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar"));

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);

        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                server.deleteFileFromLibertyInstallRoot("lib/features/xaflow-1.0.mf");
                assertFalse("Failed to uninstall xaflow-1.0 manifest",
                            server.fileExistsInLibertyInstallRoot("lib/features/xaflow-1.0.mf"));
                server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar");
                assertFalse("Failed to uninstall xaflow-1.0 bundle",
                            server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar"));
                return null;
            }
        });
    }

    @Test
    public void testXAFlow001() throws Exception {
        xaflowTest("001");
    }

    @Test
    public void testXAFlow002() throws Exception {
        xaflowTest("002");
    }

    protected void xaflowTest(String id) throws Exception {
        final String method = "xaflowTest";
        StringBuilder sb = null;
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server, SERVLET_NAME, "setupXAFlow" + id);
            Log.info(this.getClass(), method, "setupXAFlow" + id + " returned: " + sb);
            fail(sb.toString());
        } catch (IOException e) {
            Log.info(getClass(), method, "setupXAFlow" + id + " crashed the server as expected: " + e.getLocalizedMessage());
        }

        // Make sure we get the logs
        server.postStopServerArchive();

        server.setServerStartTimeout(300000); // 5 mins
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
                Log.error(this.getClass(), method, ex);
                throw ex;
            }
        }

        // Server appears to have started ok
        assertNotNull(server.waitForStringInTrace("Setting state from RECOVERING to ACTIVE"));

        int attempt = 0;
        while (true) {
            Log.info(this.getClass(), method, "calling checkRec" + id);
            try {
                sb = runTestWithResponse(server, SERVLET_NAME, "checkXAFlow" + id);
                Log.info(this.getClass(), method, "checkXAFlow" + id + " returned: " + sb);
                break;
            } catch (Exception e) {
                Log.error(this.getClass(), method, e);
                if (++attempt < 5) {
                    Thread.sleep(10000);
                } else {
                    throw e;
                }
            }
        }
    }
}
