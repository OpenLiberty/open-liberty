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
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.PrincipalMapper;

public class JakartaPolicyFactoryProxyImpl implements PolicyProxy {

    private static Subject nullSubject = new Subject();

    JakartaPolicyFactoryProxyImpl() {
    }

    @Override
    public boolean implies(String contextId, Subject subject, Permission permission) {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();

        // If there is no configured PolicyFactory, treat everything as if Jakarta Authorization is not enabled.
        // This behavior is the same as what was done with previous Jacc / Authorization function.  If there wasn't
        // a configured ProviderService, no authorization checking was done.  The difference here is we always configure
        // this proxy to delegate to any configured Policy since it can happen dynamically.
        if (policyFactory == null) {
            return true;
        }
        Policy policy = policyFactory.getPolicy(contextId);
        if (policy == null) {
            return false;
        }
        return policy.implies(permission, subject == null ? nullSubject : subject);
    }

    @Override
    public PrincipalMapper getPrincipalMapper() {
        return new PrincipalMapperImpl();
    }

    @Override
    public boolean isResetPolicyContextID() {
        // Since the PolicyContext ID is no longer just used for authorization checks, but is also
        // used for PolicyFactory calls, we do not want to "leak" its setting in the current thread
        // which may be a pooled thread.
        return true;
    }

    @Override
    public void refresh(Set<String> contextIds) {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();

        if (policyFactory != null) {
            for (String contextId : contextIds) {
                Policy policy = policyFactory.getPolicy(contextId);
                if (policy != null) {
                    policy.refresh();
                }
            }
        }
    }
}
