/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

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

	@Mode(TestMode.LITE)
	@Test
	public void testHADBControl() throws Exception {
		final String method = "testHADBControl";
		StringBuilder sb = null;
		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testControlSetup");
		} catch (Throwable e) {
		}
		Log.info(this.getClass(), method, "testControlSetup returned: " + sb);

		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
		} catch (Throwable e) {
		}
	}

	/**
	 * Run a set of transactions and simulate an HA condition
	 */
	@Mode(TestMode.LITE)
	@Test
	@ExpectedFFDC(value = { "java.sql.SQLRecoverableException" })
	public void testHADBRuntimeFailoverKnownSqlcode() throws Exception {
		final String method = "testHADBRuntimeFailoverKnownSqlcode";
		StringBuilder sb = null;

		logny("testHADBRuntimeFailoverKnownSqlcode - call testSetupKnownSqlcode");

		Log.info(this.getClass(), method, "Call testSetupKnownSqlcode");

		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testSetupKnownSqlcode");
		} catch (Throwable e) {
		}

		Log.info(this.getClass(), method, "testSetupKnownSqlcode returned: " + sb);
		Log.info(this.getClass(), method, "Call stopserver");

		server.stopServer();

		Log.info(this.getClass(), method, "set timeout");
		server.setServerStartTimeout(30000);

		Log.info(this.getClass(), method, "call startserver");
		server.startServerAndValidate(false, true, true);

		Log.info(this.getClass(), method, "Call testDriveTransactions");
		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
		} catch (Throwable e) {
		}

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
	public void testHADBRuntimeFailoverUnKnownSqlcode() throws Exception {
		final String method = "testHADBRuntimeFailoverUnKnownSqlcode";
		StringBuilder sb = null;

		Log.info(this.getClass(), method, "call testSetupUnKnownSqlcode");

		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testSetupUnKnownSqlcode");
		} catch (Throwable e) {
		}

		Log.info(this.getClass(), method, "call stopserver");
		server.stopServer();
		Log.info(this.getClass(), method, "set timeout");
		server.setServerStartTimeout(30000);
		Log.info(this.getClass(), method, "call startserver");
		server.startServerAndValidate(false, true, true);

		Log.info(this.getClass(), method, "complete");
		Log.info(this.getClass(), method, "call testDriveTransactionsWithFailure");
		// An unhandled sqlcode will lead to a failure to write to the log, the
		// invalidation of the log and the throwing of Internal LogExceptions
		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactionsWithFailure");
		} catch (Throwable e) {
		}
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
	public void testHADBStartupFailoverKnownSqlcode() throws Exception {
		final String method = "testHADBStartupFailoverKnownSqlcode";
		StringBuilder sb = null;

		Log.info(this.getClass(), method, "call testStartupSetup");

		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testStartupSetup");
		} catch (Throwable e) {
		}

		Log.info(this.getClass(), method, "call stopserver");
		server.stopServer();
		Log.info(this.getClass(), method, "set timeout");
		server.setServerStartTimeout(30000);
		Log.info(this.getClass(), method, "call startserver");
		server.startServerAndValidate(false, true, true);
		Log.info(this.getClass(), method, "call testDriveTransactions");
		try {
			sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
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
