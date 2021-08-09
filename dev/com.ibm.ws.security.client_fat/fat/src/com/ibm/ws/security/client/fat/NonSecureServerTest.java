/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class NonSecureServerTest extends CommonTest {
	private static final Class<?> c = NonSecureServerTest.class;

	/**
	 * Creates a server and runs its application
	 */
	@BeforeClass
	public static void theBeforeClass() throws Exception {
		String thisMethod = "before";
		Log.info(c, thisMethod, "Performing the server setup for all test classes");

		try {
			commonServerSetUp("NonSecureServerTest", false);
		} catch (Exception e) {
			Log.info(c, thisMethod, "Server setup failed, tests will not run: " + e.getMessage());
			throw (new Exception("Server setup failed, tests will not run: " + e.getMessage(), e));
		}

		Log.info(c, thisMethod, "Server setup is complete");
	};

	@AfterClass
	public static void theAfterClass() {
		try {
			Log.info(c, "after", "stopping server process.");
			testServer.stopServer("CWWKZ0124E: Application testmarker does not contain any modules.");
		} catch (Exception e) {
			Log.info(c, "after", "Exception thrown in after " + e.getMessage());
			e.printStackTrace();
		}
	};

	/**
	 * Test description:
	 * - Start the client and call a protected EJB with an unauthorized user name and password in client.xml.
	 * - The server does not have the appSecurity-2.0 feature defined.
	 * 
	 * Expected results:
	 * - We should see the EJB call pass despite the unauthorized credentials since security is not enabled on the server.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testNoSecurityOnServerTest() {
		try {
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_unauthorizedUser.xml");
			String output = programOutput.getStdout();

			assertTrue("Client should report it called EJB successfully.", output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

}
