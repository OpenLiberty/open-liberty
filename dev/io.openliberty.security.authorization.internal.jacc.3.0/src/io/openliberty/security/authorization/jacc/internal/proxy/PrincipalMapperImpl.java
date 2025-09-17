/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.security.context.SubjectManager;

import jakarta.security.jacc.PrincipalMapper;

public class PrincipalMapperImpl implements PrincipalMapper {

    @Override
    public Principal getCallerPrincipal(Subject subject) {
        return SubjectManager.getCallerPrincipal(subject);
    }

    @Override
    public Set<String> getMappedRoles(Subject subject) {
        return null;
    }

    @Override
    public boolean isAnyAuthenticatedUserRoleMapped() {
        return false;
    }
}
