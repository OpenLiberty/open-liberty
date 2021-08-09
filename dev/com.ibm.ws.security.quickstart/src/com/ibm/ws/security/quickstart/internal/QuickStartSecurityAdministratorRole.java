/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 * Implements the ManagementRole interface to provide the {@link ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME} role.
 * <p>
 * If any other ManagementRole services is defined, this definition must go away.
 */
class QuickStartSecurityAdministratorRole implements ManagementRole {
    private static final Set<String> EMPTY_SET = new HashSet<String>();
    private final Set<String> users;

    QuickStartSecurityAdministratorRole(String user) {
        users = new HashSet<String>();
        users.add(user);
    }

    /** {@inheritDoc} */
    @Override
    public String getRoleName() {
        return ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getUsers() {
        return users;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getGroups() {
        return EMPTY_SET;
    }

}
