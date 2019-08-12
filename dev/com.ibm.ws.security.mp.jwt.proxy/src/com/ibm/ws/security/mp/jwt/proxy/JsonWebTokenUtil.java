/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.proxy;

import java.security.Principal;
import java.util.Hashtable;

import javax.security.auth.Subject;

public interface JsonWebTokenUtil {
	/*
	 * Retrieve the JsonWebToken from the subject's hashtable and adding it in the
	 * subject as a Principal
	 */
	public void addJsonWebToken(Subject subject, Hashtable<String, ?> customProperties, String key);

	/*
	 * Retrieve the JsonWebToken from the subject and return it as a Principal
	 */
	public Principal getJsonWebTokenPrincipal(Subject subject);

	/*
	 * Clone the JsonWebToken from the subject and return it as a Principal
	 */
	public Principal cloneJsonWebToken(Subject subject);

	/**
	 * Creates a JsonWebToken from the given jwt, type, and username then returns it
	 * as a Principal
	 */
	public Principal getJsonWebToken(String jwt, String type, String username);

	/**
	 * adds a jwt string to the list of jwt's that have been logged out
	 */
	public void addLoggedOutJwtToList(String jwt);

}