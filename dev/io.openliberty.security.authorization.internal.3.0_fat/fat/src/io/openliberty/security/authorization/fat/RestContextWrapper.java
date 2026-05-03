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

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

/**
 *
 */
public class RestContextWrapper implements ComponentContextWrapper {

    private final SecurityContext restSecContext;

    public RestContextWrapper(SecurityContext secContext) {
        this.restSecContext = secContext;
    }

    @Override
    public boolean isUserInRole(String role) {
        return restSecContext.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return restSecContext.getUserPrincipal();
    }

}
