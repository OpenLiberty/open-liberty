/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.web.annotations;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/servlet2/*")
@DeclareRoles({ "WebRole2", "WebRole3" })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "WebRole2" }))
public class WebAnnotationServlet2 extends AbstractRoleTestServlet {

    private static final long serialVersionUID = 1L;
}
