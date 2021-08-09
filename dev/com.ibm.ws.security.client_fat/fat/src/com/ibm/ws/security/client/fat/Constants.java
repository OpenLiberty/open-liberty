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

package com.ibm.ws.security.client.fat;

import java.util.List;
import java.util.Map;

public class Constants {

	public final static List<String> NO_ARGS = null;
	public final static List<String> NO_MSGS = null;
	public final static Map<String, String> NO_PROPS = null;

	public final static String MY_TEST_CLIENT = "myTestClient";
	public final static String NO_DEFAULT_KEY_CLIENT = "noDefaultKeyClient";
	
	public final static String USER_1 = "user1";
	public final static String USER_1_PWD = "user1pwd";
	public final static String USER_2 = "user2";
	public final static String USER_2_PWD = "user2pwd";
	public final static String USER_5 = "user5";
	public final static String USER_5_PWD = "user5pwd";
	public final static String USER_UNAUTHORIZED = USER_5;
	public final static String USER_UNAUTHORIZED_PWD = USER_5_PWD;
	public final static String BAD_USER = "badUser";
	public final static String BAD_PWD = "badPassword";

	public final static String CLIENT_CONTAINER_LOGIN_CONTEXT = "ClientContainer";
	public final static String CLIENT_CONTAINER_JAAS_LOGIN_CONTEXT = "customClientContainer";
	public final static String WS_LOGIN_CONTEXT = "WSLogin";
	public final static String NO_CALLBACK_HANDLER = "noCallbackHandler";
	public final static String NULL_STRING = "null";
	public final static String EMPTY_STRING = "empty";

	public final static String EMPLOYEE_OPERATION = "add";
	public final static String MANAGER_OPERATION = "subtract";
	public final static String ALL_AUTHENTICATED_OPERATION = "multiply";

	public final static boolean START_CLIENT = true;
	public final static boolean START_SERVER = true;
	public final static boolean DONT_START_SERVER = false;
	public final static boolean DONT_START_CLIENT = false;
	public final static boolean RESTART_SERVER = true;
	public final static boolean DONT_RESTART_SERVER = false;
	public final static boolean MESSAGE_NOT_EXPECTED = false;
	public final static boolean EXPECTED_MESSAGE = true;
	public final static boolean IGNORE_ERROR_CONTENT = true;
	public final static boolean DONT_IGNORE_ERROR_CONTENT = false;
	public final static boolean USE_APP_CALLBACK_HANDLER = true;
	public final static boolean DONT_USE_APP_CALLBACK_HANDLER = false;

	public final static String SUCCESSFUL_EJB_CALL_MSG = "The call to the EJB was successful";
	public final static String CUSTOM_LOGIN_MODULE_MESSAGE = "Adding custom private credential to subject";
	public final static String CUSTOM_LOGIN_MODULE_CRED = "CustomCredential";

	public final static boolean IS_EMPLOYEE = true;
	public final static boolean IS_NOT_EMPLOYEE = false;
	public final static boolean IS_MANAGER = true;
	public final static boolean IS_NOT_MANAGER = false;

	protected static final int DEFAULT_LOG_SEARCH_TIMEOUT = 120 * 1000;
	protected static final int MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT = 30 * 1000;

	public static final String BAD_ID_PERMISSION_ERROR = "org.omg.CORBA.NO_PERMISSION: :  vmcid: 0x49424000 minor code: 0x300  completed: No" ;
	public static final String MSG_NULL_CALLBACK_HANDLER = "CWWKS1170E: The login on the client application failed because the CallbackHandler implementation is null. Ensure a valid CallbackHandler implementation is specified either in the LoginContext constructor or in the client applications deployment descriptor.";
	public static final String MSG_NULL_USER_OR_PASSWORD = "CWWKS1171E: The login on the client application failed because the user name or password is null. Ensure the CallbackHandler implementation is gathering the necessary credentials.";
	//We don't promise that this will always be the specific exception thrown for connect-to-port-0 problems
	public static final String CLIENT_CONNECT_EXCEPTION_1 = "org.omg.CORBA.COMM_FAILURE";
	public static final String CLIENT_CONNECT_EXCEPTION_2 = "org.omg.CORBA.TRANSIENT";
	public static final String CLIENT_SEC_NO_CLIENT_KEYSTORE_FEATURE_ERROR = "CWWKE0916E: The client feature was not enabled.";
	public static final String CLIENT_SEC_NO_CLIENT_KEYSTORE_SSL_ERROR = "CWWKS9582E: The [defaultSSLConfig] sslRef attributes required by the orb element with the defaultOrb id have not been resolved within 10 seconds.";
	public static final String CLIENT_NEEDS_SECURITY_ERROR = "CWWKI0003E:";
}