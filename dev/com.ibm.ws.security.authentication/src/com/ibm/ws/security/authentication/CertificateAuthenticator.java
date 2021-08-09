/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication;

import java.security.cert.X509Certificate;

import javax.security.auth.login.LoginException;

/**
 * This class:
 * - authenticates a certificate chain
 * - if successfully authenticated returns the components that will comprise the
 * Subject's accessid: type, realm, username
 * (an accessid has the general format: type:realm/username)
 */
public interface CertificateAuthenticator {
    /**
     * 
     * @param certChain
     * @return true - certificate authenticated
     *         false - certificate not authenticated, continue authenticating
     * @throws LoginException - authentication failed, fail the request
     */
    boolean authenticateCertificateChain(X509Certificate certChain[]) throws LoginException;

    /**
     * Return the credential type: AccessidUtil.TYPE_USER, AccessidUtil.TYPE_GROUP
     * 
     * @return credential type
     */
    public String getType();

    /**
     * Return the realm name
     * 
     * @return realm name
     */
    public String getRealm();

    /**
     * Return the username
     * 
     * @return username
     */
    public String getUsername();

}
