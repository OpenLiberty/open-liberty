/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;

import jakarta.security.jacc.Policy;

public class JakartaPolicyProxyImpl implements PolicyProxy {

    private final Policy policy;

    JakartaPolicyProxyImpl(Policy p) {
        policy = p;
    }

    @Override
    public void refresh() {
        policy.refresh();
    }

    @Override
    public void setPolicy() {
    }

    @Override
    public boolean implies(Subject subject, Permission permission) {
        PermissionCollection permCollection = policy.getPermissionCollection(subject);
        return permCollection.implies(permission);
    }
}
