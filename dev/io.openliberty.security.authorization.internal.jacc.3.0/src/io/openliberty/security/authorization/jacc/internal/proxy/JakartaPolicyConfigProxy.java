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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;

public class JakartaPolicyConfigProxy implements PolicyConfiguration {

    static enum ContextState {
        OPEN("open"),
        IN_SERVICE("inService"),
        DELETED("deleted");

        private final String stateString;

        ContextState(String stateString) {
            this.stateString = stateString;
        }

        String getStateString() {
            return stateString;
        }
    };

    private final String contextId;

    private final JakartaPolicyConfigFactoryProxy factoryProxy;

    private volatile ContextState state = ContextState.OPEN;

    private final List<Permission> excludedPermissions = new CopyOnWriteArrayList<>();

    private final List<Permission> uncheckedPermissions = new CopyOnWriteArrayList<>();

    private final Map<String, PermissionCollection> rolePermMap = new ConcurrentHashMap<>();

    private final AtomicReference<PolicyConfigurationFactory> currentFactory = new AtomicReference<>();

    private final AtomicReference<PolicyConfiguration> currentConfig = new AtomicReference<>();

    JakartaPolicyConfigProxy(JakartaPolicyConfigFactoryProxy factoryProxy, String contextId, boolean remove) throws PolicyContextException {
        this.factoryProxy = factoryProxy;
        this.contextId = contextId;

        PolicyConfigurationFactory factory = factoryProxy.getFactory();
        if (factory != null) {
            // Create the delegated policy eagerly and set the currentFactory and currentConfig to point to the factory
            PolicyConfiguration config = factory.getPolicyConfiguration(contextId, remove);
            if (config != null) {
                currentConfig.set(config);
            }
            currentFactory.set(factory);
        }
    }

    void resetDelegatePolicyConfig(boolean remove) throws PolicyContextException {
        PolicyConfigurationFactory delegateFactory = factoryProxy.getFactory();
        synchronized (this) {
            PolicyConfiguration delegateConfig = delegateFactory == null ? null : delegateFactory.getPolicyConfiguration(contextId, remove);
            if (remove) {
                 delete(true);
            }
            setState(ContextState.OPEN);

            // Order matters here.  Need to set the config before the factory to avoid a timing window in
            // getDelegatePolicyConfig where we think the config is already set if the factory is updated
            currentConfig.set(delegateConfig);
            currentFactory.set(delegateFactory);
        }
    }

    private synchronized void setState(ContextState newState) {
        state = newState;
    }

    void ensureInitialized() {
        PolicyConfigurationFactory delegateFactory = factoryProxy.getFactory();
        if (delegateFactory != null) {
            try {
                getDelegatePolicyConfig(delegateFactory, true);
            } catch (PolicyContextException pce) {
                // log and FFDC the exception
            }
        }
    }

    private PolicyConfiguration getDelegatePolicyConfig(PolicyConfigurationFactory delegateFactory, boolean checkState) throws PolicyContextException {
        if (currentFactory.get() == delegateFactory) {
            return currentConfig.get();
        }

        synchronized (this) {
            if (currentFactory.get() == delegateFactory) {
                return currentConfig.get();
            }

            // If it is a new factory or config, we need to propagate current state to the PolicyConfiguration
            PolicyConfiguration delegateConfig = delegateFactory.getPolicyConfiguration(contextId, false);

            if (delegateConfig != null) {
                // Populate with the current state
                for (Permission excludedPerm : excludedPermissions) {
                    delegateConfig.addToExcludedPolicy(excludedPerm);
                }

                for (Permission uncheckedPerm : uncheckedPermissions) {
                    delegateConfig.addToUncheckedPolicy(uncheckedPerm);
                }
                for (Entry<String, PermissionCollection> entry : rolePermMap.entrySet()) {
                    String role = entry.getKey();
                    delegateConfig.addToRole(role, entry.getValue());
                }
                if (checkState) {
                    if (state == ContextState.DELETED) {
                        delegateConfig.delete();
                    } else if (state == ContextState.IN_SERVICE) {
                        delegateConfig.commit();
                    }
                }
            }
            // Order matters here.  Need to set the config before the factory to avoid a timing window in
            // this method where we think the config is already set if the factory is updated
            currentConfig.set(delegateConfig);
            currentFactory.set(delegateFactory);
            return delegateConfig;
        }
    }

    private PolicyConfiguration getDelegateConfig(String methodName, boolean checkOpen) throws PolicyContextException {
        if (checkOpen) {
            if (state != ContextState.OPEN) {
                throw new java.lang.UnsupportedOperationException(methodName + " called when the PolicyConfiguration is not in the open state. The current state is = "
                                                                  + (state == null ? "null" : state.getStateString()));
            }
        }
        PolicyConfigurationFactory delegateFactory = factoryProxy.getFactory();
        return delegateFactory == null ? null : getDelegatePolicyConfig(delegateFactory, false);
    }

    @Override
    public void addToExcludedPolicy(Permission permission) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToExcludedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.addToExcludedPolicy(permission);
        }

        excludedPermissions.add(permission);
    }

    @Override
    public void addToExcludedPolicy(PermissionCollection perms) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToExcludedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.addToExcludedPolicy(perms);
        }

        for (Enumeration<Permission> permEnum = perms.elements(); permEnum.hasMoreElements();) {
            excludedPermissions.add(permEnum.nextElement());
        }
    }

    @Override
    public void addToRole(String roleName, Permission permission) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToRole", true);
        if (delegateConfig != null) {
            delegateConfig.addToRole(roleName, permission);
        }

        PermissionCollection rolePerms = rolePermMap.get(roleName);
        if (rolePerms == null) {
            rolePerms = new Permissions();
            PermissionCollection oldValue = rolePermMap.putIfAbsent(roleName, rolePerms);
            if (oldValue != null) {
                rolePerms = oldValue;
            }
        }
        synchronized (rolePerms) {
            rolePerms.add(permission);
        }
    }

    @Override
    public void addToRole(String roleName, PermissionCollection perms) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToRole", true);
        if (delegateConfig != null) {
            delegateConfig.addToRole(roleName, perms);
        }

        PermissionCollection rolePerms = rolePermMap.get(roleName);
        if (rolePerms == null) {
            rolePerms = new Permissions();
            PermissionCollection oldValue = rolePermMap.putIfAbsent(roleName, rolePerms);
            if (oldValue != null) {
                rolePerms = oldValue;
            }
        }
        synchronized (rolePerms) {
            for (Enumeration<Permission> permEnum = perms.elements(); permEnum.hasMoreElements();) {
                rolePerms.add(permEnum.nextElement());
            }
        }
    }

    @Override
    public void addToUncheckedPolicy(Permission permission) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToUncheckedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.addToUncheckedPolicy(permission);
        }

        uncheckedPermissions.add(permission);
    }

    @Override
    public void addToUncheckedPolicy(PermissionCollection perms) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("addToUncheckedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.addToUncheckedPolicy(perms);
        }

        for (Enumeration<Permission> permEnum = perms.elements(); permEnum.hasMoreElements();) {
            uncheckedPermissions.add(permEnum.nextElement());
        }
    }

    @Override
    public void delete() throws PolicyContextException {
        delete(false);
    }

    private void delete(boolean internalOnly) throws PolicyContextException {
        if (!internalOnly) {
            PolicyConfiguration delegateConfig = getDelegateConfig("addToUncheckedPolicy", false);
            if (delegateConfig != null) {
                delegateConfig.delete();
            }
        }

        excludedPermissions.clear();
        uncheckedPermissions.clear();
        rolePermMap.clear();
        setState(ContextState.DELETED);
    }

    @Override
    public String getContextID() throws PolicyContextException {
        return contextId;
    }

    @Override
    public PermissionCollection getExcludedPermissions() {
        PolicyConfiguration delegateConfig;
        try {
            delegateConfig = getDelegateConfig("getExcludedPermissions", false);
        } catch (PolicyContextException pce) {
            delegateConfig = null;
        }

        if (delegateConfig != null) {
            return delegateConfig.getExcludedPermissions();
        }

        Permissions perms = new Permissions();
        for (Permission excludedPerm : excludedPermissions) {
            perms.add(excludedPerm);
        }
        return perms;
    }

    @Override
    public Map<String, PermissionCollection> getPerRolePermissions() {
        PolicyConfiguration delegateConfig;
        try {
            delegateConfig = getDelegateConfig("getPerRolePermissions", false);
        } catch (PolicyContextException pce) {
            delegateConfig = null;
        }

        if (delegateConfig != null) {
            return delegateConfig.getPerRolePermissions();
        }

        Map<String, PermissionCollection> roles = new HashMap<>();
        for (Entry<String, PermissionCollection> entry : rolePermMap.entrySet()) {
            PermissionCollection perms = new Permissions();
            for (Enumeration<Permission> permEnum = entry.getValue().elements(); permEnum.hasMoreElements();) {
                perms.add(permEnum.nextElement());
            }
            roles.put(entry.getKey(), perms);
        }
        return roles;
    }

    @Override
    public PermissionCollection getUncheckedPermissions() {
        PolicyConfiguration delegateConfig;
        try {
            delegateConfig = getDelegateConfig("getUncheckedPermissions", false);
        } catch (PolicyContextException pce) {
            delegateConfig = null;
        }

        if (delegateConfig != null) {
            return delegateConfig.getUncheckedPermissions();
        }

        Permissions perms = new Permissions();
        for (Permission uncheckedPerm : uncheckedPermissions) {
            perms.add(uncheckedPerm);
        }
        return perms;
    }

    @Override
    public void linkConfiguration(PolicyConfiguration policyConfig) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("linkConfiguration", true);
        if (delegateConfig != null) {
            delegateConfig.linkConfiguration(policyConfig);
        }
    }

    @Override
    public void removeExcludedPolicy() throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("removeExcludedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.removeExcludedPolicy();
        }

        excludedPermissions.clear();
    }

    @Override
    public void removeRole(String role) throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("removeRole", true);
        if (delegateConfig != null) {
            delegateConfig.removeRole(role);
        }

        rolePermMap.remove(role);
    }

    @Override
    public void removeUncheckedPolicy() throws PolicyContextException {
        PolicyConfiguration delegateConfig = getDelegateConfig("removeUncheckedPolicy", true);
        if (delegateConfig != null) {
            delegateConfig.removeUncheckedPolicy();
        }

        uncheckedPermissions.clear();
    }

    @Override
    public void commit() throws PolicyContextException {
        if (state == ContextState.DELETED) {
            throw new java.lang.UnsupportedOperationException("commit called when the PolicyConfiguration is in the deleted state");
        }

        PolicyConfiguration delegateConfig = getDelegateConfig("commit", false);
        if (delegateConfig != null) {
            delegateConfig.commit();
        }

        setState(ContextState.IN_SERVICE);
    }

    @Override
    public boolean inService() {
        return state == ContextState.IN_SERVICE;
    }
}
