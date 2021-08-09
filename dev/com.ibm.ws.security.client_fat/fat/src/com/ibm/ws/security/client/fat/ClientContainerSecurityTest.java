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

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
public class ClientContainerSecurityTest extends CommonTest {
	private static final Class<?> c = ClientContainerSecurityTest.class;

	/**
	 * Creates a server and runs its application
	 */
	@BeforeClass
	public static void theBeforeClass() throws Exception {
		String thisMethod = "before";
		Log.info(c, thisMethod, "Performing the server setup for all test classes");

		try {
			commonServerSetUp("SecureServerTest", true);
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
	 * - start the client and check log results for validation of signer prompt.
	 * 
	 * Expected results:
	 * - We should see validation message that SSL signer prompt was displayed.
	 */
	@Test
	public void testSSLPromptClientTest() {
		try {
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = commonClientSetUp("mySSLPromptClient", "CWWKS9702W", "CWPKI0823E");
			String output = programOutput.getStdout();

			assertTrue("Client should report it found signer prompt string",
					output.contains("SSL SIGNER EXCHANGE PROMPT"));
			assertTrue("Client should report signer from target not found",
					output.contains("The SSL signer from target host is not found in trust store"));
			boolean sslKeyFileExists = testClient.fileExistsInLibertyClientRoot("resources/security/key.jks");
			assertTrue("Could not find ssl key.jksb file",sslKeyFileExists);
			if (sslKeyFileExists) {
				testClient.deleteFileFromLibertyClientRoot("resources/security/key.jks");
			}
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - check for validation the security utility command executed,  verify key file is created.
	 * 
	 * Expected results:
	 * - We should see that the client keystore was created.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testSSLClientCmdTest() {
		try {
			Log.info(c, name.getMethodName(), "Executing the security utility to create client ssl key ...");
			ProgramOutput programOutput = commonClientSetUp("mySSLCmdClient");
			String output = programOutput.getStdout();

			assertTrue("Client should report it has started", output.contains("CWWKF0035I"));
			Machine localMachine = testClient.getMachine();
			String sslCmd = " createSSLCertificate --client=mySSLCmdClient --password=s3cur1ty";
			runSecurityUtility(localMachine, sslCmd, "mySSLCmdClient");
			boolean sslKeyFileExists = testClient.fileExistsInLibertyClientRoot("resources/security/key.p12");
			assertTrue("Could not find ssl key.p12 file",sslKeyFileExists);
			if (sslKeyFileExists) {
				testClient.deleteFileFromLibertyClientRoot("resources/security/key.p12");
			}
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - check that the filemonitor detected the client keystore was updated.
	 * 
	 * Expected results:
	 * - We should validate message for key file being updated.
	 */
	@Test
	public void testFileMonitorTest() {
		try {
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = commonClientSetUpWithParms("myFileMonitorClient", "CWWKS9702W");
			String output = programOutput.getStdout();

			boolean sslKeyFileExists = testClient.fileExistsInLibertyClientRoot("resources/security/key.jks");
			assertTrue("Could not find ssl key.jks file",sslKeyFileExists);
			if (sslKeyFileExists) {
				testClient.deleteFileFromLibertyClientRoot("resources/security/key.jks");
			}
			assertTrue("Client should report it has updated the keystore", output.contains("CWPKI0811I"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}
	
	/**
	 * Test description:
	 * - start the client and check log results for validation of signer prompt.
	 * 
	 * Expected results:
	 * - We should see validation message that SSL signer prompt was displayed.
	 */
	@Mode(TestMode.LITE)
	@Test
    @AllowedFFDC(value = { "java.security.cert.CertPathValidatorException", "sun.security.validator.ValidatorException", "javax.naming.CommunicationException" })
	public void testSSLAutoAcceptClientTest() {
		try {
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = commonClientSetUpAutoAcceptWithParms("mySSLAutoAcceptClient", null, null);
			String output = programOutput.getStdout();

			assertTrue("Client should report it has started", output.contains("CWWKF0035I"));
			assertTrue("Should get a message the certificate is accepted.", output.contains("CWPKI0308I"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}


}
