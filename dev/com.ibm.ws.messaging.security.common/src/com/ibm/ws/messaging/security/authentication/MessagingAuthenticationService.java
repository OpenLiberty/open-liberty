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

package com.ibm.ws.messaging.security.authentication;

import java.security.cert.Certificate;

import javax.security.auth.Subject;

/**
 * Authentication Service Interface for Messaging Component
 * This is responsible for Authenticating the Users based on various authentication methods
 * Users can authenticate using
 * <ul>
 * <li> User Name and Password </li>
 * <li> Security Token (only LTPA supported) </li>
 * <li> Certificates </li>
 * <li> Already authenticated Subject </li>
 * <li> IDAssertion </li>
 * </ul>
 * @author Sharath Chandra B
 *
 */
public interface MessagingAuthenticationService {
	
	/**
	 * Login using a Subject
	 * @param subject
	 * @return
	 * 		Subject: If User is authenticated
	 * 		Null   : If User is unauthenticated
	 * @throws
	 * 		MessagingAuthenticationException
	 */
	public Subject login(Subject subject) throws MessagingAuthenticationException;
	
	/**
	 * Login using User Name and Password
	 * @param userName
	 * @param password
	 * @return
	 *  	Subject: If User is authenticated
	 * 		Null   : If User is unauthenticated
	 * @throws
	 * 		MessagingAuthenticationException
	 */
	public Subject login(String userName, String password) throws MessagingAuthenticationException;

	/**
	 * Login using Security Token
	 * @param securityToken
	 * @param securityTokenType
	 * @return
	 * 		Subject: If User is authenticated
	 * 		Null   : If User is unauthenticated
	 * @throws
	 * 		MessagingAuthenticationException
	 */
	public Subject login(byte[] securityToken, String securityTokenType) throws MessagingAuthenticationException;

	/**
	 * Login using just the UserName
	 * @param userName
	 * @return
	 *  	Subject: If User is authenticated
	 * 		Null   : If User is unauthenticated
	 * @throws
	 * 		MessagingAuthenticationException
	 */
	public Subject login(String userName) throws MessagingAuthenticationException;

	/**
	 * Login using Certificates
	 * @param certificates
	 * @return
	 *  	Subject: If User is authenticated
	 * 		Null   : If User is unauthenticated
	 * @throws
	 * 		MessagingAuthenticationException
	 */
	public Subject login(Certificate[] certificates) throws MessagingAuthenticationException;

	/**
	 * Logout the Subject which is already logged in
	 * @param subject
	 */
	public void logout(Subject subject);
	
}
