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
public class BasicTest extends CommonTest {
	private static final Class<?> c = BasicTest.class;

	/**
	 * Creates a server and runs its application
	 */
	@BeforeClass
	public static void theBeforeClass() throws Exception {
		String thisMethod = "before";
		Log.info(c, thisMethod, "Performing the server setup for all test classes");

		try {
			commonServerSetUp("BasicAuthTest", false);
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
	 * - start the client and call protected EJB with client user name and password in client.xml.
	 * 
	 * Expected results:
	 * - We should see EJB call was successful message
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testValidUserAndPasswordTest () {
		String output = null;
		Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
		try {
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT);
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it called EJB successfully.", output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));
		assertNoErrMessages(output);
	}

	/**
	 * Test description:
	 * - start the client and check expect failure because the user is invalid.
	 * 
	 * Expected results:
	 * - We should see a CORBA No Permission exception
	 */
	@Test
	public void testInvalidUserValidPasswordTest () {
		String output = null;
		Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
		try {
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_badUser.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report a CORBA.NO_PERMISSION exception.", output.contains(Constants.BAD_ID_PERMISSION_ERROR));

	}

	/**
	 * Test description:
	 * - Start the client and call a protected EJB with an unauthorized user name and password in client.xml.
	 * 
	 * Expected results:
	 * - Authorization for the EJB invocation should fail and a CORBA.NO_PERMISSION exception should be seen.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testUnauthorizedUser() {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_unauthorizedUser.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		// Check EJB invocation result
		assertTrue("The expected AccessException for lack of permission was not found.",
				output.contains("org.omg.CORBA.NO_PERMISSION: :  vmcid: 0x0 minor code: 0x0  completed: No"));
		// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

	}

	/**
	 * Test description:
	 * - start the client and check expect failure because the user's password is invalid.
	 * 
	 * Expected results:
	 * - We should see a CORBA No Permission exception
	 */
	@Test
	public void testValidUserInvalidPasswordTest () {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_badPassword.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report a CORBA.NO_PERMISSION exception.", output.contains(Constants.BAD_ID_PERMISSION_ERROR));
	}

	/**
	 * Test description:
	 * - start the client and check expect failure because the user is an empty string.
	 * 
	 * Expected results:
	 * - We should see a CORBA No Permission exception
	 */
	@Test
	public void testEmptyUserValidPasswordTest () {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_emptyUser.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report a CORBA.NO_PERMISSION exception.", output.contains(Constants.BAD_ID_PERMISSION_ERROR));
	}

	/**
	 * Test description:
	 * - start the client and check expect failure because the user's password is an empty string.
	 * 
	 * Expected results:
	 * - We should see a CORBA No Permission exception
	 */
	@Test
	public void testValidUserEmptyPasswordTest () {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_emptyPassword.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report a CORBA.NO_PERMISSION exception.", output.contains(Constants.BAD_ID_PERMISSION_ERROR));
	}

	/**
	 * Test description:
	 * - start the client and call protected EJB with no client user name and password in client.xml.
	 * 
	 * Expected results:
	 * - We should see EJB call failed with no permission message
	 */
	@Test
	public void testNoUserOrPasswordTest() {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_noUserPassword.xml", "CWWKS9702W");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it got CORBA.NO_PERMISSION exception.", output.contains("CORBA.NO_PERMISSION"));
	}

	/**
	 * Test description:
	 * - start the client and call protected EJB with client user name and password in client.xml.
	 * - the client will generate ssl key file
	 * 
	 * Expected results:
	 * - We should see EJB call failed with SSL Prompt to accept server signer
	 * - Note that since the key generation may take more than 10 seconds which is the timeout value of ORB code.
	 *   add upto 30 seconds wait time if the keystore doesn't exist. This check is carried out in the Calc application.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testNoDefaultKeyTest() {
		String output = null;
		Log.info(c, name.getMethodName(), "Starting the client " + Constants.NO_DEFAULT_KEY_CLIENT);
		try {
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.NO_DEFAULT_KEY_CLIENT, null, 30, "CWPKI0823E");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it found signer prompt string", output.contains("SSL SIGNER EXCHANGE PROMPT"));
	}

	/**
	 * Test description:
	 * - start the client and call protected EJB with client user name and xor encoded password in client.xml.
	 * 
	 * Expected results:
	 * - We should see EJB call was successful message
	 */
	@Test
	public void testValidXorPasswordTest () {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_xorPassword.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it called EJB successfully.", output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));
		assertNoErrMessages(output);
	}

	/**
	 * Test description:
	 * - start the client and call protected EJB with client user name and aes encoded password in client.xml.
	 * 
	 * Expected results:
	 * - We should see EJB call was successful message
	 */
	@Test
	public void testValidAesPasswordTest () {
		String output = null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_aesPassword.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it called EJB successfully.", output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));
		assertNoErrMessages(output);
	}
	
	/**
	 * Test description:
	 * - start the client and check expect failure because there is no client security configured and no keystore.
	 * 
	 * Expected results:
	 * - We should see a CORBA TRANSIENT exception
	 */
	@Test
	public void testNoClientSecurityNoClientKeystoreTest () {
		String output=null;
		try {
			Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_noSecNoKeystore.xml", "CWWKI0003E");
			output = programOutput.getStdout();

			
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should complain that it needs security.", output.contains(Constants.CLIENT_NEEDS_SECURITY_ERROR));
		//for reasons not yet clear, the exception you get can be either a COM_FAILURE or TRANSIENT.
		assertTrue("Client should report an org.omg.CORBA.COMM_FAILURE or org.omg.CORBA.TRANSIENT exception.", output.contains(Constants.CLIENT_CONNECT_EXCEPTION_1)
				||output.contains(Constants.CLIENT_CONNECT_EXCEPTION_2));
	}
	
	/**
	 * Test description:
	 * - start the client and check expected failure because we do not wait for SSL to be properly configured
	 * 
	 * Expected results:
	 * - We should see two error messages:
	 * <li>The client was unable to initialize the BasicCalculatorClient object</li>
	 * <li>A javax.naming.NameNotFoundException because we did not wait for SSL</li>
	 */
	@Test
	public void testClientSecurityNoClientKeystoreTest () {
		Log.info(c, name.getMethodName(), "Starting the client " + Constants.MY_TEST_CLIENT);
		String output = null;
		try {
			ProgramOutput programOutput = commonClientSetUpWithCalcArgs(Constants.MY_TEST_CLIENT, "client_withSecNoKeystore.xml");
			output = programOutput.getStdout();
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
		assertTrue("Client should report it was unable to initialize the BasicCalculatorClient object.", output.contains("Unable to initialize the BasicCalculatorClient"));
		assertTrue("Client should report an org.omg.CORBA.COMM_FAILURE, org.omg.CORBA.TRANSIENT or NameNotFoundException exception.", output.contains(Constants.CLIENT_CONNECT_EXCEPTION_1)
				||output.contains(Constants.CLIENT_CONNECT_EXCEPTION_2)
			    ||output.contains("javax.naming.NameNotFoundException"));
	}
	
}
