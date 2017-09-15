/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.security;

import java.security.CodeSource;
import java.security.PermissionCollection;

/**
 *
 */
public interface PermissionsCombiner {

    /**
     * Combine the static permissions with the configured permissions
     * 
     * @param staticPolicyPermissions The static permissions.
     * @param codesource The code source to get the combined permissions for.
     * @return The combined permissions.
     */
    PermissionCollection getCombinedPermissions(PermissionCollection staticPolicyPermissions, CodeSource codesource);

}
