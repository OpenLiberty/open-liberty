/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

/**
 * Digest authentication information.
 * @author dedi
 *
 */
public interface AuthInfo {

	/**
	 * Helper method to add authentication info into the 
	 * AuthInfo object for a challenge response of a specific 
	 * type (401/407) and realm.
	 * 
	 * @param statusCode - Status code (401/407) of the challenge response
     * @param realm - Realm that was returned in the challenge response
     * @param username - 
     * @param password - 
	 */
	void addAuthInfo(int statusCode, String realm, String username, 
			String password);
}
