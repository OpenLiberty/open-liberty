/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebRoleRefPermission;
import jakarta.security.jacc.WebUserDataPermission;

public class JaccPolicyProxy implements Policy {
    private JaccProvider jaccProvider = null;
    private static final TraceComponent tc = Tr.register(JaccPolicyProxy.class);
    private final String contextID;

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

    public JaccPolicyProxy(String contextId) {
        this.contextID = contextId;
    }

    @Override
    public boolean impliesByRole(Permission p, Subject subject) {
        if (p instanceof WebResourcePermission) {
            Set<Principal> principals = subject == null ? null : subject.getPrincipals();
            if (principals != null && principals.size() > 0) {
                WSPolicyConfigurationImpl pc = getPolicyConfiguration();
                if (pc != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking the role list");
                    return jaccProvider.checkRolePerm(pc, p, contextID);
                }
            }
        } else if (p instanceof WebRoleRefPermission || p instanceof EJBRoleRefPermission || p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the role list");
                return jaccProvider.checkRolePerm(pc, p, contextID);
            }
        }
        return false;
    }

    @Override
    public boolean isExcluded(Permission p) {
        if (p instanceof WebResourcePermission || p instanceof WebUserDataPermission || p instanceof EJBMethodPermission) {
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc != null) {
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
            WSPolicyConfigurationImpl pc = getPolicyConfiguration();
            if (pc != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the unchecked list");
                if (jaccProvider.checkUncheckedPerm(pc, p)) {
                    return true;
                }
                if (p instanceof WebResourcePermission) {
                    return jaccProvider.isEveryoneGranted(pc, p, contextID);
                }
            }
        }
        return false;
    }

    @Override
    public void refresh() {
    }

    private WSPolicyConfigurationImpl getPolicyConfiguration() {
        //get contextID;
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

        if (jaccProvider == null) {
            jaccProvider = JaccProvider.getInstance();
        }
        return pc;
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
            boolean result = false;
            Set<Principal> principals = subject == null ? null : subject.getPrincipals();
            if (p instanceof WebResourcePermission) {
                WSPolicyConfigurationImpl pc = getPolicyConfiguration();
                if (pc == null) {
                    return false;
                }
                if (principals == null || principals.size() < 1) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking the unchecked list");
                    if (jaccProvider.checkUncheckedPerm(pc, p)) {
                        return true;
                    } else {
                        return jaccProvider.isEveryoneGranted(pc, p, contextID);
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking the excluded list");
                    if (jaccProvider.checkExcludedPerm(pc, p)) {
                        return false;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Checking the role list");
                        return jaccProvider.checkRolePerm(pc, p, contextID);
                    }
                }
            } else if (p instanceof WebUserDataPermission) {
                WSPolicyConfigurationImpl pc = getPolicyConfiguration();
                if (pc == null) {
                    return false;
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
                if (pc == null) {
                    return false;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking the role list");
                return jaccProvider.checkRolePerm(pc, p, contextID);
            } else if (p instanceof EJBMethodPermission) {
                WSPolicyConfigurationImpl pc = getPolicyConfiguration();
                if (pc == null) {
                    return false;
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
                        return jaccProvider.checkRolePerm(pc, p, contextID);
                    }
                }
            }
            return result;
        }

        @Override
        public Enumeration<Permission> elements() {
            throw new UnsupportedOperationException();
        }
    }
}
