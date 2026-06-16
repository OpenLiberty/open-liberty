/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.authorization.jacc.provider;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import com.ibm.websphere.ras.annotation.Trivial;

public class JaccPolicyProxy extends Policy implements AuthzPolicy {
    private static Policy policy = null;
    private ProtectionDomain self = null;

    static {
        /**
         * Force a pre-load of all these classes before JaccPolicyProxy
         * gets set as the system policy. This is required to prevent a
         * circular load dependency problem where the policy gets set
         * and JaccPolicyProxy.implies is called to see if something is
         * permitted.
         *
         * The circular flow is:
         * 1. JaccPolicyProxy.implies is called.
         * 2. JaccPolicyProxy.implies requires WebUserDataPermission.
         * 3. Need to load WebUserDataPermission, which needs to access the
         * file system (to load the JAR).
         * 4. Check if we have permission to access the file system, which
         * goes through JaccPolicyProxy.implies.
         * 5. GOTO #1.
         */
        Class<?> c;
        c = WebResourcePermission.class;
        c = WebUserDataPermission.class;
        c = WebRoleRefPermission.class;
        c = EJBRoleRefPermission.class;
        c = EJBMethodPermission.class;
        c.getName(); // Use c to prevent compile warnings
    }

    // This is called during startup - only one thread is active
    public JaccPolicyProxy() {
        // get self ProtectionDomain
        final Object p = this;
        self = (ProtectionDomain) AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
            @Override
            public Object run() {
                return p.getClass().getProtectionDomain();
            }
        });

        if (policy == null) {
            policy = (Policy) AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return Policy.getPolicy();
                }
            });
        }
    }

    @Override
    @Trivial
    public PermissionCollection getPermissions(CodeSource codeSource) {
        return policy.getPermissions(codeSource);
    }

    @Override
    public void refresh() {
        policy.refresh();
    }

    @Override
    @Trivial
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return policy.getPermissions(domain);
    }

    /**
     ** @Trivial is requred in order to avoid circular issue when entry is being logged.
     **/

    @Override
    @Trivial
    public boolean implies(ProtectionDomain pd, Permission p) {
        boolean result = false;
        if ((self == pd) && (self != null)) { // self always true
            return true;
        }

        Boolean defaultChecks = implies(Arrays.asList(pd.getPrincipals()), p);
        return defaultChecks != null ? defaultChecks : policy.implies(pd, p);
    }
}
