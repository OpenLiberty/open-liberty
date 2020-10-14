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

package com.ibm.ws.security.authorization.saf;

/**
 * Maps J2EE role names to SAF profile names. SAFRoleMapper is called by
 * the SAF authorization service to map a given J2EE role name to a SAF
 * profile name. The authorization check for the J2EE role is made against
 * the underlying SAF security product using the mapped profile name in the
 * EJBROLE class.
 */
public interface SAFRoleMapper {

    /**
     * Map a role name to a valid SAF profile. This method is called
     * by the security component to build a profile name in the
     * <code>EJBROLE</code> SAF class that will be used for
     * authorization and delegation decisions.
     *
     * @param resourceName the resource that requires authorization
     * @param role         the application defined role name from the
     *                         application deployment descriptor
     * @return the mapped profile of the role
     */
    public String getProfileFromRole(final String resourceName, final String role);
}
