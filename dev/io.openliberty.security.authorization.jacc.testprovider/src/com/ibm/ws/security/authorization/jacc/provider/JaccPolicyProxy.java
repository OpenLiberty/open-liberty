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
import java.security.Principal;
import java.util.Enumeration;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.EJBRoleRefPermission;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebRoleRefPermission;
import jakarta.security.jacc.WebUserDataPermission;

public class JaccPolicyProxy implements Policy, AuthzPolicy {
    private static final TraceComponent tc = Tr.register(JaccPolicyProxy.class);

    public JaccPolicyProxy() {
    }

    @Override
    public boolean implies(Permission p, Subject subject) {
        Boolean defaultChecks = implies(subject.getPrincipals(), p);
        return defaultChecks == null ? false : defaultChecks;
    }

    @Override
    public boolean impliesByRole(Permission p, Subject subject) {
        if (p instanceof WebResourcePermission) {
            Set<Principal> principals = subject == null ? null : subject.getPrincipals();
            if (principals != null && principals.size() > 0) {
                WSPolicyConfigurationImpl pc = AuthzPolicy.getPolicyConfiguration();
                if (pc != null) {
                    JaccProvider jaccProvider = JaccProvider.getInstance();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking the role list");
                    return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
                }
            }
        } else if (p instanceof WebRoleRefPermission || p instanceof EJBRoleRefPermission || p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = AuthzPolicy.getPolicyConfiguration();
            if (pc != null) {
                JaccProvider jaccProvider = JaccProvider.getInstance();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the role list");
                return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
            }
        }
        return false;
    }

    @Override
    public boolean isExcluded(Permission p) {
        if (p instanceof WebResourcePermission || p instanceof WebUserDataPermission || p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = AuthzPolicy.getPolicyConfiguration();
            if (pc != null) {
                JaccProvider jaccProvider = JaccProvider.getInstance();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the excluded list");

                return jaccProvider.checkExcludedPerm(pc, p);
            }
        }
        return false;
    }

    @Override
    public boolean isUnchecked(Permission p) {
        if (p instanceof WebResourcePermission || p instanceof WebUserDataPermission || p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = AuthzPolicy.getPolicyConfiguration();
            if (pc != null) {
                JaccProvider jaccProvider = JaccProvider.getInstance();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the unchecked list");
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                }
                if (p instanceof WebResourcePermission) {
                    return jaccProvider.isEveryoneGranted(pc, p, PolicyContext.getContextID());
                }
            }
        }
        return false;
    }

    @Override
    public void refresh() {
    }

    @Override
    public PermissionCollection getPermissionCollection(Subject subject) {
        return new PermissionCollectionImpl(subject);
    }

    private class PermissionCollectionImpl extends PermissionCollection {

        private static final long serialVersionUID = -7028885138486150209L;

        private final Subject subject;

        PermissionCollectionImpl(Subject subject) {
            this.subject = subject;
        }

        @Override
        public void add(Permission permission) {
            throw new UnsupportedOperationException();
        }

        /**
         * @Trivial is required in order to avoid circular issue when entry is being logged.
         */
        @Trivial
        @Override
        public boolean implies(Permission p) {
            return JaccPolicyProxy.this.implies(p, subject);
        }

        @Override
        public Enumeration<Permission> elements() {
            throw new UnsupportedOperationException();
        }
    }
}
