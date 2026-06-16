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

import jakarta.ejb.EJBContext;

public class EJBContextWrapper implements ComponentContextWrapper {

    private final EJBContext ejbContext;

    public EJBContextWrapper(EJBContext context) {
        ejbContext = context;
    }

    @Override
    public boolean isUserInRole(String role) {
        return ejbContext.isCallerInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return ejbContext.getCallerPrincipal();
    }
}
