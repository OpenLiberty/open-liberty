/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.fat;

import java.security.Permission;
import java.security.PermissionCollection;

import javax.security.auth.Subject;

import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.WebResourcePermission;

public class OverridePolicy implements Policy {

    private final Policy delegate;

    OverridePolicy(PolicyFactory delegateFactory, String contextId) {
        this.delegate = delegateFactory.getPolicy(contextId);
    }

    @Override
    public boolean implies(Permission permissionToBeChecked, Subject subject) {
        if (permissionToBeChecked instanceof WebResourcePermission &&
            permissionToBeChecked.getName().endsWith("/allowUnauthenticated")) {
            return true;
        }
        if (permissionToBeChecked instanceof EJBMethodPermission &&
            permissionToBeChecked.getActions().contains("doTest2")) {
            return true;
        }
        return delegate.implies(permissionToBeChecked, subject);
    }

    @Override
    public PermissionCollection getPermissionCollection(Subject arg0) {
        throw new UnsupportedOperationException();
    }
}
