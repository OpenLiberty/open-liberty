/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.provider;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PrincipalMapper;

public class PolicyImpl implements Policy {
    private static final TraceComponent tc = Tr.register(PolicyImpl.class);
    private final String contextID;

    public PolicyImpl(String contextId) {
        this.contextID = contextId;
    }

    @Override
    public boolean implies(Permission p, Subject subject) {
        // If there is no policy configuration, the application doesn't
        // have any security constraints.  In that case return true.
        PolicyConfiguration pc = PolicyConfigurationFactory.get().getPolicyConfiguration(contextID);
        if (pc == null) {
            return true;
        }
        return Policy.super.implies(p, subject);
    }

    @Override
    public boolean impliesByRole(Permission p, Subject subject) {
        Map<String, PermissionCollection> perRolePermissions = PolicyConfigurationFactory.get().getPolicyConfiguration(contextID).getPerRolePermissions();
        if (p instanceof EJBMethodPermission) {
            List<String> requiredRoleList = getRequiredRoleList(perRolePermissions, p);
            if (requiredRoleList == null || requiredRoleList.size() == 0) {
                return true;
            }
        }
        PrincipalMapper principalMapper = PolicyContext.get(PolicyContext.PRINCIPAL_MAPPER);
        if (!principalMapper.isAnyAuthenticatedUserRoleMapped()) {
            PermissionCollection rolePermissions = perRolePermissions.get("**");
            if (rolePermissions != null && rolePermissions.implies(p)) {
                return true;
            }
        }
        Set<String> mappedRoles = principalMapper.getMappedRoles(subject);
        for (String mappedRole : mappedRoles) {
            PermissionCollection rolePermissions = perRolePermissions.get(mappedRole);
            if (rolePermissions != null && rolePermissions.implies(p)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getRequiredRoleList(Map<String, PermissionCollection> perRolePermissions, Permission p) {
        List<String> requiredRoleList = null;
        if (!perRolePermissions.isEmpty()) {
            requiredRoleList = new ArrayList<String>();
            for (Entry<String, PermissionCollection> e : perRolePermissions.entrySet()) {
                PermissionCollection perm = e.getValue();
                if (perm.implies(p)) {
                    String role = e.getKey();
                    requiredRoleList.add(role);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Added role: " + role + " to the requiredRoleList for Permission: " + p + " granted by : " + perm);
                }
            }
        }
        return requiredRoleList;
    }

    @Override
    public boolean isExcluded(Permission p) {
        return PolicyConfigurationFactory.get().getPolicyConfiguration(contextID).getExcludedPermissions().implies(p);
    }

    @Override
    public boolean isUnchecked(Permission p) {
        return PolicyConfigurationFactory.get().getPolicyConfiguration(contextID).getUncheckedPermissions().implies(p);
    }

    @Override
    public void refresh() {
    }

    @Override
    public PermissionCollection getPermissionCollection(Subject subject) {
        throw new UnsupportedOperationException();
    }
}
