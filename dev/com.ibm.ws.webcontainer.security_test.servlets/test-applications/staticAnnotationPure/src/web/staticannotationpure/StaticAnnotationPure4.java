/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.staticannotationpure;

import javax.annotation.security.DeclareRoles;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//All methods (ex. CUSTOM) are unprotected, GET requires DeclaredManager, POST is denied
@DeclareRoles("DeclaredManager")
@WebServlet(name = "StaticAnnotationPure4", urlPatterns = { "/StaticAnnotationPure4" })
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.PERMIT), httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = "DeclaredManager"),
                                                                                              @HttpMethodConstraint(value = "POST", emptyRoleSemantic = EmptyRoleSemantic.DENY) })
public class StaticAnnotationPure4 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure4() {
        super("StaticAnnotationPure4");
    }

}
