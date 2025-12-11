/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.PrincipalMapper;

public class JakartaPolicyFactoryProxyImpl implements PolicyProxy {

    JakartaPolicyFactoryProxyImpl() {
    }

    @Override
    public boolean implies(String contextId, Subject subject, Permission permission) {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();
        if (policyFactory == null) {
            return false;
        }
        Policy policy = policyFactory.getPolicy(contextId);
        if (policy == null) {
            return false;
        }
        return policy.implies(permission, subject);
    }

    @Override
    public PrincipalMapper getPrincipalMapper() {
        return new PrincipalMapperImpl();
    }
}
