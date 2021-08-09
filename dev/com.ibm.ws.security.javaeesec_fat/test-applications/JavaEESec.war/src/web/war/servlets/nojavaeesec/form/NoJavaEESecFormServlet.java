/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.servlets.nojavaeesec.form;

import web.jar.base.FlexibleBaseNoJavaEESecServlet;
import web.war.servlets.nojavaeesec.NoJavaEESecBaseServlet;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

@WebServlet("/NoJavaEESecFormServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {@HttpMethodConstraint(value = "GET", rolesAllowed = "group1")})
public class NoJavaEESecFormServlet extends NoJavaEESecBaseServlet {
    private static final long serialVersionUID = 1L;

    public NoJavaEESecFormServlet() {
        super("NoJavaEESecFormServlet");
    }
}
