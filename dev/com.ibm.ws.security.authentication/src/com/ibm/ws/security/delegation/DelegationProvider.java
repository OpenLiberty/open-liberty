/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.delegation;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.AuthenticationException;

/**
 * Interface for creating the run-as subject during delegation.
 */
public interface DelegationProvider {

    /**
     * Create a subject for the user mapped to the given role of a given application.
     *
     * @param roleName the name of the role, used to look up the corresponding user.
     * @param appName the name of the application, used to look up the corresponding user.
     * @return subject a subject representing the user that is mapped to the given run-as role.
     * @throws AuthenticationException if the identity cannot be authenticated
     */
    Subject getRunAsSubject(String roleName, String appName) throws AuthenticationException;

    /**
     * @return
     */
    String getDelegationUser();

}