/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.internal;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

public class DummyPolicy extends Policy {
    public DummyPolicy() {}

    @Override
    public PermissionCollection getPermissions(CodeSource codeSource) {
        return null;
    }

    @Override
    public void refresh() {}

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return null;
    }

    @Override
    public boolean implies(ProtectionDomain pd, Permission p) {
        return true;
    }

}
