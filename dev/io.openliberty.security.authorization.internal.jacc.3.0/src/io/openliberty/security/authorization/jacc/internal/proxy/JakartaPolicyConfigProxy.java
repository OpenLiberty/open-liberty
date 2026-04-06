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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyFactory;

/**
 * Stores the PolicyConfiguration state so that as new PolicyConfigurationFactory instances
 * are configured dynamically, new PolicyConfiguration instances can be populated and committed
 * based off of the state of each application that has started already.
 *
 * Only instances of this class access the actual PolicyConfiguration instances. All other Liberty runtime function interacts
 * with instances of this class when using Jakarta Authorization 3.0. The actual PolicyConfiguration instances are accessed
 * by the user's PolicyFactory instance to be able to get the state that this class propagates.
 */
class JakartaPolicyConfigProxy implements PolicyConfiguration {

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

    private final Set<JakartaPolicyConfigProxy> linkedConfigs = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Stores the current PolicyConfigurationFactory so that we can know if a new one was added dynamically
     * and can react appropriately.
     */
    private final AtomicReference<PolicyConfigurationFactory> currentFactory = new AtomicReference<>();

    /**
     * Stores the current PolicyConfiguration to be returned if there isn't a change to the PolicyConfigurationFactory.
     */
    private final AtomicReference<PolicyConfiguration> currentConfig = new AtomicReference<>();

    /**
     * Stores the PolicyConfigurationFactory for the PolicyConfiguration that is currently being processed to avoid
     * infinite loops when getting the PolicyConfiguration while configuring it. This can happen when dealing with
     * linked PolicyConfigurations.
     */
    private final AtomicReference<PolicyConfigurationFactory> inProgressFactory = new AtomicReference<>();

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

    /**
     * When calling getConfiguration on the JakartaPolicyConfigFactoryProxy, this method is used
     * to reset the state appropriately in this instance and calls the getPolicyConfiguration() method
     * on the actual PolicyConfigurationFactory to reset the state there.
     *
     * @param remove whether to remove all configuration
     * @throws PolicyContextException
     */
    void resetDelegatePolicyConfig(boolean remove) throws PolicyContextException {
        PolicyConfigurationFactory delegateFactory = factoryProxy.getFactory();
        synchronized (this) {
            PolicyConfiguration delegateConfig = delegateFactory == null ? null : delegateFactory.getPolicyConfiguration(contextId, remove);
            if (remove) {
                // Only deleting our internal state and not calling delete on the delegateConfig since the getPolicyConfiguration() call above handles that
                delete(true);
            }
            setState(ContextState.OPEN);

            // Order matters here.  Need to set the config before the factory to avoid a timing window in
            // getDelegatePolicyConfig where we think the config is already set if the factory is updated
            if (remove) {
                currentConfig.set(delegateConfig);
            } else {
                propagateAndSetConfig(delegateFactory, delegateConfig, false);
            }
            currentFactory.set(delegateFactory);
        }
    }

    private synchronized void setState(ContextState newState) {
        state = newState;
    }

    /**
     * Creates the actual PolicyConfiguration objects if it doesn't exists or there is a change
     * in the PolicyConfigurationFactory instance because it was dynamically added.
     */
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

    /**
     * Returns the current PolicyConfiguration or creates a new one and populates its state.
     *
     * Conditionally this method also checks that the PolicyConfiguration instance state
     * matches the current state in this class. Additionally this method updates the
     * current PolicyConfigurationFactory and PolicyConfiguration instances to point to
     * the newly created instances if there is a factory change.
     *
     * @param delegateFactory the PolicyConfigurationFactory to check
     * @param checkState      whether to check the PolicyConfiguration state to match this class
     * @return the PolicyConfiguration
     * @throws PolicyContextException
     */
    private PolicyConfiguration getDelegatePolicyConfig(PolicyConfigurationFactory delegateFactory, boolean checkState) throws PolicyContextException {
        if (currentFactory.get() == delegateFactory) {
            return currentConfig.get();
        }

        synchronized (this) {
            // Standard double check to validate that two threads don't both get in here and both make changes.  If one gets in and updates
            // the other thread should get what the first thread already did.  This can be done because AtomicRefernece makes use of volatile.
            if (currentFactory.get() == delegateFactory) {
                return currentConfig.get();
            }

            // If while configuring the PolicyConfiguration we ended up recursively calling back in, we want to return the current in progress
            // PolicyConfiguration that is set in propagateAndSetConfig.  This avoids getting into an infinite loop.  Other threads are still
            // blocked because they are looking at the state in the currentFactory field.
            if (inProgressFactory.get() == delegateFactory) {
                return currentConfig.get();
            }

            // If it is a new factory or config, we need to propagate current state to the PolicyConfiguration
            PolicyConfiguration delegateConfig = delegateFactory == null ? null : delegateFactory.getPolicyConfiguration(contextId, true);

            // Order matters here.  Need to set the config before the factory to avoid a timing window in
            // this method where we think the config is already set if the factory is updated
            propagateAndSetConfig(delegateFactory, delegateConfig, checkState);
            inProgressFactory.set(null);
            currentFactory.set(delegateFactory);
            return delegateConfig;
        }
    }

    /**
     * Callers to this method must hold the synchronization lock. This is in a separate method to allow multiple
     * methods to call it.
     *
     * @param delegateConfig
     * @param checkState
     * @throws PolicyContextException
     */
    private void propagateAndSetConfig(PolicyConfigurationFactory delegateFactory, PolicyConfiguration delegateConfig, boolean checkState) throws PolicyContextException {
        // If a config is not null and it doesn't match the existing delegateConfig, then we need
        // to populate it.
        if (delegateConfig != currentConfig.get()) {
            // Order matters here.  Need to set the config before the factory to avoid a timing window in
            // this method where we think the config is already set if the factory is updated
            // Setting the configuration here to avoid an infinite loop with linkConfiguration initializing the link
            // which could have a link to ourselves and will continue trying to initialize.  Since the current factory
            // reference is not set yet, other callers will not get a partially configured config object
            currentConfig.set(delegateConfig);
            if (delegateConfig != null) {
                inProgressFactory.set(delegateFactory);
                // Populate with the current state
                for (Permission excludedPerm : excludedPermissions) {
                    delegateConfig.addToExcludedPolicy(excludedPerm);
                }

                for (Permission uncheckedPerm : uncheckedPermissions) {
                    delegateConfig.addToUncheckedPolicy(uncheckedPerm);
                }
                for (Entry<String, PermissionCollection> entry : rolePermMap.entrySet()) {
                    delegateConfig.addToRole(entry.getKey(), entry.getValue());
                }
                if (linkedConfigs.size() > 0) {
                    for (JakartaPolicyConfigProxy linkConfig : linkedConfigs) {
                        // The PolicyConfiguration being linked may be in any state so need to pass true to set the state
                        PolicyConfiguration delegateLinkConfig = linkConfig.getDelegatePolicyConfig(delegateFactory, true);
                        if (delegateLinkConfig != null) {
                            delegateConfig.linkConfiguration(delegateLinkConfig);
                        }
                    }
                }
                if (checkState) {
                    if (state == ContextState.DELETED) {
                        delegateConfig.delete();
                    } else if (state == ContextState.IN_SERVICE) {
                        delegateConfig.commit();
                    }
                }
            }
            if (state != ContextState.OPEN) {
                refreshPolicy();
            }
        }
    }

    private void refreshPolicy() {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();

        // If we replace the underlying PolicyConfiguration, we need to call refresh on the Policy
        if (policyFactory != null) {
            Policy policy = policyFactory.getPolicy(contextId);
            if (policy != null) {
                policy.refresh();
            }
        }
    }

    private PolicyConfiguration getDelegateConfig(String methodName, boolean checkOpen) throws PolicyContextException {
        if (checkOpen) {
            if (state != ContextState.OPEN) {
                throw new UnsupportedOperationException(methodName + " called when the PolicyConfiguration is not in the open state. The current state is = "
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
            PolicyConfiguration delegateConfig = getDelegateConfig("delete", false);
            if (delegateConfig != null) {
                delegateConfig.delete();
                refreshPolicy();
            }
        }

        excludedPermissions.clear();
        uncheckedPermissions.clear();
        rolePermMap.clear();
        linkedConfigs.clear();
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
        if (!(policyConfig instanceof JakartaPolicyConfigProxy)) {
            throw new IllegalStateException("linkConfiguration is passed a type that was not expected " + policyConfig.getClass().getName());
        }

        JakartaPolicyConfigProxy linkConfig = (JakartaPolicyConfigProxy) policyConfig;
        String linkContextId = linkConfig.getContextID();
        if (contextId.equals(linkContextId)) {
            throw new IllegalArgumentException("context IDs match for this PolicyConfiguration and the linked one");
        }

        PolicyConfiguration delegateConfig = getDelegateConfig("linkConfiguration", true);
        if (delegateConfig != null) {
            // The PolicyConfiguration being linked may be in any state. Do not do an open check when getting it.
            PolicyConfiguration delegateLinkConfig = linkConfig.getDelegateConfig("linkConfiguration", false);
            if (delegateLinkConfig != null) {
                delegateConfig.linkConfiguration(delegateLinkConfig);
            }
        }
        linkedConfigs.add(linkConfig);
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
            throw new UnsupportedOperationException("commit called when the PolicyConfiguration is in the deleted state");
        }

        PolicyConfiguration delegateConfig = getDelegateConfig("commit", false);
        if (delegateConfig != null) {
            delegateConfig.commit();
            refreshPolicy();
        }

        setState(ContextState.IN_SERVICE);
    }

    @Override
    public boolean inService() {
        return state == ContextState.IN_SERVICE;
    }

    boolean isState(ContextState compareState) {
        return state == compareState;
    }

    Set<? extends PolicyConfiguration> getLinkedConfigurations() {
        return linkedConfigs;
    }
}
