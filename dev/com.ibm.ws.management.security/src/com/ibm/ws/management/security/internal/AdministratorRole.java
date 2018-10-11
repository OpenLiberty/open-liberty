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
import com.ibm.ws.security.AccessIdUtil;

/**
 * Administrator role binding: {@code
 * <administrator-role>
 *      <user>userName</user>
 *      <user-access-id>user:realm/utle</user-access-id>  or user-access-id>realm/utle</user-access-id>                 //realm is required
 *      <group>groupName</group>
 *      <group-access-id>group:realm/group1</group-access-id> or <group-access-id>realm/group1</group-access-id>        //realm is required
 * </administrator-role> }
 */
public class AdministratorRole implements ManagementRole {
    private static final TraceComponent tc = Tr.register(AdministratorRole.class);

    static final String CFG_KEY_USER = "user";
    static final String CFG_KEY_USER_ACCESSID = "user-access-id";
    static final String CFG_KEY_GROUP = "group";
    static final String CFG_KEY_GROUP_ACCESSID = "group-access-id";
    // The users set have both user and user-access-id
    private final Set<String> users = new HashSet<String>();
    // The groups set have both group and group-access-id
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

        // Process the user element
        processProps(props, CFG_KEY_USER, users);
        // Process the user-access-id element
        processProps(props, CFG_KEY_USER_ACCESSID, users);
        // Process the group element
        processProps(props, CFG_KEY_GROUP, groups);
        // Process the group-access-id element
        processProps(props, CFG_KEY_GROUP_ACCESSID, groups);
    }

    /**
     * @param props
     * @param element
     * @param entries
     */
    private void processProps(Map<String, Object> props, String element, Set<String> entries) {
        Set<String> badEntries = new HashSet<String>();
        String[] cfgEntries = (String[]) props.get(element);
        if (cfgEntries != null) {
            for (String entry : cfgEntries) {
                if (badEntries.contains(entry)) {
                    // This entry is already flagged as a duplicate or bad
                    continue;
                }
                if (entry.trim().isEmpty()) {
                    // Empty entry, ignoring
                    continue;
                }

                if (CFG_KEY_USER_ACCESSID.equals(element) || CFG_KEY_GROUP_ACCESSID.equals(element)) {
                    String updateAccessId = getCompleteAccessId(element, entry);
                    if (updateAccessId == null) {
                        if (!badEntries.contains(entry)) {
                            badEntries.add(entry);
                            continue;
                        }
                    } else
                        entry = updateAccessId;
                }

                if (!entries.add(entry)) {
                    Tr.error(tc, "ROLE_ENTRY_DUPLICATE", getRoleName(), element, entry);
                    badEntries.add(entry);
                    entries.remove(entry);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Administrator role " + element + " binding", entries);
        }
    }

    /**
     * @param element
     * @param entry
     * @return updateAccessId
     */
    private String getCompleteAccessId(String element, String accessId) {
        String updateAccessId = null;
        if (AccessIdUtil.isAccessId(accessId)) {
            return accessId;
        }
        String type = CFG_KEY_USER_ACCESSID.equals(element) ? AccessIdUtil.TYPE_USER : AccessIdUtil.TYPE_GROUP;

        updateAccessId = type + AccessIdUtil.TYPE_SEPARATOR + accessId;
        if (AccessIdUtil.isAccessId(updateAccessId)) {
            return updateAccessId;
        } else {
            //TODO: nls
            Tr.error(tc, "Invalid " + element, accessId);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRoleName() {
        return ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Set<String> getUsers() {
        return users;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Set<String> getGroups() {
        return groups;
    }
}
