/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 * Returns the various Security services that are effective for
 * the current configuration.
 */
public interface SecurityService {

    /**
     * Retrieves the AuthenticationService that is registered for the
     * current configuration.
     * 
     * @return AuthenticationService, does not return <code>null</code>.
     */
    AuthenticationService getAuthenticationService();

    /**
     * Retrieves the AuthorizationService that is registered for the
     * current configuration.
     * 
     * @return AuthorizationService, does not return <code>null</code>.
     */
    AuthorizationService getAuthorizationService();

    /**
     * Retrieves the UserRegistryService that is registered for the
     * current configuration.
     * 
     * @return UserRegistryService, does not return <code>null</code>.
     */
    UserRegistryService getUserRegistryService();

}
