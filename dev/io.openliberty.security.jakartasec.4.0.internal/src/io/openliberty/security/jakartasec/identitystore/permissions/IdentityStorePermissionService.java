/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
 */

public class IdentityStorePermissionService {

    private static final TraceComponent tc = Tr.register(IdentityStorePermissionService.class);

    // static flag to ensure the debug message is logged only once
    private static volatile boolean debugMessageLogged = false;

    public static void checkPermission(String name) {
        // **deliberately** functionally empty as this interface signature needs to be kept 
        //   as is, to bypass this check in JS 4.0 when it is unconditionally invoked
        //   from the identity stores credential validation code
        if (!(debugMessageLogged) && tc.isDebugEnabled()) {
            Tr.debug(tc, "Using Jakarta Security 4.0+ implementation (no-op).");
            debugMessageLogged = true;
        }
    }

    public static void checkPermission(String name, String action) {
        checkPermission(name);
    }
}
