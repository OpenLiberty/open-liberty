/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.csiv2;

/**
 * Evaluates whether the received identity (ID) is trusted to assert ID's.
 * 
 */
public interface TrustedIDEvaluator
{
    /**
     * Determine if the user is trusted to assert ID's.
     * This is typically the user information from an LTPA token.
     * The token keys are used to establish trust in the identity.
     * 
     * @param user the user to perform the authorization check for.
     * 
     * @return <code>true</code> if the user is trusted, <code>false</code>
     *         otherwise.
     * 
     */
    public boolean isTrusted(String user);

    /**
     * Determine if the user/password is trusted to assert ID's.
     * This is typically either the server identity or an alternate identity
     * which is specified by the sending server and should be known by
     * this trust mechanism.
     * 
     * @param user the user to perform the authorization check for
     * @param password the password for the user.
     * 
     * @return <code>true</code> if the user is trusted, <code>false</code>
     *         otherwise.
     * 
     */
    public boolean isTrusted(String user, String password);

    /**
     * Determine if the user associated with the cert chain is trusted
     * to assert ID's.
     * 
     * This certificate is typically the sending server's identity
     * which must be trusted to assert IDs.
     * 
     * @param cert the cert chain associated with the user
     * 
     * @return <code>true</code> if the user associated with the cert chain is trusted, <code>false</code>
     *         otherwise.
     * 
     */
    public boolean isTrusted(java.security.cert.X509Certificate[] certChain);

}
