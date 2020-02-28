/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security;

import java.security.cert.Certificate;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationException;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationService;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A Proxy class for authenticating the users
 * <ul>
 * <li>When Messaging Security is disabled, it returns an Unauthenticated
 * subject for all the login methods</li>
 * <li>When Messaging Security is enabled, it calls the Messaging Authentication
 * Service to authenticate</li>
 * </ul>
 * 
 * @author Sharath Chandra B
 * 
 */
public class Authentication {

	// Trace component for the Authentication class
	private static TraceComponent tc = SibTr.register(Authentication.class,
			MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
			MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

	// Absolute class name along with the package used for tracing
	private static final String CLASS_NAME = "com.ibm.ws.messaging.security.Authentication.";

	/* Authentication Service for messaging, exists only if security for
	   messaging is enabled */
	private MessagingAuthenticationService messagingAuthenticationService = null;

	/* RuntimeSecurityService is a singleton instance and used to query if
	   Messaging Security is enabled or not */
	private RuntimeSecurityService runtimeSecurityService = RuntimeSecurityService.SINGLETON_INSTANCE;

	/**
	 * Login method to authenticate a user based on UserName and Password sent
	 * <ul>
	 * <li>If Security is enabled, it calls the MessagingAuthenticationService
	 * for authenticating</li> 
	 * <li>If Security is disabled, it returns an Unauthenticated Subject</li>
	 * </ul>
	 * 
	 * @param userName
	 *            UserName of the User
	 * @param password
	 *            Password for the User
	 * @return subject - If User is authenticated null - If User is
	 *         unauthenticated
	 */
	public Subject login(String userName, String password) throws MessagingAuthenticationException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "login", new Object[] { userName,
					"Password Not Traced" });
		}
		Subject result = null;
		if (!runtimeSecurityService.isMessagingSecure()) {
			result = runtimeSecurityService.createUnauthenticatedSubject();
		} else {
			if (messagingAuthenticationService != null) {
				result = messagingAuthenticationService.login(userName,
						password);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "login", result);
		}
		return result;
	}

	/**
	 * Login method to authenticate based on the Subject
	 * <ul>
	 * <li>If Security is enabled, it calls the MessagingAuthenticationService 
	 * for authenticating</li>
	 * <li>If Security is disabled, it returns a Unauthenticated Subject</li>
	 * </ul>
	 * 
	 * @param subject
	 *            The Subject (mostly already authenticated) passed from
	 *            authorization code
	 * @return subject - If User is authenticated null - If User is
	 *         unauthenticated
	 */
	public Subject login(Subject subject) throws MessagingAuthenticationException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "login", subject);
		}
		Subject result = null;
		if (!runtimeSecurityService.isMessagingSecure()) {
			result = runtimeSecurityService.createUnauthenticatedSubject();
		} else {
			if (messagingAuthenticationService != null) {
				result = messagingAuthenticationService.login(subject);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "login", result);
		}
		return result;
	}

	/**
	 * Login method to authenticate based on Token (LTPA Token authentication)
	 * <ul>
	 * <li>If Security is enabled, it calls the MessagingAuthenticationService
	 * for authenticating</li>
	 * <li>If Security is disabled, it returns an Unauthenticated Subject</li>
	 * </ul>
	 * 
	 * @param securityToken
	 *            Security Token for authentication
	 * @param securityTokenType
	 *            Type of the Security Token (LTPA is supported)
	 * @return subject - If User is authenticated null - If User is
	 *         unauthenticated
	 */
	public Subject login(byte[] securityToken, String securityTokenType) throws MessagingAuthenticationException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "login", securityTokenType);
		}
		Subject result = null;
		if (!runtimeSecurityService.isMessagingSecure()) {
			result = runtimeSecurityService.createUnauthenticatedSubject();
		} else {
			if (messagingAuthenticationService != null) {
				result = messagingAuthenticationService.login(securityToken,
						securityTokenType);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "login", result);
		}
		return result;
	}

	/**
	 * Login method to authenticate based on the Certificates
	 * <ul>
	 * <li>If Security is enabled, it calls the MessagingAuthenticationService
	 *  for authenticating</li>
	 * <li>If Security is disabled, it returns a Unauthenticated Subject</li>
	 * </ul>
	 * 
	 * @param certificates
	 *            Certificates used for authentication
	 * @return subject - If User is authenticated null - If User is
	 *         unauthenticated
	 */
	public Subject login(Certificate[] certificates) throws MessagingAuthenticationException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "login", certificates);
		}
		Subject result = null;
		if (!runtimeSecurityService.isMessagingSecure()) {
			result = runtimeSecurityService.createUnauthenticatedSubject();
		} else {
			if (messagingAuthenticationService != null) {
				result = messagingAuthenticationService.login(certificates);
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "login", result);
		}
		return result;
	}

	/**
	 * Logout method is used only for auditing purpose
	 * 
	 * @param subject
	 *            Subject which needs to be logged out
	 */
	public void logout(Subject subject) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "logout");
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "logout");
		}
	}

	/**
	 * Set the MessagingAuthenticationService
	 * 
	 * @param messagingAuthenticationService
	 */
	public void setMessagingAuthenticationService(
			MessagingAuthenticationService messagingAuthenticationService) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, CLASS_NAME + "setMessagingAuthenticationService",
					messagingAuthenticationService);
		}
		this.messagingAuthenticationService = messagingAuthenticationService;
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, CLASS_NAME + "setMessagingAuthenticationService");
		}
	}
}
