/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.ejb.annotations;

import java.io.PrintWriter;

import io.openliberty.security.authorization.fat.EJBContextWrapper;
import io.openliberty.security.authorization.fat.TestUtil;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;

@Stateless
public class AnnotationEJB2 {

    @Inject
    SecurityContext secContext;

    @Resource
    EJBContext ejbContext;

    @RolesAllowed({ "EJBRole1" })
    public void doTest(PrintWriter out) {
        TestUtil.printSecurityData(out, new EJBContextWrapper(ejbContext), secContext);
    }

    @RolesAllowed({ "EJBRole3" })
    public void doTest2(PrintWriter out) {
        TestUtil.printSecurityData(out, new EJBContextWrapper(ejbContext), secContext);
    }
}
