/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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

package com.ibm.ws.messaging.security.beans;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.security.MSTraceConstants;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Permission object to hold the data about a Destination. It has two maps,
 * one for Users and other for Groups. The Map will have action as key and
 * List of Users/Groups as there value.
 * 
 * @author Sharath Chandra B
 * 
 */
public abstract class Permission {

    // Trace component for the Permission class
    private static TraceComponent tc = SibTr.register(Permission.class,
            MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
            MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.beans.Permission";

    /*
     * Key in the below two Maps can be anyone of the following roles
     * SEND
     * RECEIVE
     * CREATE
     * BROWSE
     */
    // A Map which maps role to a set of Users
    private final Map<String, Set<String>> roleToUserMap = Collections.synchronizedMap(new HashMap<String, Set<String>>());

    // A Map which maps role to a set of Groups
    private final Map<String, Set<String>> roleToGroupMap = Collections.synchronizedMap(new HashMap<String, Set<String>>());

    /**
     * Get the map which gives the list of roles and the users associated with it
     * 
     * @return roleToUserMap
     */
    public Map<String, Set<String>> getRoleToUserMap() {
        return roleToUserMap;
    }

    /**
     * Get the map which gives the list of roles and the groups associated with it
     * 
     * @return roleToGroup
     */
    public Map<String, Set<String>> getRoleToGroupMap() {
        return roleToGroupMap;
    }

    /**
     * Add all the users to a particular role
     * 
     * @param users
     * @param role
     */
    protected void addAllUsersToRole(Set<String> users, String role) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "addAllUsersToRole", new Object[] { users, role });
        }

        if (users != null && role != null) {
            Set<String> roleToUserLocalRef = roleToUserMap.computeIfAbsent(role, Permission::createSynchronizedSet);
            users.stream().filter(Objects::nonNull).forEach(roleToUserLocalRef::add);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "addAllUsersToRole");
        }
    }

    /**
     * Add all the groups to a particular role
     * 
     * @param groups
     * @param role
     */
    protected void addAllGroupsToRole(Set<String> groups, String role) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "addAllGroupsToRole", new Object[] { groups, role });
        }

        if (groups != null && role != null) {
            Set<String> roleToGroupLocalRef = roleToGroupMap.computeIfAbsent(role, Permission::createSynchronizedSet);
            groups.stream().filter(Objects::nonNull).forEach(roleToGroupLocalRef::add);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "addAllGroupsToRole");
        }
    }

    protected boolean checkActionArrayHasAllPermission(String[] actionArray) {
        for (String action : actionArray) {
            if (action.equalsIgnoreCase(MessagingSecurityConstants.OPERATION_TYPE_ALL))
                return true;
        }
        return false;
    }

    /**
     * 
     * @param actions
     * @param users
     * @param groups
     */
    public abstract void addUserAndGroupsToRole(String[] actions, Set<String> users, Set<String> groups);

    /**
     * Validate the action which is supplied to a specific permission
     * 
     * @param action
     * @return
     */
    public abstract boolean validateAction(String action);

    /**
     * When action ALL is defined for a permission, we have to add all the allowed actions to the set of Users and Groups which are specified
     * 
     * @param users
     * @param groups
     */
    public abstract void addUsersAndGroupsToAllActions(Set<String> users, Set<String> groups);

    //Object ignored is to match the signature of computeIfAbsent
    private static <V extends Comparable<V>> SortedSet<V> createSynchronizedSet(Object ignored) {
        return Collections.synchronizedSortedSet(new TreeSet<V>());
    }

}
