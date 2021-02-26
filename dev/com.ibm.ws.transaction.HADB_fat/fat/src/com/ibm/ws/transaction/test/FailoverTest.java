/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.FailoverServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests are designed to exercise the ability of the SQLMultiScopeRecoveryLog (transaction logs stored
 * in a database) to recover from transient SQL errors, such as those encountered when a High Availability (HA)
 * database fails over.
 *
 * They work as follows:
 *
 * -> A JDBC driver is implemented that wraps the underlying Derby driver. The driver is provided in a jar
 * named ifxjdbc.jar. This is inferred to be an Informix driver by the Liberty JDBC driver code.
 *
 * The wrapper code generally passes calls straight through to the real jdbc driver it wraps but, when prompted,
 * it can generate SQLExceptions that will be flowed to calling code, such as SQLMultiScopeRecoveryLog.
 *
 * -> The JDBC driver is configured through a table named HATABLE with 3 columns,
 * testtype - is this a runtime or startup test
 * failoverval - how many SQL operations should be executed before generating an SQLException
 * simsqlcode - what sqlcode should be passed in the generated SQLexception
 *
 * Note, in modern versions of jdbc drivers, the driver will generate SQLTransientExceptions rather than SQLExceptions
 * with a specific sqlcode value.
 *
 * -> Each test starts by (re)creating HATable and inserting a row to specify the test characteristics.
 *
 * ->The tests will drive a batch of 2PC transactions using artificial XAResourceImpl resources. The jdbc driver will
 * generate a SQLException at a point defined in the HATABLE row.
 */
@Mode
@RunWith(FATRunner.class)
public class FailoverTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's
        // written to a file
        // Include the 'app1.web' package and all of it's java classes and
        // sub-packages
        // Automatically includes resources under
        // 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/
        // directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<ProgramOutput>() {

            @Override
            public ProgramOutput run() throws Exception {
                return server.stopServer("WTRN0075W", "WTRN0076W"); // Stop the
                // server
                // and
                // indicate
                // the
                // '"WTRN0075W",
                // "WTRN0076W"
                // error
                // messages
                // were
                // expected
            }
        });
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC(value = { "java.sql.SQLRecoverableException" })
    public void testHADBRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBRecoverableRuntimeFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "Call setupForRecoverableFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForRecoverableFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setupForRecoverableFailover returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver");

        server.stopServer();

        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        server.startServerAndValidate(false, true, true);

        Log.info(this.getClass(), method, "Call testDriveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
        } catch (Throwable e) {
        }

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", server.waitForStringInLog("Have recovered from SQLException"));
        Log.info(this.getClass(), method, "Complete");
    }

    /**
     * Run a set of transactions and simulate an unexpected sqlcode
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.transaction.SystemException", "java.sql.SQLRecoverableException", "java.lang.Exception" })
    // Defect RTC171085 - an XAException may or may not be generated during
    // recovery, depending on the "speed" of the recovery relative to work
    // going on in the main thread. It is most sensible to make the potential
    // set of observable FFDCs allowable.
    public void testHADBNonRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBNonRecoverableRuntimeFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call setupForNonRecoverableFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForNonRecoverableFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer();
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        server.startServerAndValidate(false, true, true);

        Log.info(this.getClass(), method, "complete");
        Log.info(this.getClass(), method, "call driveTransactionsWithFailure");
        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactionsWithFailure");
        } catch (Throwable e) {
        }

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", server.waitForStringInLog("Cannot recover from SQLException"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0029E", "WTRN0066W");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        server.startServerAndValidate(false, true, true);
        Log.info(this.getClass(), method, "call testControlSetup");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testControlSetup");
        } catch (Throwable e) {
        }
        server.waitForStringInLog("testControlSetup complete");
        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        server.waitForStringInLog("recover\\(");
    }

    /**
     * Simulate an HA condition at server start (testing log open error
     * handling))
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHADBRecoverableStartupFailover() throws Exception {
        final String method = "testHADBRecoverableStartupFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call setupForStartupFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForStartupFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer();
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        server.startServerAndValidate(false, true, true);
        Log.info(this.getClass(), method, "call driveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "complete");
    }

    private static void logny(String text) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/temp/HADBTranlogTest.txt", true)));
            java.util.Date date = new java.util.Date();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
            String formattedDate = sdf.format(date);
            out.println(formattedDate + ": " + text);
            out.close();
        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }
    }
}
