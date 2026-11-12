/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.context_fat;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Custom login module that adds a custom private credential, public credential and principal to the subject
 */
public class CustomLoginModule implements LoginModule {

	protected Map<String, ?> _sharedState;
	protected Subject _subject = null;
	protected CallbackHandler _callbackHandler;

	private final String USER_MAP_TO = "user1";

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
		String username = null;
		boolean succeeded = true;

		// Get the username from the callback information
		javax.security.auth.callback.Callback callbacks[] = new javax.security.auth.callback.Callback[1];
		callbacks[0] = new javax.security.auth.callback.NameCallback("Username: ");

		try
		{
			_callbackHandler.handle(callbacks);
		} 
		catch (Exception e)
		{
			// do nothing
		}
		username = ((NameCallback) callbacks[0]).getName();
		System.out.println("-----> Callback handler returned: " + username);
		String mappedUser = USER_MAP_TO;			
		System.out.println("-----> Username mapping to: " + mappedUser);

		java.util.Hashtable<String, Object> customProperties = (java.util.Hashtable<String, Object>) _sharedState.get(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY);
		if (customProperties == null) {
			customProperties = new java.util.Hashtable<String, Object>();
		}
		customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, mappedUser);

		Map<String, java.util.Hashtable> mySharedState = (Map<String, java.util.Hashtable>) _sharedState;
		mySharedState.put(com.ibm.wsspi.security.token.AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, customProperties);
		
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