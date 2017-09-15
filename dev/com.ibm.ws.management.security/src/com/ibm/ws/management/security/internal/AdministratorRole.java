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
package com.ibm.ws.management.security.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 * Administrator role binding: {@code
 * <administrator-role>
 * <user>userName</user>
 * <group>groupName</group>
 * </administrator-role> }
 */
public class AdministratorRole implements ManagementRole {
    private static final TraceComponent tc = Tr.register(AdministratorRole.class);

    static final String CFG_KEY_USER = "user";
    static final String CFG_KEY_GROUP = "group";
    private final Set<String> users = new HashSet<String>();
    private final Set<String> groups = new HashSet<String>();

    protected synchronized void activate(Map<String, Object> props) {
        resetBindings();

        updateBindings(props);
    }

    protected synchronized void modify(Map<String, Object> props) {
        resetBindings();

        updateBindings(props);
    }

    protected synchronized void deactivate() {
        resetBindings();
    }

    /**
     * Resets the bindings such that the sets are empty.
     */
    private void resetBindings() {
        users.clear();
        groups.clear();
    }

    /**
     * Update the binding sets based on the properties from the configuration.
     * 
     * @param props
     */
    private void updateBindings(Map<String, Object> props) {
        Set<String> badUsers = new HashSet<String>();
        String[] cfgUsers = (String[]) props.get(CFG_KEY_USER);
        if (cfgUsers != null) {
            for (String user : cfgUsers) {
                if (badUsers.contains(user)) {
                    // This user is already flagged as a duplicate
                    continue;
                }
                if (user.trim().isEmpty()) {
                    // Empty entry, ignoring
                    continue;
                }
                if (!users.add(user)) {
                    Tr.error(tc, "ROLE_ENTRY_DUPLICATE", getRoleName(), CFG_KEY_USER, user);
                    badUsers.add(user);
                    users.remove(user);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Administrator role user binding", users);
        }

        Set<String> badGroups = new HashSet<String>();
        String[] cfgGroups = (String[]) props.get(CFG_KEY_GROUP);
        if (cfgGroups != null) {
            for (String group : cfgGroups) {
                if (badGroups.contains(group)) {
                    // This user is already flagged as a duplicate
                    continue;
                }
                if (group.trim().isEmpty()) {
                    // Empty entry, ignoring
                    continue;
                }
                if (!groups.add(group)) {
                    Tr.error(tc, "ROLE_ENTRY_DUPLICATE", getRoleName(), CFG_KEY_GROUP, group);
                    badGroups.add(group);
                    groups.remove(group);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Administrator role group binding", groups);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRoleName() {
        return ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME;
    }

    /** {@inheritDoc} */
    public synchronized Set<String> getUsers() {
        return users;
    }

    /** {@inheritDoc} */
    public synchronized Set<String> getGroups() {
        return groups;
    }

}
