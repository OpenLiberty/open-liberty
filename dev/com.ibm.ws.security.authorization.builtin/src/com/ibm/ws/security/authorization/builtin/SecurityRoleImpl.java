/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.builtin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authorization.SecurityRole;

/**
 * This class represents one role definition from the config. It contains
 * the role name, the set of users, the set of groups, and the set of
 * special subjects.
 */

public class SecurityRoleImpl implements SecurityRole {
    private static final TraceComponent tc = Tr.register(SecurityRoleImpl.class);
    static final String CFG_KEY_USER = "user";
    static final String CFG_KEY_GROUP = "group";
    static final String CFG_KEY_ACCESSID = "access-id";
    static final String CFG_KEY_SPECIAL_SUBJECT = "special-subject";
    private final Set<String> users = new HashSet<String>();
    private final Set<String> groups = new HashSet<String>();
    private final Set<String> specialSubjects = new HashSet<String>();
    private final Set<String> accessIds = new HashSet<String>();
    private String name = null;
    private final String bundleLocation;

    /**
     * Create a role from the configuration properties
     *
     * @param pids TODO
     * @param props
     */
    public SecurityRoleImpl(ConfigurationAdmin configAdmin,
                            String bundleLocation,
                            String roleName,
                            Dictionary<String, Object> roleProps, Set<String> pids) {
        name = roleName;
        this.bundleLocation = bundleLocation;

        processUsers(configAdmin, roleName, roleProps, pids);
        processGroups(configAdmin, roleName, roleProps, pids);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Role " + roleName + " has accessIds:", accessIds);
        }
        processSpecialSubjects(configAdmin, roleName, roleProps, pids);
    }

    /** {@inheritDoc} */
    @Override
    public String getRoleName() {
        return name;
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

    /** {@inheritDoc} */
    @Override
    public Set<String> getSpecialSubjects() {
        return specialSubjects;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAccessIds() {
        return accessIds;
    }

    /**
     * Read and process all the user elements
     *
     * @param configAdmin
     * @param roleName
     * @param roleProps
     * @param pids TODO
     */
    private void processUsers(ConfigurationAdmin configAdmin,
                              String roleName,
                              Dictionary<String, Object> roleProps, Set<String> pids) {

        String[] userPids = (String[]) roleProps.get(CFG_KEY_USER);
        if (userPids == null || userPids.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No users in role " + roleName);
            }
        } else {
            Set<String> badUsers = new HashSet<String>();
            Set<String> badAccessIds = new HashSet<String>();
            for (int i = 0; i < userPids.length; i++) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "user pid " + i + ": " + userPids[i]);
                }
                pids.add(userPids[i]);
                Configuration userConfig = null;
                try {
                    userConfig = configAdmin.getConfiguration(userPids[i], bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid user entry " + userPids[i]);
                    }
                    continue;
                }
                if (userConfig == null || userConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Null user element", userPids[i]);
                    }
                    continue;
                }

                Dictionary<String, Object> userProps = userConfig.getProperties();
                final String name = (String) userProps.get("name");
                final String accessId = (String) userProps.get("access-id");
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                if (accessId != null && AccessIdUtil.isUserAccessId(accessId)) {
                    if (badAccessIds.contains(accessId)) {
                        // This accessId is already flagged as a duplicate
                        continue;
                    }
                    if (!accessIds.add(accessId)) {
                        Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_MEMBER", getRoleName(), CFG_KEY_ACCESSID, accessId);
                        badAccessIds.add(accessId);
                        accessIds.remove(accessId);
                    }
                } else {
                    // TODO: check for bad access id
                    if (badUsers.contains(name)) {
                        // This user is already flagged as a duplicate
                        continue;
                    }
                    if (name.trim().isEmpty()) {
                        // Empty entry, ignoring
                        continue;
                    }
                    if (!users.add(name)) {
                        Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_MEMBER", getRoleName(), CFG_KEY_USER, name);
                        badUsers.add(name);
                        users.remove(name);
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Role " + roleName + " has users:", users);
            }
        }
    }

    /**
     * Read and process all the group elements
     *
     * @param configAdmin
     * @param roleName
     * @param roleProps
     * @param pids TODO
     */
    private void processGroups(ConfigurationAdmin configAdmin,
                               String roleName,
                               Dictionary<String, Object> roleProps, Set<String> pids) {

        String[] groupPids = (String[]) roleProps.get(CFG_KEY_GROUP);
        if (groupPids == null || groupPids.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No groups in role " + roleName);
            }
        } else {
            Set<String> badGroups = new HashSet<String>();
            Set<String> badAccessIds = new HashSet<String>();
            for (int i = 0; i < groupPids.length; i++) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "group pid " + i + ": " + groupPids[i]);
                }
                pids.add(groupPids[i]);
                Configuration groupConfig = null;
                try {
                    groupConfig = configAdmin.getConfiguration(groupPids[i], bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid group entry " + groupPids[i]);
                    }
                    continue;
                }
                if (groupConfig == null || groupConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Null group element", groupPids[i]);
                    }
                    continue;
                }

                Dictionary<String, Object> groupProps = groupConfig.getProperties();
                final String name = (String) groupProps.get("name");
                final String accessId = (String) groupProps.get("access-id");
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                // access id takes precedence
                if (accessId != null && AccessIdUtil.isGroupAccessId(accessId)) {
                    if (badAccessIds.contains(accessId)) {
                        // This accessId is already flagged as a duplicate
                        continue;
                    }
                    if (!accessIds.add(accessId)) {
                        Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_MEMBER", getRoleName(), CFG_KEY_ACCESSID, accessId);
                        badAccessIds.add(accessId);
                        accessIds.remove(accessId);
                    }
                } else {
                    // TODO: check for bad access id
                    if (badGroups.contains(name)) {
                        // This group is already flagged as a duplicate
                        continue;
                    }
                    if (name.trim().isEmpty()) {
                        // Empty entry, ignoring
                        continue;
                    }
                    if (!groups.add(name)) {
                        Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_MEMBER", getRoleName(), CFG_KEY_GROUP, name);
                        badGroups.add(name);
                        groups.remove(name);
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Role " + roleName + " has groups:", groups);
            }
        }
    }

    /**
     * Read and process all the specialSubject elements
     *
     * @param configAdmin
     * @param roleName
     * @param roleProps
     * @param pids TODO
     */
    private void processSpecialSubjects(ConfigurationAdmin configAdmin,
                                        String roleName,
                                        Dictionary<String, Object> roleProps, Set<String> pids) {

        String[] specialSubjectPids = (String[]) roleProps.get(CFG_KEY_SPECIAL_SUBJECT);
        if (specialSubjectPids == null || specialSubjectPids.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No special subjects in role " + roleName);
            }
        } else {
            Set<String> badSpecialSubjects = new HashSet<String>();
            for (int i = 0; i < specialSubjectPids.length; i++) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "special subject pid " + i + ": " + specialSubjectPids[i]);
                }
                pids.add(specialSubjectPids[i]);
                Configuration specialSubjectConfig = null;
                try {
                    specialSubjectConfig = configAdmin.getConfiguration(specialSubjectPids[i], bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid special subject entry " + specialSubjectPids[i]);
                    }
                    continue;
                }
                if (specialSubjectConfig == null || specialSubjectConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Null special subject element", specialSubjectPids[i]);
                    }
                    continue;
                }

                Dictionary<String, Object> specialSubjectProps = specialSubjectConfig.getProperties();
                final String type = (String) specialSubjectProps.get("type");
                if (type == null || type.trim().isEmpty()) {
                    continue;
                }
                if (badSpecialSubjects.contains(type)) {
                    // This special subject is already flagged as a duplicate
                    continue;
                }
                // TODO: check for invalid type
                if (type.trim().isEmpty()) {
                    // Empty entry, ignoring
                    continue;
                }
                if (!specialSubjects.add(type)) {
                    Tr.error(tc, "AUTHZ_TABLE_DUPLICATE_ROLE_MEMBER", getRoleName(), CFG_KEY_SPECIAL_SUBJECT, type);
                    badSpecialSubjects.add(type);
                    specialSubjects.remove(type);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Role " + roleName + " has special subjects:", specialSubjects);
            }
        }
    }
}
