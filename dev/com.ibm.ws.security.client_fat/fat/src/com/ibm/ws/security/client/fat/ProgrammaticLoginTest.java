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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ProgrammaticLoginTest extends CommonTest {
	private static final Class<?> c = ProgrammaticLoginTest.class;

	@BeforeClass
	public static void theBeforeClass() throws Exception {
		String thisMethod = "before";
		Log.info(c, thisMethod, "Performing server setup");
		try {
			commonServerSetUp("BasicAuthTest", true);
		} catch (Exception e) {
			Log.info(c, thisMethod, "Server setup failed, tests will not run: " + e.getMessage());
			throw (new Exception("Server setup failed, tests will not run: " + e.getMessage(), e));
		}

		Log.info(c, thisMethod, "Server setup is complete");
	};

	@AfterClass
	public static void theAfterClass() {
		try {
			Log.info(c, "after", "Stopping server process");
			testServer.stopServer("CWWKZ0124E: Application testmarker does not contain any modules.");
		} catch (Exception e) {
			Log.info(c, "after", "Exception thrown in after " + e.getMessage());
			e.printStackTrace();
		}
	};

	/**
	 * Test description:
	 * - Perform a programmatic login with the default login context.
	 * - Perform an EJB operation using the valid subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should be successful and a subject for the specified user should be returned.
	 * - The EJB operation should be successful.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testProgrammaticLogin() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with default credentials and login context");
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", Constants.NO_ARGS, "CWWKS9702W");
			String output = programOutput.getStdout();

			String user = Constants.USER_1;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// Check EJB invocation result
			Pattern pattern = Pattern.compile(".+Result:\\s*2\\s*\\*\\s*4\\s*=\\s*8.+", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(output);
			assertTrue("Did not find the expected calculator operation.", matcher.matches());
			assertTrue("Did not find expected message saying the EJB call was successful.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with a null user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLogin_NullUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with a null user");
			List<String> args = new ArrayList<String>();
			args.add(Constants.NULL_STRING);
			args.add(Constants.USER_1_PWD);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with an empty string as the user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLogin_EmptyUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with an empty user");
			List<String> args = new ArrayList<String>();
			args.add(Constants.EMPTY_STRING);
			args.add(Constants.USER_1_PWD);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with a user not defined in the server's registry.
	 * - Attempt to invoke an EJB with the subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should be successful and a subject for the undefined user should be present.
	 * - Authorization for the EJB invocation should fail.
	 */
	@Test
	public void testProgrammaticLogin_UserNotDefined() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with a user not defined in the server's registry");
			String user = Constants.BAD_USER;
			List<String> args = new ArrayList<String>();
			args.add(user);
			args.add(Constants.USER_1_PWD);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains(Constants.BAD_ID_PERMISSION_ERROR));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with a user not authorized to perform a certain EJB operation.
	 * - Perform the EJB operation using the unauthorized subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should be successful and a subject for the specified user should be returned.
	 * - Authorization for the EJB invocation should fail.
	 */
	@Test
	public void testProgrammaticLogin_UnauthorizedUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login and EJB invocation with unauthorized credentials");
			String user = Constants.USER_UNAUTHORIZED;
			List<String> args = new ArrayList<String>();
			args.add(user);
			args.add(Constants.USER_UNAUTHORIZED_PWD);
			args.add(Constants.CLIENT_CONTAINER_LOGIN_CONTEXT);
			args.add(Constants.EMPLOYEE_OPERATION);
			args.add("1");
			args.add("2");
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains("org.omg.CORBA.NO_PERMISSION: :  vmcid: 0x0 minor code: 0x0  completed: No"));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with a null password.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLogin_NullPassword() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with a null password");
			List<String> args = new ArrayList<String>();
			args.add(Constants.USER_1);
			args.add(Constants.NULL_STRING);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with an empty string as the password.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLogin_EmptyPassword() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with an empty password");
			List<String> args = new ArrayList<String>();
			args.add(Constants.USER_1);
			args.add(Constants.EMPTY_STRING);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with an invalid password.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testProgrammaticLogin_BadPassword() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with an invalid password");
			String user = Constants.USER_1;
			List<String> args = new ArrayList<String>();
			args.add(user);
			args.add(Constants.BAD_PWD);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains(Constants.BAD_ID_PERMISSION_ERROR));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with a custom login module.
	 * - Perform an EJB operation using the valid subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should be successful and a subject for the specified user should be returned.
	 * - Attributes specific to the custom login module should be present in the subject.
	 * - The EJB operation should be successful.
	 */
	@Test
	public void testProgrammaticLogin_CustomLoginModule() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with a custom login module");
			String user = Constants.USER_1;
			List<String> args = new ArrayList<String>();
			args.add(user);
			args.add(Constants.USER_1_PWD);
			args.add(Constants.CLIENT_CONTAINER_LOGIN_CONTEXT);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_customLoginModule.xml", args, "CWWKS1108E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output did not contain the message expected to be output by the custom login module.",
					output.contains(Constants.CUSTOM_LOGIN_MODULE_MESSAGE));
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));
			assertTrue("Client output did not contain the expected custom private credential.",
					output.contains("Private Credential: " + Constants.CUSTOM_LOGIN_MODULE_CRED));

			// Check EJB invocation result
			Pattern pattern = Pattern.compile(".+Result:\\s*2\\s*\\*\\s*4\\s*=\\s*8.+", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(output);
			assertTrue("Did not find the expected calculator operation.", matcher.matches());
			assertTrue("Did not find expected message saying the EJB call was successful.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with the default login context.
	 * - Valid user credentials are also specified in client.xml.
	 * - Perform an EJB operation using the valid subject obtained from the programmatic login.
	 * 
	 * Expected results:
	 * - Login should be successful and a subject for the programmatically specified user should be returned.
	 * - Credentials for the user configured in client.xml should not be present.
	 * - The EJB operation should be successful.
	 */
	@Test
	public void testProgrammaticLogin_CredentialsAlsoInClientXml() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login with default credentials and credentials specified in client.xml");
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_withCredentials.xml", Constants.NO_ARGS);
			String output = programOutput.getStdout();

			String user = Constants.USER_1;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// The credentials for the user configured in client.xml should not be found
			assertFalse("A security name was found for " + Constants.USER_5 + " when it should not have been.",
					output.contains("securityName=" + Constants.USER_5));

			// Check EJB invocation result
			Pattern pattern = Pattern.compile(".+Result:\\s*2\\s*\\*\\s*4\\s*=\\s*8.+", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(output);
			assertTrue("Did not find the expected calculator operation.", matcher.matches());
			assertTrue("Did not find expected message saying the EJB call was successful.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login with the default login context but without providing a callback handler.
	 * 
	 * Expected results:
	 * - Login should fail due to the lack of a callback handler.
	 */
	@Test
	public void testProgrammaticLoginWithoutCallbackHandler() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login without specifying a callback handler");
			List<String> args = new ArrayList<String>();
			args.add(Constants.USER_1);
			args.add(Constants.USER_1_PWD);
			args.add(Constants.CLIENT_CONTAINER_LOGIN_CONTEXT);
			args.add(Constants.EMPLOYEE_OPERATION);
			args.add("1");
			args.add("2");
			args.add(Constants.NO_CALLBACK_HANDLER);
			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_orig.xml", args, "CWWKS1170E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected message saying the callback handler was null.",
					output.contains(Constants.MSG_NULL_CALLBACK_HANDLER));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - Perform an EJB operation using the valid subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should should be successful.
	 * - The subject returned should be for user2, who is hard coded as the user to use in the callback handler specified
	 * in application-client.xml.
	 * - Credentials for user1 should not be present.
	 * - The EJB operation should be successful.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testProgrammaticLoginWithCallbackHandler() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using the callback handler specified in application-client.xml");
			List<String> args = new ArrayList<String>();
			args.add(Constants.USER_1);
			args.add(Constants.USER_1_PWD);
			args.add(Constants.CLIENT_CONTAINER_LOGIN_CONTEXT);

			String operand1 = "2";
			String operand2 = "3";
			String answer = "5";
			args.add(Constants.EMPLOYEE_OPERATION);
			args.add(operand1);
			args.add(operand2);

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			// The callback handler is hard coded to use user2's credentials instead of user1
			String user = Constants.USER_2;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			assertFalse("A security name was found for " + Constants.USER_1 + " when it should not have been.",
					output.contains("securityName=" + Constants.USER_1));

			// Check EJB invocation result
			Pattern pattern = Pattern.compile(".+Result:\\s*" + operand1 + "\\s*\\+\\s*" + operand2 + "\\s*=\\s*" + answer + ".+", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(output);
			assertTrue("Did not find the expected calculator operation (" + operand1 + "+" + operand2 + "=" + answer + ").", matcher.matches());
			assertTrue("Did not find expected message saying the EJB call was successful.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies a null user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_NullUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses a null user");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_nullUser.xml", Constants.NO_ARGS, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies an empty string for the user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_EmptyUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses an empty string user");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_emptyUser.xml", Constants.NO_ARGS, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies a user that doesn't exist in the server's registry.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_UserNotDefined() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses a user not defined in the server's registry");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_userNotDefined.xml", Constants.NO_ARGS, "CWWKS9702W");
			String output = programOutput.getStdout();

			// The callback handler is hard coded to use an undefined user in the credentials instead of user1
			String user = Constants.BAD_USER;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains(Constants.BAD_ID_PERMISSION_ERROR));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies a user not authorized to perform the necessary EJB operation.
	 * - Perform the EJB operation using the valid subject obtained from the login.
	 * 
	 * Expected results:
	 * - Login should should be successful.
	 * - The subject returned should be for the unauthorized user, who is hard coded as the user to use in the callback handler
	 * specified in application-client.xml.
	 * - Credentials for user1 should not be present.
	 * - Authorization for the EJB invocation should fail.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_UnauthorizedUser() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses an unauthorized user");
			List<String> args = new ArrayList<String>();
			args.add(Constants.USER_1);
			args.add(Constants.USER_1_PWD);
			args.add(Constants.CLIENT_CONTAINER_LOGIN_CONTEXT);
			args.add(Constants.EMPLOYEE_OPERATION);
			args.add("2");
			args.add("3");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_unauthzUser.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			// The callback handler is hard coded to use an unauthorized user's credentials instead of user1
			String user = Constants.USER_UNAUTHORIZED;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			assertFalse("A security name was found for " + Constants.USER_1 + " when it should not have been.",
					output.contains("securityName=" + Constants.USER_1));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains("org.omg.CORBA.NO_PERMISSION: :  vmcid: 0x0 minor code: 0x0  completed: No"));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies a null password for the user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_NullPwd() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses a null password");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_nullPwd.xml", Constants.NO_ARGS, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies an empty string for the password for the user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_EmptyPwd() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses an empty string as the password");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_emptyPwd.xml", Constants.NO_ARGS, "CWWKS1171E", "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output should show that we failed to obtain a login context, but no such message was found.",
					output.contains("Failed to obtain login context"));
			assertTrue("Did not find the expected null user name or password message in the client output.",
					output.contains(Constants.MSG_NULL_USER_OR_PASSWORD));

			assertFalse("Client output contained a public credential even though authentication should have failed.",
					output.contains("Public Credential:"));
			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specifies a bad password for the user.
	 * 
	 * Expected results:
	 * - Login should fail and no subject should be contained in the response.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_BadPwd() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler specified in application-client.xml that uses a bad password");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_badPwd.xml", Constants.NO_ARGS, "CWWKS9702W");
			String output = programOutput.getStdout();

			// The callback handler is hard coded to use user2's credentials instead of user1
			String user = Constants.USER_2;
			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			assertFalse("A security name was found for " + Constants.USER_1 + " when it should not have been.",
					output.contains("securityName=" + Constants.USER_1));

			// Check EJB invocation result
			assertTrue("The expected AccessException for lack of permission was not found.",
					output.contains(Constants.BAD_ID_PERMISSION_ERROR));
			// TODO This expectation needs to be changed to check for the correct message once work item 169189 is resolved

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using the WSLogin login context.
	 * - A callback handler is also specified in the application's application-client.xml file which specifies a different user.
	 * - Perform an EJB operation using the valid subject obtained from the programmatic login.
	 * 
	 * Expected results:
	 * - Login should should be successful.
	 * - The subject returned should be for the programmatic user, not the user configured in the callback handle specified in
	 * application-client.xml.
	 * - Credentials for user2 should not be present.
	 * - The EJB operation should be successful.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_WSLoginContext() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using the WSLogin login context alongside a callback handler specified in application-client.xml");
			String user = Constants.USER_1;
			List<String> args = new ArrayList<String>();
			args.add(user);
			args.add(Constants.USER_1_PWD);
			args.add(Constants.WS_LOGIN_CONTEXT);

			String operand1 = "8";
			String operand2 = "6";
			String answer = "2";
			args.add(Constants.MANAGER_OPERATION);
			args.add(operand1);
			args.add(operand2);

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler.xml", args, "CWWKS9702W");
			String output = programOutput.getStdout();

			assertTrue("Client output did not contain a public credential, so an error must have occurred obtaining the subject.",
					output.contains("Public Credential:"));
			assertTrue("Client output did not contain the expected WSPrincipal name " + user,
					output.contains("WSPrincipal:" + user));
			assertTrue("Client output did not contain the expected user name " + user,
					output.contains("securityName=" + user));

			// The callback handler is hard coded to use user2's credentials instead of user1, but the programmatic credentials should take precedence
			assertFalse("A security name was found for " + Constants.USER_2 + " when it should not have been.",
					output.contains("securityName=" + Constants.USER_2));

			// Check EJB invocation result
			Pattern pattern = Pattern.compile(".+Result:\\s*" + operand1 + "\\s*\\-\\s*" + operand2 + "\\s*=\\s*" + answer + ".+", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(output);
			assertTrue("Did not find the expected calculator operation (" + operand1 + "+" + operand2 + "=" + answer + ").", matcher.matches());
			assertTrue("Did not find expected message saying the EJB call was successful.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - Perform a programmatic login using a callback handler specified in the application's application-client.xml file.
	 * - The callback handler specified does not exist.
	 * 
	 * Expected results:
	 * - Client startup should fail with a CWWKZ0002E message including a ClassNotFoundException for the callback handler.
	 * - No login or EJB invocation should take place.
	 */
	@Test
	public void testProgrammaticLoginWithCallbackHandler_NonexistentHandler() {
		try {
			Log.info(c, name.getMethodName(), "Performing programmatic login using a callback handler that doesn't exist specified in application-client.xml.");

			ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticLoginTestClient", "client_appWithCallbackHandler_nonexistentHandler.xml", Constants.NO_ARGS, "CWWKZ0002E", "CWWKZ0130E", "CWWKS9702W");
			String output = programOutput.getStdout();

			Pattern pattern = Pattern.compile(".+^.+?CWWKZ0002E.+?ClassNotFoundException:.+?NonexistentCallbackHandler\\s*$.+", Pattern.DOTALL | Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(output);
			if (!matcher.matches()) {
				fail("The expected ClassNotFoundException for the nonexistent callback handler was not found in the output.");
			}

			assertFalse("Found the message saying the EJB call was successful even though no EJB invocation should have been made.",
					output.contains(Constants.SUCCESSFUL_EJB_CALL_MSG));

			assertNoErrMessages(output);

		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

}
