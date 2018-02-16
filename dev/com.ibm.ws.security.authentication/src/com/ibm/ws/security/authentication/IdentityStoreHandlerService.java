/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication;

import javax.security.auth.Subject;

/**
 * The IdentityStoreHandlerService interface. The IdentityStoreHandlerervice implementation performs
 * the authentication given use idr and password and return partial subject for hashtable login, or
 * return partial subject for hashtable login by given user id.
 * The purpose of this service is to provide a way to create run-as user subject under
 * JavaEESecurity (JSR375) application.
 */
public interface IdentityStoreHandlerService {

    /**
     * Returns the partial subject for hashtable login
     *
     * @param username
     * @param password
     *
     * @return the partial subject which can be used for hashtable login if username and password are valid.
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    public Subject createHashtableInSubject(String username, String password) throws AuthenticationException;

    /**
     * Returns the partial subject for hashtable login
     * This method only works if the IdentityStores can validate users without using password which is a unique
     * function which the container provided IdentityStores have.
     *
     * @param username
     *
     * @return the partial subject which can be used for hashtable login if username and password are valid.
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    public Subject createHashtableInSubject(String username) throws AuthenticationException;

    /**
     * Returns whether an IdentiyStoreHander is available for validation.
     *
     * @return whether an identityStoreHander is available.
     */
    public boolean isIdentityStoreHanderAvailable();
}
