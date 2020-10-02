/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.delegation;

import java.util.Collection;

import com.ibm.ws.javaee.dd.appbnd.SecurityRole;

/**
 * Interface for create and remove a security roles mapping for a given application
 */
public interface DefaultDelegationProvider extends DelegationProvider {

    /**
     * Creates the application to security roles mapping for a given application.
     *
     * @param appName the name of the application for which the mappings belong to.
     * @param securityRoles the security roles of the application.
     */
    void createAppToSecurityRolesMapping(String appName, Collection<SecurityRole> securityRoles);

    /**
     * Removes the role to RunAs mappings for a given application.
     *
     * @param appName the name of the application for which the mappings belong to.
     */
    void removeRoleToRunAsMapping(String appName);

}