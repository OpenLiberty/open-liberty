/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.identitystore.permissions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation for Jakarta Security 4.0 where the IdentityStorePermission
 * class has been removed.
 *
 * Jakarta Security 4.0 (appSecurity-6.0) =
 * io.openliberty.security.jakartasec.4.0.internal_1.0.<VER>.jar
 */

public class IdentityStorePermissionService {

    private static final TraceComponent tc = Tr.register(IdentityStorePermissionService.class);

    public static void checkPermission(String name) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Using Jakarta Security 4.0+ implementation (no-op).");
        }
    }

    public static void checkPermission(String name, String action) {
        checkPermission(name);
    }
}
