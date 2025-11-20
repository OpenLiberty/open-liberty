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

import javax.security.enterprise.identitystore.IdentityStorePermission;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation for Jakarta Security 1.0, 2.0 and 3.0 which all include
 * the IdentityStorePermission class.
 * 
 * NOTE: For the Jakarta Security 2.0 and 3.0 implementations, they will 
 * use this bundle but transformed for jakarta instead of javax.
 * 
 * Jakarta Security 1.0 (appSecurity-3.0) = 
 *     io.openliberty.security.javaeesec.internal_1.0.<VER>.jar
 *
 * Jakarta Security 2.0 (appSecurity-4.0) = 
 *     io.openliberty.security.javaeesec.internal.jakarta_1.0.<VER>.jar (transformed)
 *
 * Jakarta Security 3.0 (appSecurity-5.0)  = 
 *     io.openliberty.security.javaeesec.internal.jakarta_1.0.<VER>.jar (transformed)
 *     
 * *** appSecurity-3.0 was the first occurrence of the implementation (JSR375).
 */

public class IdentityStorePermissionService { 

    private static final TraceComponent tc = Tr.register(IdentityStorePermissionService.class);

    @SuppressWarnings("deprecation")
    public static void checkPermission(String name) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Using Jakarta Security 1.0/2.0/3.0 implementation.");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new IdentityStorePermission(name));
        }
    }

    @SuppressWarnings("deprecation")
    public static void checkPermission(String name, String action) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new IdentityStorePermission(name, action));
        }
    }
}
