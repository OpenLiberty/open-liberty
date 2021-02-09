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

package com.ibm.ws.security.client.fat.programmaticlogin;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.ibm.websphere.samples.technologysamples.basiccalcclient.BasicCalculatorClientJ2EEMain;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.client.fat.Constants;

public class ProgrammaticLoginApp {

	private static String user = Constants.USER_1;
	private static String password = Constants.USER_1_PWD;
	private static String loginContext = Constants.CLIENT_CONTAINER_LOGIN_CONTEXT;
	private static boolean useCallbackHandler = true;

	public static String operation = "multiply";
	public static String operand1 = "2";
	public static String operand2 = "4";

	/**
	 * Performs a programmatic login. Arguments are expected to be in the following order:
	 * <p>
	 * 	args[0]: User name<br>
	 * 	args[1]: User password<br>
	 * 	args[2]: Login context<br>
	 * 	args[3]: Operation (add, subtract, multiply, divide)<br>
	 * 	args[4]: Operand 1 (double value)<br>
	 * 	args[5]: Operand 2 (double value)<br>
	 * <p>
	 * If any argument is missing, the default values of {@value Constants#USER_1}, {@value Constants#USER_1_PWD},
	 * {@value Constants#CLIENT_CONTAINER_LOGIN_CONTEXT}, {@value #operation}, {@value #operand1}, and {@value #operand2} are used.
	 * @param args
	 */
	public static void main(String args[]) {
		parseArguments(args);
		System.out.println("User set to: " + user);
		System.out.println("Password set to: " + password);
		System.out.println("Login context set to: " + loginContext);

		ProgrammaticLoginApp app = new ProgrammaticLoginApp();
		LoginContext ctx = app.login(user, password, loginContext);
		if (ctx == null) {
			System.out.println("Failed to obtain login context.");
			return;
		}
		Subject subject = ctx.getSubject();
		if (subject == null) {
			System.out.println("Failed to obtain subject.");
			return;
		}
		System.out.println("Subject: " + subject);
		Set<Principal> principals = subject.getPrincipals();
		if (principals.isEmpty()) {
			System.out.println("No principals were contained in the subject.");
			return;
		}

		app.performEJBCall(ctx);

		System.out.println("End of client application.");
	}

	/**
	 * Parses the given arguments. Arguments are expected to be in the following order:
	 * <p>
	 * 	args[0]: User name<br>
	 * 	args[1]: User password<br>
	 * 	args[2]: Login context<br>
	 * 	args[3]: Operation (add, subtract, multiply, divide)<br>
	 * 	args[4]: Operand 1 (double value)<br>
	 * 	args[5]: Operand 2 (double value)<br>
	 * 	args[...]: Any number of additional optional arguments<br>
	 * <p>
	 * If more than 3 arguments are provided, an operation and two operands must be specified, for a total of at least 5
	 * arguments. If more than 3 but less than 6 arguments are provided, an error is output to System.err.
	 * @param args
	 */
	private static void parseArguments(String[] args) {
		if (args.length > 0) {
			user = args[0];
			if (user.equals(Constants.NULL_STRING)) {
				user = null;
			} else if (user.equals(Constants.EMPTY_STRING)) {
				user = "";
			}

			if (args.length > 1) {
				password = args[1];
				if (password.equals(Constants.NULL_STRING)) {
					password = null;
				} else if (password.equals(Constants.EMPTY_STRING)) {
					password = "";
				}

				if (args.length > 2) {
					loginContext = args[2];

					// If more than 3 arguments were provided, we expect the rest to include at least an operation (string) and two operands (doubles)
					if (args.length > 3) {
						if (args.length < 6) {
							System.err.println("An operation was provided as an argument but insufficient operands were provided.");
							return;
						}
						operation = args[3];
						operand1 = args[4];
						operand2 = args[5];

						if (args.length > 6) {
							for (int i = 6; i < args.length; i++) {
								String additionalArg = args[i];
								if (additionalArg.equals(Constants.NO_CALLBACK_HANDLER)) {
									useCallbackHandler = false;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Performs a programmatic login with the specified user credentials and login context. If successful, the LoginContext
	 * object obtained after login is returned.
	 * 
	 * @param userName
	 * @param userPassword
	 * @param loginContext
	 * @return Returns the LoginContext object obtained from performing the programmatic login. If any exceptions are caught,
	 * {@code null} is returned.
	 */
	public LoginContext login(String userName, String userPassword, String loginContext) {
		CallbackHandler wscbh = new WSCallbackHandlerImpl(userName, userPassword);
		LoginContext ctx = null;
		try {
			System.out.println("Obtaining login context");
			if (useCallbackHandler) {
				ctx = new LoginContext(loginContext, wscbh);
			} else {
				ctx = new LoginContext(loginContext);
			}
		} catch (LoginException le) {
			System.err.println("Could not create LoginContext. " + le.getMessage());
			le.printStackTrace();
			return null;
		}

		try {
			System.out.println("Logging in");
			ctx.login();
		} catch (LoginException le) {
			System.err.println("Failed to log in: " + le.getMessage());
			le.printStackTrace();
			return null;
		}

		return ctx;
	}

	/**
	 * Performs an EJB invocation using the subject contained in the provided LoginContext object. Any exceptions caught will
	 * have their messages output to System.err.
	 * 
	 * @param ctx
	 */
	@SuppressWarnings("rawtypes")
	public void performEJBCall(LoginContext ctx) {
		Subject subject = ctx.getSubject();
		WSSubject.doAs(subject, new PrivilegedAction() {
			public Object run() {
				try {
					// Perform EJB lookup and invocation
					System.out.println("Instantiating client EJB calculator application");
					BasicCalculatorClientJ2EEMain calcclient = new BasicCalculatorClientJ2EEMain();
					System.out.println("Attempting to perform EJB operation");
					calcclient.doCalculation(new String[]{operation, operand1, operand2});
				} catch (Exception ex) {
					System.err.println("Caught an unexpected exception when performing EJB lookup: " + ex.getMessage());
					ex.printStackTrace();
				}
				return null;
			}
		});
		try {
			System.out.println("Logging out");
			ctx.logout();
		} catch (LoginException e) {
			System.err.println("Caught an unexpected LoginException when performing logout: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
