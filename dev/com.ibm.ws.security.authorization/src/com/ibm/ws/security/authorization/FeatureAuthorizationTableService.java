/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.security.authorization;

/**
 * This class extends AuthorizationTableService for feature authz tables
 * 
 * @see AuthorizationTableService
 */
public interface FeatureAuthorizationTableService extends AuthorizationTableService {

    /**
     * Add an authorization table
     * 
     * @param resource the name of the authz table, i.e. the IBM-Feature-Authorization header value
     * @param authzTable the authorization table
     */
    void addAuthorizationTable(String resourceName, AuthorizationTableService authzTable);

    /**
     * Remove an authorization table
     * 
     * @param resource the name of the authz table, i.e. the IBM-Feature-Authorization header value
     */
    void removeAuthorizationTable(String resourceName);

    /**
     * Return the value of the IBM-Authorization-Roles header from the web module
     * of the web request currently on the thread context
     * 
     * @return IBM-Authorization-Roles value or null if there is no IBM-Authorization-Roles header
     */
    public String getFeatureAuthzRoleHeaderValue();
}
