/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@AllowedFFDC(value = { "javax.transaction.SystemException" })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SingleRecoveryTest {
	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("WSATSingleRecovery");
	private static String BASE_URL = "http://" + server.getHostname() + ":"
			+ server.getHttpDefaultPort();
	private final static int REQUEST_TIMEOUT = 10;
    private static final int LOG_SEARCH_TIMEOUT = 300000;


	@BeforeClass
	public static void beforeTests() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer();
		}

		ShrinkHelper.defaultDropinApp(server, "recoveryClient", "com.ibm.ws.wsat.fat.client.recovery.*");
		ShrinkHelper.defaultDropinApp(server, "recoveryServer", "com.ibm.ws.wsat.fat.server.*");

		if (server != null && !server.isStarted()) {
			server.startServer(true);
		}

		server.setServerStartTimeout(600000);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer(true, true, ".*"); // ensure server has stopped
		}
	}

	@Test
	@Mode(TestMode.LITE)
	public void WSTXREC001FVT() throws Exception {
		recoveryTest("01");
	}

	@Test
	public void WSTXREC002FVT() throws Exception {
		recoveryTest("02");
	}

	@Test
	public void WSTXREC003FVT() throws Exception {
		recoveryTest("03");
	}

	@Test
	@Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC004FVT() throws Exception {
		recoveryTest("04");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC005FVT() throws Exception {
		recoveryTest("05");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC006FVT() throws Exception {
		recoveryTest("06");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException" })
	@Mode(TestMode.LITE)
	public void WSTXREC007FVT() throws Exception {
		recoveryTest("07");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC008FVT() throws Exception {
		recoveryTest("08");
	}

	@Test
	public void WSTXREC009FVT() throws Exception {
		recoveryTest("09");
	}

	@Test
	@Mode(TestMode.LITE)
	public void WSTXREC010FVT() throws Exception {
		recoveryTest("10");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC011FVT() throws Exception {
		recoveryTest("11");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC012FVT() throws Exception {
		recoveryTest("12");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	@Mode(TestMode.LITE)
	public void WSTXREC013FVT() throws Exception {
		recoveryTest("13");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC014FVT() throws Exception {
		recoveryTest("14");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	// Should be expected but that doesn't seem to work with multiple ffdc
	// summaries
	public void WSTXREC015FVT() throws Exception {
		recoveryTest("15");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	@Mode(TestMode.LITE)
	// Should be expected but that doesn't seem to work with multiple ffdc
	// summaries
	public void WSTXREC016FVT() throws Exception {
		recoveryTest("16");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	// Should be expected but that doesn't seem to work with multiple ffdc
	// summaries summaries
	public void WSTXREC017FVT() throws Exception {
		recoveryTest("17");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	// Should be expected but that doesn't seem to work with multiple ffdc
	// summaries
	public void WSTXREC018FVT() throws Exception {
		recoveryTest("18");
	}

	@Test
	@Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC037FVT() throws Exception {
		recoveryTest("37");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC038FVT() throws Exception {
		recoveryTest("38");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.SystemException" })
	public void WSTXREC039FVT() throws Exception {
		recoveryTest("39");
	}

	@Test
	@Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.SystemException" })
	public void WSTXREC040FVT() throws Exception {
		recoveryTest("40");
	}

	@Test
	// - removed pending defect 194984
	// Jon's comment before fix: This test is wrong I think. Can't see where
	// server is restarted for the second time
	@AllowedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC041FVT() throws Exception {
		recoveryTest("41");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException" })
	@ExpectedFFDC(value = { "javax.transaction.RollbackException" })
	public void WSTXREC042FVT() throws Exception {
		recoveryTest("42");
	}

	@Test
	@Mode(TestMode.LITE)
	//	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC043FVT() throws Exception {
		recoveryTest("43");
	}

	@Test
	// Jon's comment before fix: This test is wrong. Trace is saying things
	// like commit is not the expected direction but the doc for the test
	// says everything should commit
	@AllowedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC044FVT() throws Exception {
		recoveryTest("44");
	}

	@Test
	// Jon's comment before fix: This test is wrong. It kills the server before
	// resync has completed
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC045FVT() throws Exception {
		recoveryTest("45");
	}

	@Test
	@Mode(TestMode.LITE)
	@AllowedFFDC(value = { "java.net.SocketException" })
	public void WSTXREC046FVT() throws Exception {
		recoveryTest("46");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC047FVT() throws Exception {
		recoveryTest("47");
	}

	@Test
	@ExpectedFFDC(value = { "java.lang.RuntimeException", "com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC048FVT() throws Exception {
		recoveryTest("48");
	}

	protected void recoveryTest(String id) throws Exception {
		final String method = "recoveryTest";
		String result = null;
		String logKeyword = "Jordan said in test: ";
		System.out.println(logKeyword + "========== recoveryTest " + id
				+ " start ==========");
		try {
			// We expect this to fail since it is gonna crash the server
			System.out.println(logKeyword + "callServlet with setupRec and "
					+ id + " start");
			result = callServlet("setupRec" + id);
			System.out.println(logKeyword + "callServlet with setupRec and "
					+ id + " end");
		} catch (Throwable e) {
		}
		Log.info(this.getClass(), method, "setupRec" + id + " returned: "
				+ result);

		System.out.println(logKeyword + "waitForStringInLog Dump State start");
		server.waitForStringInLog("Dump State:");
		System.out.println(logKeyword + "waitForStringInLog Dump State end");

		ProgramOutput po;
		// Some tests kill the server twice so we need an extra restart here
		if (id.equals("42") || id.equals("41")) {
			try {
				po = server.startServerAndValidate(false, false, false, true);
				System.out.println("Start server return code: " + po.getReturnCode());
			} catch (TopologyException e) {
				e.printStackTrace(System.out);
			}

			// make sure it's dead
			server.waitForStringInLog("Dump State:");
		}

		po = server.startServerAndValidate(false, true, true);
		System.out.println("Start server return code: " + po.getReturnCode());

		// Server appears to have started ok
		server.waitForStringInTrace("Performed recovery for "+server.getServerName(), LOG_SEARCH_TIMEOUT);

//		if (id.equals("37") || id.equals("38") || id.equals("39")
//				|| id.equals("40") || id.equals("45")) {
//			try {
//				System.out
//						.println("Sleep some seconds for test " + id + " before RecoveryCheckServlet...");
//				Thread.sleep(30000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		Log.info(this.getClass(), method, "callServlet checkRec" + id);
		try {
			System.out.println(logKeyword + "callServlet checkRec " + id
					+ " start");
			result = callServlet("checkRec" + id);
			System.out.println(logKeyword + "callServlet checkRec " + id
					+ " start");
			System.out.println(logKeyword
					+ "callServlet finally get result: " + result);
		} catch (Exception e) {
			Log.error(this.getClass(), "recoveryTest", e);
			throw e;
		}

		Log.info(this.getClass(), method, "checkRec" + id + " returned: "
				+ result);
		System.out.println(logKeyword + "********** recoveryTest " + id
				+ " end **********");
	}

	private String callServlet(String testMethod) throws IOException {
		String servletName = "";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		int testNumber = Integer.parseInt(testMethod.substring(8, 10));
		if (testMethod.startsWith("setupRec")) {
			servletName = "RecoverySetupServlet";
			if (testNumber == 1)
				expectedConnectionCode = HttpURLConnection.HTTP_NOT_FOUND;
		} else if (testMethod.startsWith("checkRec")) {
			servletName = "RecoveryCheckServlet";
			expectedConnectionCode = HttpURLConnection.HTTP_OK;
		}

		String providerURL = BASE_URL;
		String urlStr = BASE_URL + "/recoveryClient/" + servletName
				+ "?number=" + testNumber + "&baseurl=" + providerURL;
		System.out.println(testMethod + " URL: " + urlStr);
		String result = "";
		HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
			BufferedReader br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
		} finally {
			con.disconnect();
		}
		assertNotNull(result);
		System.out
				.println("Recover test " + testNumber + " Result : " + result);
		assertTrue("Get emplty reply from server: " + result,
				!result.equals(""));
		assertTrue("Cannot get expected success reply from server: " + result,
				result.contains("get resource states successfully"));
		return "";
	}
}
