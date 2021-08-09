/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jca;

import java.util.Map;

import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * The AuthDataService is the interface to obtain the subject for an auth data alias.
 */
public interface AuthDataService {

    /**
     * Gets the subject representing the auth data specified by the auth data alias.
     * A javax.resource.spi.security.PasswordCredential is used to hold the auth data contents and it
     * is placed in the subject's private credentials list.
     * 
     * @param managedConnectionFactory the Managed Connection Factory.
     * @param jaasEntryName the name of the JAAS entry in the JAAS configuration.
     *            When it is null, <code>getMappedSubject</code> uses the <code>DefaultMappingModule</code> login configuration.
     * @param loginData a Map that contains parameters that are used by the configured mapping <code>LoginModules</code>.
     *            In the case of the <code>DefaultPrincipalMapping</code>, a property <code>"com.ibm.mapping.authDataAlias"</code>
     *            should be defined to specify the authData entry to use.
     * 
     * @return the subject with the PasswordCredential containing the auth data.
     * 
     * @throws LoginException when an non-existent auth data alias is specified or
     *             if the auth data does not contain the required <code>user</code> and <code>password</code> attributes.
     */
    public Subject getSubject(ManagedConnectionFactory managedConnectionFactory, String jaasEntryName, Map<String, Object> loginData) throws LoginException;

}
