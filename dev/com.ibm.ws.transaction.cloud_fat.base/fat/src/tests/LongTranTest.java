/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LongTranTest extends CloudTestBase {

    public static final String APP_NAME = "longtran";
    public static final String SERVLET_NAME = APP_NAME + "/LongtranServlet";

    @Server("com.ibm.ws.transaction_longtran")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.longtran.web.*");

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    /**
     * The purpose of this test is to verify the fix for issue 23669, where logs can be prematurely
     * deleted in the presence of running transactions.
     *
     * The Cloud001 server is started a little transactional work is done through the normalTran servlet, then a thread
     * is spawned to start a transaction, the thread sleeps, no resources are enlisted. The server is meantime shutdown.
     *
     * As described in issue 23669, the result is an FFDC resulting from an attempt to delete an open recovery log file.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testLongRunningTranAtShutdown() throws Exception {

        serversToCleanup = Arrays.asList(server1);

        // Start Server1
        FATUtils.startServers(server);
        runTest(server, SERVLET_NAME, "normalTran");
        runTest(server, SERVLET_NAME, "longRunningTran");

        // Stop server1
        // "WTRN0075W", "WTRN0076W", "CWWKE0701E" error messages are expected/allowed
        FATUtils.stopServers(new String[] { "CWWKE0701E" }, server);
    }

    /**
     * The purpose of this test is to verify the fix for issue 24104, when the Transaction Service shuts down laggard
     * transactions will be marked as rollbackOnly.
     *
     * The Cloud001 server is started a little transactional work is done through the normalTran servlet, then a thread
     * is spawned to start a transaction, the thread sleeps, resources are enlisted. The server is meantime shutdown.
     * We verify that the long running tran is marked rollbackOnly.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testLongRunningTranMarkedRBOnlyAtShutdown() throws Exception {

        serversToCleanup = Arrays.asList(server1);

        // Start Server1
        FATUtils.startServers(server);
        runTest(server, SERVLET_NAME, "normalTran");
        runTest(server, SERVLET_NAME, "longRunningTranRBOnly");

        // Stop server1
        // "WTRN0075W", "WTRN0076W", "CWWKE0701E" error messages are expected/allowed
        FATUtils.stopServers(new String[] { "CWWKE0701E" }, server);

        // Server appears to have stopped ok. Check for key string to see whether the long running tran was marked rollback only
        assertNotNull("transaction not marked rollback only", readTranState().contains("marked rollbackOnly"));
    }

    @After
    public void after() throws Exception {

        // Clean up XA resource files
        server.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    private String readTranState() throws Exception {
        String stateString = "";

        RemoteFile rf = server.getFileFromLibertyInstallRoot("/usr/shared/longtran.dat");
        System.out.println("readTranState: Reading state from " + rf.getAbsolutePath() + " of size " + rf.length());
        InputStream is = rf.openForReading();

        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(is, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0;) {
            out.append(buffer, 0, numRead);
        }
        stateString = out.toString();
        System.out.println("readTranState: read string - " + stateString);

        return stateString;
    }
}