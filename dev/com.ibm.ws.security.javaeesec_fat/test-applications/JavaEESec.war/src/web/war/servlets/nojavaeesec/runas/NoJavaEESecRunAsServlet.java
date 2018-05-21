/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.servlets.nojavaeesec.runas;

import web.jar.base.FlexibleBaseNoJavaEESecServlet;
import web.war.servlets.nojavaeesec.NoJavaEESecBaseServlet;

import javax.annotation.security.RunAs;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

@RunAs("nojavaeesec_runas")
@WebServlet("/NoJavaEESecRunAsServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {@HttpMethodConstraint(value = "GET", rolesAllowed = "basicgroup1")})
public class NoJavaEESecRunAsServlet extends NoJavaEESecBaseServlet {
    private static final long serialVersionUID = 1L;

    public NoJavaEESecRunAsServlet() {
        super("NoJavaEESecRunAsServlet");
    }
}
