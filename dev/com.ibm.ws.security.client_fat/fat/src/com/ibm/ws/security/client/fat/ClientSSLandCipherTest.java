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
public class ClientSSLandCipherTest extends CommonTest {
	private static final Class<?> c = ClientSSLandCipherTest.class;
	public static boolean IBM_JDK = true;

	/**
	 * Creates a server and runs its application
	 */
	@BeforeClass
	public static void theBeforeClass() throws Exception {
		String thisMethod = "before";
		Log.info(c, thisMethod, "Performing the server setup for all test classes");

		String jdkVendor = System.getProperty("java.vendor");
		if (!jdkVendor.toLowerCase().contains("ibm")) {
			Log.info(c, thisMethod, "The JDK in use on this system is from a non-IBM vendor: " + jdkVendor
					+ ". Because only IBM JDK vendors are currently supported, no tests will be run.");
			IBM_JDK = false;
		}

		try {
			if (IBM_JDK) {
				commonServerSetUp("SSLCipherTest", false);
			} else {
				commonServerSetUp("SSLnonIBMCipherTest", false);	
			}
			Thread.sleep(45000);
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
	 * - start the client and call protected EJB with incompatible cipher suites on client and server.
	 * 
	 * Expected results:
	 * - We should see an exception indicating some kind of handshake failure
	 */
	@Test
	public void testClientWithCipherListTest () {
		try {
			ProgramOutput programOutput = null;
			Log.info(c, name.getMethodName(), "Starting the client ...");
			if (IBM_JDK) {
				programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_with_ibm_ciphersuite.xml", "CWWKF0040E");
			} else {
				programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_with_nonIBM_ciphersuite.xml", "CWWKF0040E");		
			}
			String output = programOutput.getStdout();

			assertTrue("Client should report it failed with handshake exception.", output.contains("handshake_failure") || output.contains("org.omg.CORBA.COMM_FAILURE")  || output.contains("Unable to initialize the BasicCalculatorClient"));
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client and check log results for validation of configuration and verify key file is created.
	 * 
	 * Expected results:
	 * - We should see that the client key file had been created.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testSSLClientTest() {
		try {
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs("noDefaultKeyClient", null, "CWPKI0823E");
			String output = programOutput.getStdout();

			assertTrue("Client should report it has started", output.contains("CWWKF0035I"));
			boolean sslKeyFileExists = testClient.fileExistsInLibertyClientRoot("resources/security/key.p12");
			assertTrue("Could not find ssl key.p12 file",sslKeyFileExists);
			if (sslKeyFileExists) {
				testClient.deleteFileFromLibertyClientRoot("resources/security/key.p12");
			}
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

}
