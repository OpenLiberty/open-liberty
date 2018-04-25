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
package web.war.servlets.form.get.redirectrunas;
import web.jar.base.FlexibleBaseServlet;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

import javax.annotation.security.RunAs;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

@RunAs("javaeesec_runas")
@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=false, useForwardToLoginExpression=""))
@WebServlet("/MultipleISFormRunAsServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {@HttpMethodConstraint(value = "GET", rolesAllowed = "grantedgroup")})
public class MultipleISFormRunAsServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public MultipleISFormRunAsServlet() {
        super("MultipleISFormRunAsServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
        mySteps.add(new WriteJSR375Step());
    }
}
