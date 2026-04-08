/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import jakarta.security.jacc.PolicyConfiguration;

class PolicyConfigImpl implements PolicyConfiguration {

    final String contextID;

    enum State {
        OPEN, DELETED, IN_SERVICE;
    }

    List<Permission> excludeList = new ArrayList<Permission>();
    List<Permission> uncheckedList = new ArrayList<Permission>();
    Map<String, List<Permission>> rolesMap = new HashMap<>();
    Set<PolicyConfiguration> linkedConfigs = new HashSet<>();
    State state = State.OPEN;

    PolicyConfigImpl(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "ctor");
        this.contextID = contextID;
    }

    @Override
    public void addToExcludedPolicy(Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(Permission)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        excludeList.add(perm);
    }

    @Override
    public void addToRole(String roleName, Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToRole(String, Permission)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        List<Permission> perms = rolesMap.get(roleName);
        if (perms == null) {
            perms = new ArrayList<>();
            rolesMap.put(roleName, perms);
        }
        perms.add(perm);
    }

    @Override
    public void addToUncheckedPolicy(Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(Permission)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        uncheckedList.add(perm);
    }

    @Override
    public void delete() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "delete");
        excludeList.clear();
        uncheckedList.clear();
        rolesMap.clear();
        linkedConfigs.clear();
        state = State.DELETED;
    }

    @Override
    public String getContextID() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "getContextID");
        return contextID;
    }

    @Override
    public PermissionCollection getExcludedPermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "getExcludedPermissions");
        Permissions excludedPerms = new Permissions();
        for (Permission perm : excludeList) {
            excludedPerms.add(perm);
        }
        return excludedPerms;
    }

    @Override
    public Map<String, PermissionCollection> getPerRolePermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "getPerRolePermissions");
        Map<String, PermissionCollection> roles = new HashMap<>();
        for (Entry<String, List<Permission>> entry : rolesMap.entrySet()) {
            List<Permission> perms = entry.getValue();
            Permissions permCollection = new Permissions();
            for (Permission perm : perms) {
                permCollection.add(perm);
            }
            roles.put(entry.getKey(), permCollection);
        }
        return roles;
    }

    @Override
    public PermissionCollection getUncheckedPermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "getUncheckedPermissions");
        Permissions uncheckedPerms = new Permissions();
        for (Permission perm : uncheckedList) {
            uncheckedPerms.add(perm);
        }
        return uncheckedPerms;
    }

    @Override
    public void linkConfiguration(PolicyConfiguration config) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "linkConfiguration");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        if (!(config instanceof PolicyConfigImpl)) {
            throw new IllegalArgumentException("wrong type " + config.getClass().getName());
        }
        PolicyConfigImpl linkedConfig = (PolicyConfigImpl) config;
        if (linkedConfig.getContextID().equals(contextID)) {
            throw new IllegalArgumentException("context ids match between this config and the linked config");
        }
        linkedConfigs.add(linkedConfig);
    }

    @Override
    public void removeExcludedPolicy() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "removeExcludedPolicy");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        excludeList.clear();
    }

    @Override
    public void removeRole(String roleName) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "removeRole");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        rolesMap.remove(roleName);
    }

    @Override
    public void removeUncheckedPolicy() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "removeUncheckedPolicy");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        uncheckedList.clear();
    }

    @Override
    public void addToExcludedPolicy(PermissionCollection perms) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(PermissionCollection)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        for (Enumeration<Permission> permEnum = perms.elements(); permEnum.hasMoreElements();) {
            excludeList.add(permEnum.nextElement());
        }
    }

    @Override
    public void addToRole(String roleName, PermissionCollection permCollection) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToRole(String, PermissionCollection)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        List<Permission> perms = rolesMap.get(roleName);
        if (perms == null) {
            perms = new ArrayList<>();
            rolesMap.put(roleName, perms);
        }
        for (Enumeration<Permission> permEnum = permCollection.elements(); permEnum.hasMoreElements();) {
            perms.add(permEnum.nextElement());
        }
    }

    @Override
    public void addToUncheckedPolicy(PermissionCollection perms) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(PermissionCollection)");
        if (state != State.OPEN) {
            throw new UnsupportedOperationException();
        }
        for (Enumeration<Permission> permEnum = perms.elements(); permEnum.hasMoreElements();) {
            uncheckedList.add(permEnum.nextElement());
        }
    }

    @Override
    public void commit() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "commit");
        if (state == State.DELETED) {
            throw new UnsupportedOperationException();
        }
        state = State.IN_SERVICE;
    }

    @Override
    public boolean inService() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "inService");
        return state == State.IN_SERVICE;
    }

    void reOpen() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY_CONFIG, "reOpen");
        state = State.OPEN;
    }

    Set<PolicyConfiguration> getLinkedConfigurations() {
        return linkedConfigs;
    }
}