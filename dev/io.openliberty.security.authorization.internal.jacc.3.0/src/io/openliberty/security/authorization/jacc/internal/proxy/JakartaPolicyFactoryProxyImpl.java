/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.security.Permission;

import javax.security.auth.Subject;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.PrincipalMapper;

/**
 * Jakarta Authorization 3.0 implementation of PolicyProxy that is the interface used for interacting with
 * the jakarta.security.jacc.PolicyFactory instead of java.security.Policy as was done in previous spec versions.
 */
class JakartaPolicyFactoryProxyImpl implements PolicyProxy {

    private static Subject nullSubject = new Subject();

    private final SecurityService securityService;

    JakartaPolicyFactoryProxyImpl(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public boolean implies(String contextId, Subject subject, Permission permission) {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();

        // If there is no configured PolicyFactory, treat everything as if nothing has permission.  This should never
        // happen because we check to see if a policy is defined and if one isn't defined we do the built-in authorization
        // logic.
        //
        // This behavior is the same as what was done with previous Jacc / Authorization function.  If there wasn't
        // a configured ProviderService, default authorization checking was done.  The difference here is we always configure
        // this proxy to delegate to any configured Policy since it can happen dynamically.
        if (policyFactory == null) {
            return false;
        }
        Policy policy = policyFactory.getPolicy(contextId);
        if (policy == null) {
            return false;
        }
        return policy.implies(permission, subject == null ? nullSubject : subject);
    }

    @Override
    public PrincipalMapper getPrincipalMapper(String appName) {
        return new PrincipalMapperImpl(appName, securityService);
    }

    @Override
    public boolean isResetPolicyContextID() {
        // Since the PolicyContext ID is no longer just used for authorization checks, but is also
        // used for PolicyFactory calls, we do not want to "leak" its setting in the current thread
        // which may be a pooled thread.
        return true;
    }

    /**
     * This method is used to determine if there is a Policy configured. If there isn't
     * one configured, the Liberty runtime will use the built-in authorization logic since there
     * isn't a Jakarta Authorization Policy to call.
     *
     * @return whether there is a PolicyFactory configured or not
     */
    @Override
    public boolean isPolicyConfigured() {
        return PolicyFactory.getPolicyFactory() != null;
    }

    @Override
    public boolean isUnauthenticatedAuthorizationCheckAllowed() {
        // If there is no policy defined, we fall back to built-in authorization behavior which
        // follows the servlet specification rules.
        return PolicyFactory.getPolicyFactory() != null;
    }
}
