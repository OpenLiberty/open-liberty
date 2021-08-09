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

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Custom login module that adds a custom private credential, public credential and principal to the subject
 */
public class CustomJaasLoginModule implements LoginModule {

	protected Map<String, ?> _sharedState;
	protected Subject _subject = null;
	protected CallbackHandler _callbackHandler;
	
	private final String managerUser = "user1";
	
    /**
	 * Initialization of login module
	 */
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		_sharedState = sharedState;
		_subject = subject;
		_callbackHandler = callbackHandler;
	}

	@Override
	public boolean login() throws LoginException {
		System.out.println(Constants.CUSTOM_LOGIN_MODULE_MESSAGE);
		_subject.getPrivateCredentials().add(Constants.CUSTOM_LOGIN_MODULE_CRED + " = true");
		WSTestPrincipal principal = new WSTestPrincipal(managerUser, managerUser, WSTestPrincipal.AUTH_METHOD_BASIC);
		_subject.getPrincipals().add(principal);
		
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		return true;
	}

	@Override
	public boolean abort() {
		cleanup();
		return true;
	}

	@Override
	public boolean logout() {
		cleanup();
		return true;
	}

	/**
	 * Clears the subject
	 */
	private void cleanup() {
		_subject.getPrivateCredentials().clear();
	}

}