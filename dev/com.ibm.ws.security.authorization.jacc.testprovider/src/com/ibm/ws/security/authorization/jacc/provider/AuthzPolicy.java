/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.provider;

import java.security.Permission;
import java.security.Principal;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public interface AuthzPolicy {

    static final TraceComponent tc = Tr.register(JaccPolicyProxy.class);

    /**
     * Return Boolean.TRUE or Boolean.FALSE if the permission check is handled by this method.
     * A value of "null" is returned when the call of this method needs to do additional checking
     *
     * @param principals
     * @param perm
     * @return
     */
    default Boolean implies(Iterable<Principal> principals, Permission p) {
        if (p instanceof WebResourcePermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            JaccProvider jaccProvider = JaccProvider.getInstance();
            // If there is no policy configuration, the application doesn't
            // have any security constraints.  In that case return true.
            if (pc == null) {
                return true;
            }
            if (principals == null || !principals.iterator().hasNext()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the unchecked list");
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                } else {
                    return jaccProvider.isEveryoneGranted(pc, p, PolicyContext.getContextID());
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the excluded list");
                if (jaccProvider.checkExcludedPerm(pc, p)) {
                    return false;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking the role list");
                    return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
                }
            }
        } else if (p instanceof WebUserDataPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            JaccProvider jaccProvider = JaccProvider.getInstance();
            // If there is no policy configuration, the application doesn't
            // have any security constraints.  In that case return true.
            if (pc == null) {
                return true;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Checking the excluded list");
            if (jaccProvider.checkExcludedPerm(pc, p)) {
                return false;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Not in the excluded list: Checking for unchecked");
                return jaccProvider.checkUncheckedPerm(pc, p);
            }
        } else if (p instanceof WebRoleRefPermission || p instanceof EJBRoleRefPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            JaccProvider jaccProvider = JaccProvider.getInstance();
            // If there is no policy configuration, the application doesn't
            // have any security constraints.  In that case return true.
            if (pc == null) {
                return true;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Checking the role list");
            return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
        } else if (p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            JaccProvider jaccProvider = JaccProvider.getInstance();
            // If there is no policy configuration, the application doesn't
            // have any security constraints.  In that case return true.
            if (pc == null) {
                return true;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Checking the excluded list");
            if (jaccProvider.checkExcludedPerm(pc, p)) {
                return false;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the unchecked list");
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                } else {
                    return jaccProvider.checkRolePerm(pc, p, PolicyContext.getContextID());
                }
            }
        }
        return null;
    }

    public static WSPolicyConfigurationImpl getPolicyConfiguration() {
        //get contextID;
        String contextID = PolicyContext.getContextID();
        WSPolicyConfigurationImpl pc = null;
        pc = AllPolicyConfigs.getInstance().getPolicyConfig(contextID);

        if (pc == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Cannot get the policy configuration object. exit value:false");
            return null;
        }

        boolean inService = false;
        try {
            inService = pc.inService();
        } catch (PolicyContextException pce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "security.jacc.provider.inservice", new Object[] { pce });
        }

        if (!inService) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The policy configuration object is not in the commit state. exit value:false");
            return null;
        }

        return pc;
    }
}
