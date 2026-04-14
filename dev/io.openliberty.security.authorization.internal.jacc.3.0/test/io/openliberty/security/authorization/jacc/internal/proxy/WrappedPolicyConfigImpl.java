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
import java.util.Map;
import java.util.Set;

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import jakarta.security.jacc.PolicyConfiguration;

class WrappedPolicyConfigImpl implements PolicyConfiguration {

    private final String contextID;
    private final PolicyConfigImpl wrappee;

    WrappedPolicyConfigImpl(String contextID, PolicyConfigImpl config) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "ctor");
        this.contextID = contextID;
        wrappee = config;
    }

    @Override
    public void addToExcludedPolicy(Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToExcludedPolicy(Permission)");
        wrappee.addToExcludedPolicy(perm);
    }

    @Override
    public void addToRole(String roleName, Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToRole(String, Permission)");
        wrappee.addToRole(roleName, perm);
    }

    @Override
    public void addToUncheckedPolicy(Permission perm) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToUncheckedPolicy(Permission)");
        wrappee.addToUncheckedPolicy(perm);
    }

    @Override
    public void delete() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "delete");
        wrappee.delete();
    }

    @Override
    public String getContextID() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "getContextID");
        return wrappee.getContextID();
    }

    @Override
    public PermissionCollection getExcludedPermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "getExcludedPermissions");
        return wrappee.getExcludedPermissions();
    }

    @Override
    public Map<String, PermissionCollection> getPerRolePermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "getPerRolePermissions");
        return wrappee.getPerRolePermissions();
    }

    @Override
    public PermissionCollection getUncheckedPermissions() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "getUncheckedPermissions");
        return wrappee.getUncheckedPermissions();
    }

    @Override
    public void linkConfiguration(PolicyConfiguration config) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "linkConfiguration");
        if (config instanceof WrappedPolicyConfigImpl) {
            config = ((WrappedPolicyConfigImpl) config).wrappee;
        } else {
            throw new IllegalStateException("Got wrong type of linked configuration object");
        }
        wrappee.linkConfiguration(config);
    }

    @Override
    public void removeExcludedPolicy() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "removeExcludedPolicy");
        wrappee.removeExcludedPolicy();
    }

    @Override
    public void removeRole(String roleName) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "removeRole");
        wrappee.removeRole(roleName);
    }

    @Override
    public void removeUncheckedPolicy() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "removeUncheckedPolicy");
        wrappee.removeUncheckedPolicy();
    }

    @Override
    public void addToExcludedPolicy(PermissionCollection perms) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToExcludedPolicy(PermissionCollection)");
        wrappee.addToExcludedPolicy(perms);
    }

    @Override
    public void addToRole(String roleName, PermissionCollection permCollection) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToRole(String, PermissionCollection)");
        wrappee.addToRole(roleName, permCollection);
    }

    @Override
    public void addToUncheckedPolicy(PermissionCollection perms) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "addToUncheckedPolicy(PermissionCollection)");
        wrappee.addToUncheckedPolicy(perms);
    }

    @Override
    public void commit() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "commit");
        wrappee.commit();
    }

    @Override
    public boolean inService() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "inService");
        return wrappee.inService();
    }

    void reOpen() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.WRAPPING_POLICY_CONFIG, "reOpen");
        wrappee.reOpen();
    }

    Set<PolicyConfiguration> getLinkedConfigurations() {
        return wrappee.getLinkedConfigurations();
    }
}