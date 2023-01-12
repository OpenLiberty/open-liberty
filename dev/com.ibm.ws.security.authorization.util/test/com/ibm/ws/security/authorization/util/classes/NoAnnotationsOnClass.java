/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util.classes;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

public class NoAnnotationsOnClass {

    public void unannotated() {
    }

    @DenyAll
    public void denyAll() {
    }

    @PermitAll
    public void permitAll() {
    }

    @RolesAllowed({ "role1", "role2" })
    public void rolesAllowed() {
    }
}