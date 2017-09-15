/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security.authentication.actions;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.security.MSTraceConstants;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * MessagingLoginAction is responsible for authenticating the users of Liberty Messaging
 * This acts as the common point for all types of Messaging Authentications
 * It calls the Liberty Security component's AuthenticationService to authenticate the User
 * 
 * @author Sharath Chandra B
 *
 */
public class MessagingLoginAction implements MessagingSecurityConstants, PrivilegedAction<Subject> {
	
	// Trace component for the MessagingLoginAction class
	private static TraceComponent tc = SibTr.register(MessagingLoginAction.class,
			MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
			MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

	// Absolute class name along with the package name, used for tracing
	private static final String CLASS_NAME = "com.ibm.ws.messaging.security.authentication.actions.MessagingLoginAction";
	
	// The Authentication Service exposed by Liberty Security Component
	protected static AuthenticationService _authenticationService = null;

	// This is the place holder for Authentication. Data required for Authentication are stored here
	protected AuthenticationData _authenticationData = null;
	
	// Specifies which LoginType we are using
	protected String _loginType = null;
	
	/*
	 * When a User is authenticated, we just pass the authenticated subject
	 * while we are trying to do authorization. This is the partial subject
	 */
	protected Subject _partialSubject = null;

	/**
	 * Constructor
	 * @param authData
	 * 		AuthenticationData Object
	 * @param loginType
	 * 		Type of Login
	 * @param securityService
	 * 		Security Service
	 */
	public MessagingLoginAction(AuthenticationData authData, String loginType, SecurityService securityService) {
		this(authData, loginType, securityService, null);
	}

	/**
	 * Constructor
	 * @param authData
	 * 		AuthenticationData Object
	 * @param loginType
	 * 		Type of Login
	 * @param securityService
	 * 		Security Service
	 * @param partialSubject
	 * 		Partial Subject is the subject which was already authenticated
	 */
	public MessagingLoginAction(AuthenticationData authData, String loginType, SecurityService securityService, Subject partialSubject) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "constructor", new Object[] { authData,
					loginType, securityService });
		}
		_authenticationData = authData;
		_loginType = loginType;
		_authenticationService = getAuthenticationService(securityService);
		_partialSubject = partialSubject;
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "constructor");
		}
	}
	
	/**
	 * Get Authentication Service from the Liberty Security component
	 * It will get the AuthenticationService only if the SecurityService is activated
	 * @param securityService
	 * @return
	 */
	public AuthenticationService getAuthenticationService(SecurityService securityService) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "getAuthenticationService", securityService);
		}
		if(_authenticationService == null) {
			if (securityService != null)
				_authenticationService = securityService
						.getAuthenticationService();
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "getAuthenticationService", _authenticationService);
		}
		return _authenticationService;
	}

	/**
	 * @see PrivilegedAction#run()
	 */
	public final Subject run() {
		return login();
	}
	
	/**
	 * This method returns the type of the login being performed.
	 * 
	 * @return The login type
	 */
	public String getLoginType() {
		return _loginType;
	}
	
	/**
	 * The method to authenticate a User
	 * @return
	 * 		Subject: If the User is authenticated
	 * 		Null   : If User is not authenticated
	 */
	protected Subject login() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "login");
		}
		Subject subject = null;
		try {
			/*
			 * Only if we have the AuthenticationService running, we can do
			 * Authentication. If it is not present we cannot do any
			 * authentication and hence we have return null, which means
			 * authentication failed
			 */
			if (_authenticationService != null) {
				subject = _authenticationService.authenticate(MESSAGING_JASS_ENTRY_NAME,
						_authenticationData, _partialSubject);
			}
		} catch (AuthenticationException ae) {
			// No FFDC Required. We will throw exception if the subject is Null later
			if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				SibTr.debug(tc, "EXCEPTION_OCCURED_DURING_AUTHENTICATION_MSE1001");
				SibTr.exception(tc, ae);
			}
		} finally {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
				SibTr.exit(tc, CLASS_NAME + "login");
			}
		}
		return subject;
	}

}
