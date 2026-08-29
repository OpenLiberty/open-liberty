/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.sl.provider;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

public class JaccPolicyProxy extends Policy {
    private static Policy policy = null;
    private ProtectionDomain self = null;
    private JaccProvider jaccProvider = null;

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
    public PermissionCollection getPermissions(CodeSource codeSource) {
        System.out.println("JaccPolicyProxy:getPermissions");
        return policy.getPermissions(codeSource);
    }

    @Override
    public void refresh() {
        System.out.println("JaccPolicyProxy:refresh");
        policy.refresh();
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        System.out.println("JaccPolicyProxy:getPermissions");
        return policy.getPermissions(domain);
    }

    /**
     ** @Trivial is requred in order to avoid circular issue when entry is being logged.
     **/

    @Override
    public boolean implies(ProtectionDomain pd, Permission p) {
        System.out.println("JaccPolicyProxy:implies pd : " + pd + " p : " + p);
        boolean result = false;
        if ((self == pd) && (self != null)) { // self always true
            result = true;
        } else if (p instanceof WebResourcePermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc == null) {
                return false;
            }
            if (pd.getPrincipals() == null || pd.getPrincipals().length < 1) {
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                } else {
                    return jaccProvider.isEveryoneGranted(pc, p, PolicyContext.getContextID());
                }
            } else {
                if (jaccProvider.checkExcludedPerm(pc, p)) {
                    return false;
                } else {
                    return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
                }
            }
        } else if (p instanceof WebUserDataPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc == null) {
                return false;
            }
            if (jaccProvider.checkExcludedPerm(pc, p)) {
                return false;
            } else {
                return jaccProvider.checkUncheckedPerm(pc, p);
            }
        } else if (p instanceof WebRoleRefPermission || p instanceof EJBRoleRefPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc == null) {
                return false;
            }
            return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
        } else if (p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc == null) {
                return false;
            }
            if (jaccProvider.checkExcludedPerm(pc, p)) {
                return false;
            } else {
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                } else {
                    return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
                }
            }
        } else {
            result = policy.implies(pd, p);
        }
        return result;
    }

    private WSPolicyConfigurationImpl getPolicyConfiguration() {
        //get contextID;
        String contextID = PolicyContext.getContextID();
        System.out.println("JaccPolicyProxy:getPolicyConfiguration contextId :" + contextID);
        WSPolicyConfigurationImpl pc = null;
        pc = AllPolicyConfigs.getInstance().getPolicyConfig(contextID);
        System.out.println("JaccPolicyProxy:getPolicyConfiguration pc :" + pc);

        if (pc == null) {
            return null;
        }

        boolean inService = false;
        try {
            inService = pc.inService();
        } catch (PolicyContextException pce) {
        }
        System.out.println("JaccPolicyProxy:getPolicyConfiguration inService :" + inService);

        if (!inService) {
            return null;
        }

        if (jaccProvider == null) {
            jaccProvider = JaccProvider.getInstance();
        }
        return pc;
    }
}
