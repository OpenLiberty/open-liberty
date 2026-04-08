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

import javax.security.auth.Subject;

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;

public class PolicyImpl implements Policy {

    final String contextID;

    PolicyConfiguration policyConfig;

    PolicyImpl(String contextID) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "ctor");
        this.contextID = contextID;
        setPolicyConfiguration();
    }

    @Override
    public PermissionCollection getPermissionCollection(Subject subject) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "getPermissionsCollection");
        return null;
    }

    @Override
    public boolean implies(Permission permissionToBeChecked, Subject subject) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "imples");
        return Policy.super.implies(permissionToBeChecked, subject);
    }

    @Override
    public boolean impliesByRole(Permission permissionToBeChecked, Subject subject) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "implesByRole");
        return true;
    }

    @Override
    public boolean isExcluded(Permission permissionToBeChecked) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "isExcluded");
        return false;
    }

    @Override
    public boolean isUnchecked(Permission permissionToBeChecked) {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "isUnchecked");
        return false;
    }

    private void setPolicyConfiguration() {
        PolicyConfigurationFactory pcf = null;
        try {
            pcf = PolicyConfigurationFactory.get();
        } catch (IllegalStateException ise) {
            // expected if nothing is defined
        }
        policyConfig = pcf == null ? null : pcf.getPolicyConfiguration(contextID);
    }

    @Override
    public void refresh() {
        AuthzModuleTracker.addOperation(contextID, ModuleType.POLICY, "refresh");
        setPolicyConfiguration();
    }
}
